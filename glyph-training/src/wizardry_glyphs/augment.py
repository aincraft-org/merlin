"""Seeded bounded geometric augmentation."""
from __future__ import annotations

import copy
import math
import random
from typing import Any


def _get(obj, name, default=None):
    return obj.get(name, default) if isinstance(obj, dict) else getattr(obj, name, default)


def _set(obj, name, value):
    if isinstance(obj, dict):
        obj[name] = value
    else:
        setattr(obj, name, value)


def augment_example(example: Any, seed: int, *, translation: float = 8.0, scale: float = .08, rotation: float = .12, jitter: float = 1.0, brush_variation: float = .1) -> Any:
    rng = random.Random(seed)
    try:
        out = copy.deepcopy(example)
        for stroke in _get(out, "strokes", ()):
            _set(stroke, "brush_width", float(_get(stroke, "brush_width", 1.0)))
            for point in _get(stroke, "points", ()):
                _set(point, "x", float(_get(point, "x"))); _set(point, "y", float(_get(point, "y")))
    except Exception:
        out = None
    tx, ty = (rng.uniform(-translation, translation) for _ in range(2))
    factor = 1.0 + rng.uniform(-scale, scale)
    angle = rng.uniform(-rotation, rotation)
    ca, sa = math.cos(angle), math.sin(angle)
    cx = cy = 64.0
    transformed = []
    for stroke in _get(example, "strokes", ()):
        width = min(32.0, max(1e-6, float(_get(stroke, "brush_width", 1.0)) * (1.0 + rng.uniform(-brush_variation, brush_variation))))
        points = []
        for point in _get(stroke, "points", ()):
            x, y = float(_get(point, "x")), float(_get(point, "y"))
            x = (x - cx) * factor; y = (y - cy) * factor
            x, y = x * ca - y * sa + cx + tx, x * sa + y * ca + cy + ty
            x += rng.uniform(-jitter, jitter); y += rng.uniform(-jitter, jitter)
            x = min(127.999999, max(0.0, x)); y = min(127.999999, max(0.0, y))
            if out is None:
                from .schema import GlyphPointData
                points.append(GlyphPointData(x, y))
            else:
                points.append((point, x, y))
        if out is None:
            from .schema import GlyphStrokeData
            transformed.append(GlyphStrokeData(tuple(points), width, int(_get(stroke, "started_at_millis", 0))))
    if out is None:
        out = GlyphExample(_get(example, "schema_version"), f"{_get(example, 'example_id', 'example')}:aug:{seed}", _get(example, "label"), _get(example, "source"), _get(example, "lineage_group"), str(seed), _get(example, "author_group"), _get(example, "session_group"), _get(example, "split_group"), _get(example, "consent"), tuple(transformed), _get(example, "generation"))
        return out
    for stroke, original_points in zip(_get(out, "strokes", ()), transformed):
        _set(stroke, "brush_width", width)
        for (point, x, y) in original_points:
            _set(point, "x", x); _set(point, "y", y)
    _set(out, "example_id", f"{_get(example, 'example_id', 'example')}:aug:{seed}")
    _set(out, "seed_id", seed)
    _set(out, "split_group", _get(example, "split_group", _get(example, "example_id", "example")))
    return out
