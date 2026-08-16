"""Deterministic vector and raster preprocessing for glyph examples."""
from __future__ import annotations

import math
from typing import Any

import numpy as np

FEATURES = ("x", "y", "dx", "dy", "progress", "pen_down", "stroke_start", "brush_width")
CANVAS = 128
RASTER = 64


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


def _stamp(pixels: np.ndarray, x: float, y: float, width: float) -> None:
    radius = width / 2.0
    bounds = max(0, int(math.ceil(radius + 0.5)))
    cx, cy = int(round(x)), int(round(y))
    y0, y1 = max(0, cy - bounds), min(CANVAS - 1, cy + bounds)
    x0, x1 = max(0, cx - bounds), min(CANVAS - 1, cx + bounds)
    for py in range(y0, y1 + 1):
        for px in range(x0, x1 + 1):
            if math.hypot(px - x, py - y) <= radius + 0.5:
                pixels[py, px] = 1.0


def _interpolation_steps(x0: float, y0: float, x1: float, y1: float, start_width: float, end_width: float) -> int:
    distance = math.hypot(x1 - x0, y1 - y0)
    radius_delta = abs(end_width - start_width) / 2.0
    movement_steps = int(math.ceil(distance / 0.5))
    radius_steps = int(math.ceil(radius_delta / 0.25))
    return max(1, max(movement_steps, radius_steps))


def _draw_line(pixels: np.ndarray, x0: float, y0: float, x1: float, y1: float, start_width: float, end_width: float) -> None:
    steps = _interpolation_steps(x0, y0, x1, y1, start_width, end_width)
    for index in range(steps + 1):
        t = index / steps
        width = start_width + (end_width - start_width) * t
        _stamp(pixels, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, width)


def rasterize_full(usable: list[tuple[list[tuple[float, float]], float]]) -> np.ndarray:
    """128×128 round-brush bitmap matching Java GlyphRasterizer.renderFull."""
    pixels = np.zeros((CANVAS, CANVAS), dtype=np.float32)
    for raw, brush in usable:
        width = min(32.0, max(1e-6, float(brush)))
        if len(raw) == 1:
            _stamp(pixels, raw[0][0], raw[0][1], width)
            continue
        for (x0, y0), (x1, y1) in zip(raw, raw[1:]):
            _draw_line(pixels, x0, y0, x1, y1, width, width)
    return pixels


def rasterize_model(usable: list[tuple[list[tuple[float, float]], float]]) -> np.ndarray:
    """Downsample the full canvas to the 64×64 model raster with 2×2 max-pool."""
    full = rasterize_full(usable)
    return np.maximum.reduce((full[0::2, 0::2], full[0::2, 1::2], full[1::2, 0::2], full[1::2, 1::2]))


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
    raster = rasterize_model(usable)
    slots = min(64, len(usable))
    for si, (raw, brush) in enumerate(usable[:slots]):
        pts = _resample(raw, 32)
        for pi, (x, y) in enumerate(pts):
            x = min(127.999999, max(0.0, x)); y = min(127.999999, max(0.0, y))
            prev = pts[pi - 1] if pi else pts[pi]
            dx, dy = (x - prev[0]) / 128.0, (y - prev[1]) / 128.0
            vectors[si, pi] = (x / 128.0, y / 128.0, dx, dy, pi / 31.0, float(pi > 0), float(pi == 0), min(32.0, max(0.0, brush)) / 32.0)
            mask[si, pi] = True
    return {"vectors": vectors, "mask": mask, "raster": raster[None, ...]}
