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
        model, labels = load_checkpoint(BUNDLE / "model.pt", torch)
    except RuntimeError:
        pytest.skip("bundle predates the order-sensitive encoder")
    catalog = json.loads((BUNDLE.parents[1] / "catalog-geometry-v1.json").read_text())
    strokes = [{"points": [{"x": x, "y": y} for x, y in stroke], "brush_width": 6.0}
               for stroke in catalog["glyphs"]["heal"]["templates"][0]["strokes"]]
    shifted = [{"points": [{"x": min(127.5, p["x"]), "y": min(127.5, p["y"] + 40)} for p in stroke["points"]],
                "brush_width": 6.0} for stroke in strokes]
    center = classify_strokes(strokes, model, labels, torch)
    moved = classify_strokes(shifted, model, labels, torch)
    assert center["label"] == "heal"
    assert moved["label"] == "heal"
