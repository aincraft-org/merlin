"""Run the shipped preprocessor and a trained classifier on drawn strokes."""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np

from .evaluate import _softmax
from .preprocess import preprocess_example, rasterize_full

MIN_FULL_INK = 80
DECISION_TOP = 0.90
DECISION_MARGIN = 0.20


def _decision_thresholds(calibration: dict) -> tuple[float, float]:
    if not calibration:
        return 0.0, 0.0
    top = float(calibration.get("top_threshold") or DECISION_TOP)
    margin = float(calibration.get("margin") or DECISION_MARGIN)
    if top > 0.99:
        top = DECISION_TOP
    if margin > 0.5:
        margin = DECISION_MARGIN
    return top, margin


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


def _full_ink(example) -> int:
    usable = [([(point.x, point.y) for point in stroke.points], stroke.brush_width) for stroke in example.strokes]
    return int((rasterize_full(usable) > 0).sum())


def classify_strokes(strokes, model, labels, torch, *, device=None, calibration=None) -> dict:
    example = example_from_strokes(strokes)
    arrays = preprocess_example(example)
    if device is None:
        device = torch.device("cpu")
    model = model.to(device).eval()
    vectors = torch.from_numpy(arrays["vectors"][None, ...]).float().to(device)
    mask = torch.from_numpy(arrays["mask"][None, ...].astype("float32")).to(device)
    raster = torch.from_numpy(arrays["raster"][None, ...]).float().to(device)
    with torch.no_grad():
        logits = model(vectors, mask, raster).detach().cpu().numpy()[0]
    calibration = calibration or {}
    top_threshold, margin = _decision_thresholds(calibration)
    probabilities = _softmax(np.asarray(logits, dtype=np.float64)[None, ...], 1.0)[0]
    order = np.argsort(-probabilities)
    candidates = [{"label": labels[index], "score": float(probabilities[index])} for index in order]
    top = candidates[0]
    runner_up = candidates[1]["score"] if len(candidates) > 1 else 0.0
    ink = _full_ink(example)
    reason = None
    if ink < MIN_FULL_INK:
        reason = "too_little_ink"
    elif top["label"] == "reject":
        reason = "reject_class"
    elif top["score"] < top_threshold or (top["score"] - runner_up) < margin:
        reason = "low_confidence"
    accepted = reason is None
    return {
        "label": top["label"] if accepted else "reject",
        "accepted": accepted,
        "reason": reason,
        "suggestion": top["label"],
        "score": top["score"],
        "candidates": candidates,
        "raster": arrays["raster"][0],
        "stroke_count": int(arrays["mask"].sum(axis=1).astype(bool).sum()),
        "ink": ink,
    }


def load_checkpoint(path: Path, torch):
    checkpoint = torch.load(Path(path), map_location="cpu", weights_only=True)
    from .model import FusedClassifier, RasterClassifier, VectorClassifier
    constructors = {"fused": FusedClassifier, "raster": RasterClassifier, "vector": VectorClassifier}
    name = checkpoint.get("model", "fused")
    model = constructors[name](int(checkpoint["classes"]), int(checkpoint["embedding_dim"]))
    model.load_state_dict(checkpoint["state_dict"])
    manifest_path = Path(path).parent / "manifest.json"
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    labels = list(manifest.get("labels") or [])
    calibration = dict(manifest.get("calibration") or {})
    return model.eval(), labels, calibration
