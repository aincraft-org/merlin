import numpy as np
import pytest
from wizardry_glyphs.schema import GlyphExample, GlyphPointData, GlyphStrokeData
from wizardry_glyphs.augment import augment_example
from wizardry_glyphs.preprocess import preprocess_example


class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y


class Stroke:
    def __init__(self, points, brush_width=2.0):
        self.points, self.brush_width = points, brush_width


class Example:
    def __init__(self, strokes):
        self.strokes = strokes
        self.example_id = "e1"
        self.label = "circle"
        self.source = "test"
        self.seed_id = None
        self.lineage_group = "template:cast:0"
        self.split_group = "g1"

def sample():
    return Example([
        Stroke([Point(8, 8), Point(40, 8), Point(40, 40)]),
        Stroke([Point(60, 60), Point(90, 90)], 3.0),
    ])


def test_preprocess_shapes_features_masks_raster_and_determinism():
    out = preprocess_example(sample())
    assert out["vectors"].shape == (64, 32, 8)
    assert out["mask"].shape == (64, 32)
    assert out["raster"].shape == (1, 64, 64)
    assert out["vectors"].dtype == np.float32
    assert np.array_equal(out["vectors"], preprocess_example(sample())["vectors"])
    assert np.all((out["vectors"][:, :, :2] >= 0) & (out["vectors"][:, :, :2] < 1))
    assert np.all((out["vectors"][:, :, 5:7] == 0) | (out["vectors"][:, :, 5:7] == 1))
    assert np.all((out["raster"] >= 0) & (out["raster"] <= 1))
    assert np.all(out["mask"][:2, :]) and not np.any(out["mask"][2:, :])


def test_stroke_order_changes_vector_input():
    a, b = sample(), sample()
    b.strokes = list(reversed(b.strokes))
    assert not np.array_equal(preprocess_example(a)["vectors"], preprocess_example(b)["vectors"])


def test_empty_rejected():
    with pytest.raises(ValueError):
        preprocess_example(Example([]))


def test_augmentation_seed_provenance_grouping_bounds_and_determinism():
    e = sample()
    a = augment_example(e, seed=7)
    b = augment_example(e, seed=7)
    assert a.example_id != e.example_id
    assert a.lineage_group == e.lineage_group
    assert a.seed_id == b.seed_id and a.split_group == e.split_group
    assert [(p.x, p.y) for s in a.strokes for p in s.points] == [(p.x, p.y) for s in b.strokes for p in s.points]
    assert all(0 <= p.x < 128 and 0 <= p.y < 128 for s in a.strokes for p in s.points)
    assert all(0 < s.brush_width <= 32 for s in a.strokes)
    assert np.all((preprocess_example(a)["raster"] >= 0) & (preprocess_example(a)["raster"] <= 1))

def test_immutable_glyph_example_augmentation_preserves_parent():
    example = GlyphExample(
        "glyph-dataset-v1", "e1", "fire", "canonical", "template-source:cast:0", "template:cast:0", "seed-1",
        "author", "session", "split", True,
        (GlyphStrokeData((GlyphPointData(8, 8), GlyphPointData(40, 8)), 2, 0),),
    )
    augmented = augment_example(example, seed=7)
    assert augmented is not example
    assert augmented.lineage_group == example.lineage_group
    assert augmented.example_id != example.example_id
    assert augmented.seed_id == "7"
    assert example.strokes[0].points[0].x == 8.0

def test_augmentation_falls_back_for_uncopyable_mutable_input():
    class Uncopyable(Example):
        def __deepcopy__(self, memo):
            raise RuntimeError("cannot copy")

    example = Uncopyable(sample().strokes)
    original = [(p.x, p.y) for s in example.strokes for p in s.points]
    augmented = augment_example(example, seed=7)
    transformed = [(p.x, p.y) for s in augmented.strokes for p in s.points]
    assert augmented is not example
    assert augmented.lineage_group == example.lineage_group
    assert augmented.example_id != example.example_id
    assert transformed != original
    assert [(p.x, p.y) for s in example.strokes for p in s.points] == original
