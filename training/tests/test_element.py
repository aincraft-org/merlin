import numpy as np

from wizardry_glyphs.element import ELEMENTS, blend, coverage
from wizardry_glyphs.preprocess import preprocess_example


class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y


class Stroke:
    def __init__(self, points, width=8.0, element="physical"):
        self.points, self.brush_width, self.element = points, width, element


class Example:
    def __init__(self, strokes):
        self.strokes = strokes


def test_palette_matches_java_hex():
    np.testing.assert_allclose(ELEMENTS["fire"], [1.0, 77 / 255, 0.0], atol=1e-6)
    np.testing.assert_allclose(ELEMENTS["frost"], [61 / 255, 220 / 255, 1.0], atol=1e-6)
    np.testing.assert_allclose(ELEMENTS["arcane"], [180 / 255, 74 / 255, 1.0], atol=1e-6)
    np.testing.assert_allclose(ELEMENTS["physical"], [232 / 255, 228 / 255, 217 / 255], atol=1e-6)


def test_coverage_soft_disk():
    assert coverage(0, 3) == 1.0
    assert coverage(10, 3) == 0.0
    assert abs(coverage(3.2, 3) - 0.3) < 1e-6


def test_blend_is_computed_mix():
    pixel = np.zeros(3, dtype=np.float32)
    blend(pixel, ELEMENTS["fire"], 1.0)
    np.testing.assert_allclose(pixel, ELEMENTS["fire"], atol=1e-6)
    blend(pixel, ELEMENTS["frost"], 0.5)
    np.testing.assert_allclose(pixel, (ELEMENTS["fire"] + ELEMENTS["frost"]) / 2, atol=1e-5)


def test_overlapping_strokes_compute_mixed_rgb():
    raster = preprocess_example(Example([
        Stroke([Point(64, 64)], 8.0, "fire"),
        Stroke([Point(67, 64)], 8.0, "frost"),
    ]))["raster"]
    # Cropped 64×64; mixed ink must use more than one channel somewhere.
    mixed = (raster[0] > 0.05) & (raster[2] > 0.05)
    assert mixed.any()
