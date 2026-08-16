import json

from wizardry_glyphs.classify import example_from_strokes
from wizardry_glyphs.preprocess import preprocess_example
from wizardry_glyphs.studio import catalog_previews, classify_request


def test_classify_request_uses_shipped_classifier():
    import torch
    from wizardry_glyphs.model import FusedClassifier
    from wizardry_glyphs.train import _fit
    from wizardry_glyphs.classify import classify_strokes

    labels = ["heal", "damage"]
    heal_strokes = [
        {"points": [{"x": 64, "y": 20}, {"x": 64, "y": 108}], "brush_width": 6.0},
        {"points": [{"x": 20, "y": 64}, {"x": 108, "y": 64}], "brush_width": 6.0},
    ]
    damage_strokes = [
        {"points": [{"x": 30, "y": 30}, {"x": 98, "y": 98}], "brush_width": 6.0},
        {"points": [{"x": 98, "y": 30}, {"x": 30, "y": 98}], "brush_width": 6.0},
    ]
    rows = [
        {"label": "heal", **preprocess_example(example_from_strokes(heal_strokes))},
        {"label": "damage", **preprocess_example(example_from_strokes(damage_strokes))},
    ]
    model = FusedClassifier(classes=2, embedding_dim=16)
    _fit(model, rows * 8, labels, {"epochs": 40, "optimizer": "rmsprop", "learning_rate": 0.01}, torch)
    payload = classify_request({"strokes": heal_strokes}, model, labels, torch)
    direct = classify_strokes(heal_strokes, model, labels, torch)
    assert payload["label"] == direct["label"] == "heal"
    assert payload["raster"] == direct["raster"].tolist()


def test_catalog_previews_cover_composition_roles():
    previews = catalog_previews()
    labels = {item["label"] for item in previews}
    assert "heal" in labels and "on-hit" in labels and "shield" in labels
    assert "reject" not in labels
    assert previews[0]["raster"][0]
    assert json.dumps(previews[0]["raster"])
