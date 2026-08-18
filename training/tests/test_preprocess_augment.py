import numpy as np
import pytest
from wizardry_glyphs.schema import GlyphExample, GlyphPointData, GlyphStrokeData
from wizardry_glyphs.augment import augment_example
from wizardry_glyphs.preprocess import preprocess_example
from wizardry_glyphs.train import _augment_training_rows


class Point:
    def __init__(self, x, y): self.x, self.y = x, y


class Stroke:
    def __init__(self, points, width=2.0): self.points, self.brush_width = points, width


class Example:
    def __init__(self, strokes):
        self.strokes = strokes; self.example_id = "e1"; self.label = "circle"; self.source = "test"; self.seed_id = None
        self.lineage_group = "template:cast:0"; self.independent_source = "test-source"; self.split_group = "g1"


def sample():
    return Example([Stroke([Point(8, 8), Point(40, 8), Point(40, 40)]), Stroke([Point(60, 60), Point(90, 90)], 3.0)])


def test_preprocess_shapes_and_bounds():
    arrays = preprocess_example(sample())
    assert arrays["vectors"].shape == (64, 32, 8); assert arrays["mask"].shape == (64, 32); assert arrays["raster"].shape == (3, 64, 64)
    assert arrays["vectors"].dtype == np.float32
    assert np.array_equal(arrays["vectors"], preprocess_example(sample())["vectors"])
    assert np.all((arrays["raster"] >= 0) & (arrays["raster"] <= 1))
    assert np.all(arrays["mask"][:2, :]) and not np.any(arrays["mask"][2:, :])


def test_stroke_order_changes_vector_input():
    a, b = sample(), sample()
    b.strokes = list(reversed(b.strokes))
    assert not np.array_equal(preprocess_example(a)["vectors"], preprocess_example(b)["vectors"])


def test_empty_rejected():
    with pytest.raises(ValueError):
        preprocess_example(Example([]))


def test_augmentation_is_seeded_bounded_and_preserves_parent():
    e = sample(); original = [(p.x, p.y) for s in e.strokes for p in s.points]
    a = augment_example(e, seed=7); b = augment_example(e, seed=7)
    assert a.example_id != e.example_id and a.lineage_group == e.lineage_group
    assert a.seed_id == b.seed_id and a.split_group == e.split_group
    assert [(p.x, p.y) for s in a.strokes for p in s.points] == [(p.x, p.y) for s in b.strokes for p in s.points]
    assert all(0 <= p.x < 128 and 0 <= p.y < 128 for s in a.strokes for p in s.points); assert all(0 < s.brush_width <= 32 for s in a.strokes)
    assert [(p.x, p.y) for s in e.strokes for p in s.points] == original


def test_augmentation_preserves_independent_stroke_width_variation():
    augmented = augment_example(sample(), seed=7, brush_variation=0.1)
    assert augmented.strokes[0].brush_width != augmented.strokes[1].brush_width


def test_training_augmentation_preserves_originals_and_group_isolation():
    original = sample(); row = {"example": original, **preprocess_example(original), "label": original.label, "split_group": original.split_group}
    expanded = _augment_training_rows([row], count=2, seed=11)
    assert len(expanded) == 3 and all(item["split_group"] == original.split_group for item in expanded)
    assert expanded[0]["example"] is original and not np.array_equal(expanded[0]["vectors"], expanded[1]["vectors"])


def test_loaded_immutable_example_can_be_augmented():
    example = GlyphExample("glyph-dataset-v1", "immutable", "damage", "synthetic", "independent", "lineage", "seed", "author", "session", "group", None, (GlyphStrokeData((GlyphPointData(8, 8), GlyphPointData(40, 40)), 2.0, 0),), {"profile": "synthetic-development"})
    augmented = augment_example(example, seed=3)
    assert augmented.generation == example.generation and augmented.example_id != example.example_id and augmented is not example
