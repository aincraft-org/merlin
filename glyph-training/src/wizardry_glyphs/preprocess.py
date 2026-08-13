"""Deterministic vector and raster preprocessing for glyph examples."""
from __future__ import annotations

import math
from typing import Any

import numpy as np

FEATURES = ("x", "y", "dx", "dy", "progress", "pen_down", "stroke_start", "brush_width")


def _value(obj: Any, name: str, default: Any = None) -> Any:
    if isinstance(obj, dict):
        return obj.get(name, default)
    return getattr(obj, name, default)


def _strokes(example: Any):
    strokes = _value(example, "strokes", None)
    if strokes is None:
        raise ValueError("example has no strokes")
    return list(strokes)


def _points(stroke: Any):
    return list(_value(stroke, "points", ()))


def _xy(point: Any):
    return float(_value(point, "x")), float(_value(point, "y"))


def _resample(points: list[tuple[float, float]], count: int) -> list[tuple[float, float]]:
    if not points:
        return []
    if len(points) == 1:
        return points * count
    distances = [0.0]
    for a, b in zip(points, points[1:]):
        distances.append(distances[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    total = distances[-1]
    if total == 0:
        return [points[0]] * count
    targets = np.linspace(0.0, total, count)
    result = []
    for t in targets:
        i = min(len(points) - 2, int(np.searchsorted(distances, t, side="right") - 1))
        span = distances[i + 1] - distances[i]
        f = 0.0 if span == 0 else (t - distances[i]) / span
        result.append((points[i][0] + f * (points[i + 1][0] - points[i][0]), points[i][1] + f * (points[i + 1][1] - points[i][1])))
    return result


def preprocess_example(example: Any) -> dict[str, np.ndarray]:
    strokes = _strokes(example)
    usable = []
    for stroke in strokes:
        pts = [_xy(p) for p in _points(stroke)]
        if pts:
            usable.append((pts, float(_value(stroke, "brush_width", 1.0))))
    if not usable:
        raise ValueError("cannot preprocess empty glyph")

    vectors = np.zeros((64, 32, 8), dtype=np.float32)
    mask = np.zeros((64, 32), dtype=np.bool_)
    raster = np.zeros((64, 64), dtype=np.float32)
    slots = min(64, len(usable))
    for si, (raw, brush) in enumerate(usable[:slots]):
        pts = _resample(raw, 32)
        for pi, (x, y) in enumerate(pts):
            x = min(127.999999, max(0.0, x)); y = min(127.999999, max(0.0, y))
            prev = pts[pi - 1] if pi else pts[pi]
            dx, dy = (x - prev[0]) / 128.0, (y - prev[1]) / 128.0
            vectors[si, pi] = (x / 128.0, y / 128.0, dx, dy, pi / 31.0, float(pi > 0), float(pi == 0), min(32.0, max(0.0, brush)) / 32.0)
            mask[si, pi] = True
            raster[min(63, max(0, int(y / 2))), min(63, max(0, int(x / 2)))] = 1.0
    return {"vectors": vectors, "mask": mask, "raster": raster[None, ...]}
