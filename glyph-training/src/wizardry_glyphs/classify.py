"""Run the shipped preprocessor and a trained classifier on drawn strokes."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np

from .preprocess import preprocess_example


class _Point:
    def __init__(self, x, y):
        self.x, self.y = float(x), float(y)


class _Stroke:
    def __init__(self, points, brush_width=6.0):
        self.points = [_Point(point["x"] if isinstance(point, dict) else point.x,
                              point["y"] if isinstance(point, dict) else point.y)
                       for point in points]
        self.brush_width = float(brush_width)


class _Example:
    def __init__(self, strokes):
        self.strokes = strokes


def example_from_strokes(strokes) -> _Example:
    if not strokes:
        raise ValueError("cannot classify empty glyph")
    parsed = []
    for stroke in strokes:
        if isinstance(stroke, dict):
            parsed.append(_Stroke(stroke.get("points", ()), stroke.get("brush_width", 6.0)))
        else:
            parsed.append(_Stroke(getattr(stroke, "points"), getattr(stroke, "brush_width", 6.0)))
        if not parsed[-1].points:
            raise ValueError("cannot classify empty stroke")
    return _Example(parsed)


def classify_strokes(strokes, model, labels, torch, *, device=None) -> dict:
    arrays = preprocess_example(example_from_strokes(strokes))
    if device is None:
        device = torch.device("cpu")
    model = model.to(device).eval()
    vectors = torch.from_numpy(arrays["vectors"][None, ...]).float().to(device)
    mask = torch.from_numpy(arrays["mask"][None, ...].astype("float32")).to(device)
    raster = torch.from_numpy(arrays["raster"][None, ...]).float().to(device)
    with torch.no_grad():
        logits = model(vectors, mask, raster).detach().cpu().numpy()[0]
    logits = logits.astype(np.float64)
    logits = logits - logits.max()
    probabilities = np.exp(logits)
    probabilities = probabilities / probabilities.sum()
    order = np.argsort(-probabilities)
    candidates = [{"label": labels[index], "score": float(probabilities[index])} for index in order]
    return {
        "label": candidates[0]["label"],
        "score": candidates[0]["score"],
        "candidates": candidates,
        "raster": arrays["raster"][0],
        "stroke_count": int(arrays["mask"].sum(axis=1).astype(bool).sum()),
    }


def load_checkpoint(path: Path, torch):
    checkpoint = torch.load(Path(path), map_location="cpu", weights_only=True)
    from .model import FusedClassifier, RasterClassifier, VectorClassifier
    constructors = {"fused": FusedClassifier, "raster": RasterClassifier, "vector": VectorClassifier}
    name = checkpoint.get("model", "fused")
    model = constructors[name](int(checkpoint["classes"]), int(checkpoint["embedding_dim"]))
    model.load_state_dict(checkpoint["state_dict"])
    return model.eval(), list(json.loads((Path(path).parent / "manifest.json").read_text())["labels"]) if (Path(path).parent / "manifest.json").exists() else None
