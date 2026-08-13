"""Seeded bounded geometric augmentation."""
from __future__ import annotations

import copy
import math
import random
from typing import Any


def _get(obj, name, default=None):
    return obj.get(name, default) if isinstance(obj, dict) else getattr(obj, name, default)


def _set(obj, name, value):
    if isinstance(obj, dict): obj[name] = value
    else: setattr(obj, name, value)


def augment_example(example: Any, seed: int, *, translation: float = 8.0, scale: float = .08, rotation: float = .12, jitter: float = 1.0, brush_variation: float = .1) -> Any:
    rng = random.Random(seed)
    out = copy.deepcopy(example)
    tx, ty = (rng.uniform(-translation, translation) for _ in range(2))
    factor = 1.0 + rng.uniform(-scale, scale)
    angle = rng.uniform(-rotation, rotation)
    ca, sa = math.cos(angle), math.sin(angle)
    cx = cy = 64.0
    for stroke in _get(out, "strokes", ()):
        width = float(_get(stroke, "brush_width", 1.0)) * (1.0 + rng.uniform(-brush_variation, brush_variation))
        _set(stroke, "brush_width", min(32.0, max(1e-6, width)))
        for point in _get(stroke, "points", ()):
            x, y = float(_get(point, "x")), float(_get(point, "y"))
            x = (x - cx) * factor; y = (y - cy) * factor
            x, y = x * ca - y * sa + cx + tx, x * sa + y * ca + cy + ty
            x += rng.uniform(-jitter, jitter); y += rng.uniform(-jitter, jitter)
            _set(point, "x", min(127.999999, max(0.0, x))); _set(point, "y", min(127.999999, max(0.0, y)))
    original_id = _get(example, "example_id", "example")
    _set(out, "example_id", f"{original_id}:aug:{seed}")
    _set(out, "seed_id", seed)
    _set(out, "split_group", _get(example, "split_group", original_id))
    return out
