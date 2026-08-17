import json
from pathlib import Path

import numpy as np
import pytest

from wizardry_glyphs.classify import classify_strokes, example_from_strokes, load_checkpoint
from wizardry_glyphs.preprocess import preprocess_example

BUNDLE = Path(__file__).resolve().parents[1] / "artifacts/dev-basic-v1"


def test_example_from_strokes_is_preprocessed_by_the_shipped_function():
    strokes = [{"points": [{"x": 20, "y": 32}, {"x": 108, "y": 32}], "brush_width": 6.0}]
    example = example_from_strokes(strokes)
    arrays = preprocess_example(example)
    assert arrays["raster"].shape == (1, 64, 64)
    assert arrays["vectors"].shape == (64, 32, 8)
    shifted = example_from_strokes([{"points": [{"x": 20, "y": 96}, {"x": 108, "y": 96}], "brush_width": 6.0}])
    assert np.array_equal(arrays["raster"], preprocess_example(shifted)["raster"])


def test_classify_strokes_uses_trained_model_on_real_preprocess():
    import torch
    from wizardry_glyphs.model import FusedClassifier
    from wizardry_glyphs.train import _fit

    labels = ["heal", "damage"]
    heal = {
        "label": "heal",
        **preprocess_example(example_from_strokes([
            {"points": [{"x": 64, "y": 20}, {"x": 64, "y": 108}], "brush_width": 6.0},
            {"points": [{"x": 20, "y": 64}, {"x": 108, "y": 64}], "brush_width": 6.0},
        ])),
    }
    damage = {
        "label": "damage",
        **preprocess_example(example_from_strokes([
            {"points": [{"x": 30, "y": 30}, {"x": 98, "y": 98}], "brush_width": 6.0},
            {"points": [{"x": 98, "y": 30}, {"x": 30, "y": 98}], "brush_width": 6.0},
        ])),
    }
    model = FusedClassifier(classes=2, embedding_dim=16)
    _fit(model, [heal, damage] * 8, labels, {"epochs": 40, "optimizer": "rmsprop", "learning_rate": 0.01}, torch)
    result = classify_strokes([
            {"points": [{"x": 64, "y": 20}, {"x": 64, "y": 108}], "brush_width": 6.0},
            {"points": [{"x": 20, "y": 64}, {"x": 108, "y": 64}], "brush_width": 6.0},
        ], model, labels, torch)
    assert result["label"] == "heal"
    assert abs(sum(item["score"] for item in result["candidates"]) - 1.0) < 1e-5
    assert result["candidates"][0]["label"] == "heal"
    assert result["raster"].shape == (64, 64)


def test_classify_uses_stroke_order_when_rasters_are_identical():
    import torch
    from wizardry_glyphs.model import FusedClassifier
    from wizardry_glyphs.train import _fit

    vertical_then_horizontal = [
        {"points": [{"x": 64, "y": 20}, {"x": 64, "y": 108}], "brush_width": 6.0},
        {"points": [{"x": 20, "y": 64}, {"x": 108, "y": 64}], "brush_width": 6.0},
    ]
    horizontal_then_vertical = list(reversed(vertical_then_horizontal))
    rows = [
        {"label": "heal", **preprocess_example(example_from_strokes(vertical_then_horizontal))},
        {"label": "damage", **preprocess_example(example_from_strokes(horizontal_then_vertical))},
    ]
    assert (rows[0]["raster"] == rows[1]["raster"]).all()
    model = FusedClassifier(classes=2, embedding_dim=16)
    _fit(model, rows * 16, ["heal", "damage"], {"epochs": 80, "optimizer": "rmsprop", "learning_rate": 0.02}, torch)
    first = classify_strokes(vertical_then_horizontal, model, ["heal", "damage"], torch)
    second = classify_strokes(horizontal_then_vertical, model, ["heal", "damage"], torch)
    assert first["label"] == "heal"
    assert second["label"] == "damage"


def test_retrained_bundle_classifies_catalog_heal_and_ignores_placement():
    if not (BUNDLE / "model.pt").is_file():
        pytest.skip("corrected training bundle not present")
    import torch
    try:
        model, labels, calibration = load_checkpoint(BUNDLE / "model.pt", torch)
    except RuntimeError:
        pytest.skip("bundle predates the order-sensitive encoder")
    catalog = json.loads((BUNDLE.parents[1] / "catalog-geometry-v1.json").read_text())
    strokes = [{"points": [{"x": x, "y": y} for x, y in stroke], "brush_width": 6.0}
               for stroke in catalog["glyphs"]["heal"]["templates"][0]["strokes"]]
    shifted = [{"points": [{"x": min(127.5, p["x"]), "y": min(127.5, p["y"] + 40)} for p in stroke["points"]],
                "brush_width": 6.0} for stroke in strokes]
    center = classify_strokes(strokes, model, labels, torch, calibration=calibration)
    moved = classify_strokes(shifted, model, labels, torch, calibration=calibration)
    assert center["accepted"] is True
    assert center["label"] == "heal"
    assert moved["label"] == "heal"


def test_bundle_does_not_force_a_class_on_a_lone_dab_or_scribble():
    if not (BUNDLE / "model.pt").is_file():
        pytest.skip("corrected training bundle not present")
    import torch
    try:
        model, labels, calibration = load_checkpoint(BUNDLE / "model.pt", torch)
    except RuntimeError:
        pytest.skip("bundle predates the order-sensitive encoder")
    dab = classify_strokes([{"points": [{"x": 64, "y": 64}], "brush_width": 6.0}], model, labels, torch, calibration=calibration)
    scribble = classify_strokes(
        [{"points": [{"x": 20, "y": 20}, {"x": 40, "y": 80}, {"x": 70, "y": 30}, {"x": 100, "y": 90}], "brush_width": 6.0}],
        model, labels, torch, calibration=calibration,
    )
    assert dab["accepted"] is False
    assert dab["label"] == "reject"
    assert scribble["accepted"] is False
    assert scribble["label"] == "reject"


def test_catalog_required_strokes_use_template_stroke_counts():
    from wizardry_glyphs.classify import catalog_required_strokes

    counts = catalog_required_strokes(BUNDLE.parents[1] / "catalog-geometry-v1.json")
    assert counts["heal"] == frozenset({2})
    assert counts["target"] == frozenset({3})
    assert counts["charges"] == frozenset({3})
    assert "reject" not in counts


def test_bundle_rejects_confident_fragments_with_the_wrong_stroke_count():
    if not (BUNDLE / "model.pt").is_file():
        pytest.skip("corrected training bundle not present")
    import torch
    from wizardry_glyphs.classify import catalog_required_strokes

    try:
        model, labels, calibration = load_checkpoint(BUNDLE / "model.pt", torch)
    except RuntimeError:
        pytest.skip("bundle predates the order-sensitive encoder")
    required = catalog_required_strokes(BUNDLE.parents[1] / "catalog-geometry-v1.json")
    catalog = json.loads((BUNDLE.parents[1] / "catalog-geometry-v1.json").read_text())
    heal = [{"points": [{"x": x, "y": y} for x, y in stroke], "brush_width": 6.0}
            for stroke in catalog["glyphs"]["heal"]["templates"][0]["strokes"]]
    dash = classify_strokes(
        [{"points": [{"x": 40, "y": 64}, {"x": 70, "y": 64}], "brush_width": 6.0}],
        model, labels, torch, calibration=calibration, required_strokes=required,
    )
    cross = classify_strokes(
        [
            {"points": [{"x": 20, "y": 20}, {"x": 108, "y": 108}], "brush_width": 6.0},
            {"points": [{"x": 108, "y": 20}, {"x": 20, "y": 108}], "brush_width": 6.0},
        ],
        model, labels, torch, calibration=calibration, required_strokes=required,
    )
    accepted = classify_strokes(heal, model, labels, torch, calibration=calibration, required_strokes=required)
    assert dash["accepted"] is False
    assert dash["reason"] == "wrong_structure"
    assert dash["label"] == "reject"
    assert cross["accepted"] is False
    assert cross["reason"] == "wrong_structure"
    assert accepted["accepted"] is True
    assert accepted["label"] == "heal"
