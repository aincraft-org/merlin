import json
from pathlib import Path

import numpy as np

from wizardry_glyphs.element import ELEMENTS
from wizardry_glyphs.preprocess import preprocess_example


class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y


class Stroke:
    def __init__(self, points, width=2.0, element="physical"):
        self.points, self.brush_width, self.element = points, width, element


class Example:
    def __init__(self, strokes):
        self.strokes = strokes


def ink(raster):
    return raster.max(axis=0)


def test_raster_is_connected_brush_bit_image_not_resampled_dots():
    raster = ink(preprocess_example(Example([Stroke([Point(8, 32), Point(120, 32)], 6.0)]))["raster"])
    row = raster[int(np.argmax(raster.sum(axis=1)))]
    inked = np.where(row > 0)[0]
    assert inked.size >= 50
    assert inked.min() <= 5 and inked.max() >= 58
    assert inked[-1] - inked[0] + 1 == len(inked)


def test_thicker_brush_covers_more_raster_pixels():
    thin = ink(preprocess_example(Example([Stroke([Point(20, 64), Point(100, 64)], 2.0)]))["raster"])
    thick = ink(preprocess_example(Example([Stroke([Point(20, 64), Point(100, 64)], 8.0)]))["raster"])
    assert int((thick > 0).sum()) > int((thin > 0).sum())


def test_one_point_stroke_raster_is_round_dab():
    raster = preprocess_example(Example([Stroke([Point(64, 64)], 6.0)]))["raster"]
    np.testing.assert_allclose(raster[:, 32, 32], ELEMENTS["physical"], atol=1e-5)
    assert raster[:, 0, 0].sum() == 0.0


def test_translated_glyph_has_the_same_model_features():
    center = Example([Stroke([Point(20, 32), Point(108, 32)], 6.0)])
    shifted = Example([Stroke([Point(20, 96), Point(108, 96)], 6.0)])
    a = preprocess_example(center)
    b = preprocess_example(shifted)
    assert np.array_equal(a["raster"], b["raster"])
    np.testing.assert_allclose(a["vectors"][0, :, :2], b["vectors"][0, :, :2], atol=1e-5)


def test_catalog_heal_raster_is_a_solid_plus():
    catalog = json.loads((Path(__file__).resolve().parents[1] / "catalog-geometry-v1.json").read_text())
    strokes = [
        Stroke([Point(float(x), float(y)) for x, y in stroke], catalog["brush_width"])
        for stroke in catalog["glyphs"]["heal"]["templates"][0]["strokes"]
    ]
    raster = ink(preprocess_example(Example(strokes))["raster"])
    horizontal = np.where(raster[32] > 0)[0]
    vertical = np.where(raster[:, 32] > 0)[0]
    assert horizontal.size >= 40 and vertical.size >= 40
    assert horizontal[-1] - horizontal[0] + 1 == len(horizontal)
    assert vertical[-1] - vertical[0] + 1 == len(vertical)
