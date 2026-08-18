"""Four-element ink colors. Must match Java GlyphElement."""

from __future__ import annotations

import numpy as np

ELEMENTS = {
    "fire": np.array([255, 77, 0], dtype=np.float32) / 255.0,
    "frost": np.array([61, 220, 255], dtype=np.float32) / 255.0,
    "arcane": np.array([180, 74, 255], dtype=np.float32) / 255.0,
    "physical": np.array([232, 228, 217], dtype=np.float32) / 255.0,
}


def coverage(distance: float, radius: float) -> float:
    raw = radius + 0.5 - distance
    if raw <= 0:
        return 0.0
    if raw >= 1:
        return 1.0
    return float(raw)


def blend(pixel: np.ndarray, color: np.ndarray, alpha: float) -> None:
    if alpha <= 0:
        return
    if alpha >= 1:
        pixel[:] = color
        return
    pixel[:] = (1.0 - alpha) * pixel + alpha * color
