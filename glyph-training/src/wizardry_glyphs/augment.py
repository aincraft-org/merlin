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
            points.append((min(127.999999, max(0.0, x)), min(127.999999, max(0.0, y))))
        transformed.append((width, points, int(_get(stroke, "started_at_millis", 0))))
    from .schema import GlyphExample, GlyphPointData, GlyphStrokeData
    if isinstance(example, GlyphExample):
        out_strokes = tuple(
            GlyphStrokeData(tuple(GlyphPointData(x, y) for x, y in points), width, started)
            for width, points, started in transformed
        )
        return GlyphExample(
            example.schema_version, f"{example.example_id}:aug:{seed}", example.label,
            example.source, example.lineage_group, str(seed), example.author_group,
            example.session_group, example.split_group, example.consent, out_strokes,
            dict(example.generation) if example.generation is not None else None,
        )
    try:
        out = copy.deepcopy(example)
    except Exception:
        out = type(example).__new__(type(example))
        if hasattr(example, "__dict__"):
            out.__dict__ = dict(example.__dict__)
        out.strokes = [
            type(stroke)(
                [type(point)(point.x, point.y) for point in _get(stroke, "points", ())],
                _get(stroke, "brush_width", 1.0),
            )
            for stroke in _get(example, "strokes", ())
        ]
    for stroke, (width, points, _) in zip(_get(out, "strokes", ()), transformed):
        _set(stroke, "brush_width", width)
        for point, (x, y) in zip(_get(stroke, "points", ()), points):
            _set(point, "x", x); _set(point, "y", y)
    _set(out, "example_id", f"{_get(example, 'example_id', 'example')}:aug:{seed}")
    _set(out, "seed_id", seed)
    _set(out, "split_group", _get(example, "split_group", _get(example, "example_id", "example")))
    _set(out, "lineage_group", _get(example, "lineage_group"))
    return out
