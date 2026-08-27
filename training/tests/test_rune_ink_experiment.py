import json
from pathlib import Path

import numpy as np

from wizardry_glyphs.element import ELEMENTS
from wizardry_glyphs.preprocess import preprocess_example
from wizardry_glyphs.rune_ink_experiment import (
    ELEMENT_LABELS,
    generate_corpus,
    validate_development_corpus,
)
from wizardry_glyphs.schema import load_examples


ROOT = Path(__file__).parents[1]
CATALOG = ROOT / "catalog-runes-v1.json"


def test_generated_rune_corpus_is_schema_valid_and_records_per_stroke_inks(tmp_path):
    output = tmp_path / "corpus"
    manifest = generate_corpus(
        CATALOG,
        output,
        seed_variants=1,
        derivatives_per_label=2,
        mix_count=1,
        reject_count=4,
    )
    examples = load_examples(output / "corpus.jsonl")
    assert manifest["counts"] == {
        "fire": 24,
        "frost": 24,
        "arcane": 24,
        "physical": 24,
        "reject": 4,
    }
    assert not validate_development_corpus(output / "corpus.jsonl", output / "manifest.json")
    assert {example.label for example in examples} == {*ELEMENT_LABELS, "reject"}
    element_examples = [example for example in examples if example.label != "reject"]
    assert element_examples
    assert all(stroke.element is not None for example in element_examples for stroke in example.strokes)
    assert all(
        all(("fire" if stroke.element == "flame" else stroke.element) == example.label for stroke in example.strokes)
        for example in element_examples
        if example.generation.get("ink_kind") == "single"
    )
    mixed = [example for example in element_examples if example.generation.get("ink_kind") == "mixed"]
    assert mixed
    assert all(
        any(("fire" if stroke.element == "flame" else stroke.element) == example.label for stroke in example.strokes)
        and any(("fire" if stroke.element == "flame" else stroke.element) != example.label for stroke in example.strokes)
        for example in mixed
    )


def test_fire_and_flame_are_the_same_raster_color_alias():
    points = [{"x": 24.0, "y": 24.0}, {"x": 104.0, "y": 104.0}]

    class Example:
        def __init__(self, element):
            self.label = "fire"
            self.strokes = [{"points": points, "brush_width": 6.0, "element": element}]

    fire = preprocess_example(Example("fire"))["raster"]
    flame = preprocess_example(Example("flame"))["raster"]
    np.testing.assert_array_equal(fire, flame)
    np.testing.assert_array_equal(ELEMENTS["fire"], ELEMENTS["flame"])


def test_mixed_ink_raster_retains_distinct_color_channels():
    record = json.loads(
        (Path(__file__).parents[1] / "catalog-runes-v1.json").read_text()
    )
    strokes = record["glyphs"]["gebo"]["templates"][0]["strokes"]

    class Example:
        label = "fire"
        pass

    Example.strokes = [
        {"points": [{"x": x, "y": y} for x, y in strokes[0]], "brush_width": 6.0, "element": "fire"},
        {"points": [{"x": x, "y": y} for x, y in strokes[1]], "brush_width": 6.0, "element": "frost"},
    ]
    raster = preprocess_example(Example())["raster"]
    assert np.any(np.isclose(raster[0], ELEMENTS["fire"][0]))
    assert np.any(np.isclose(raster[2], ELEMENTS["frost"][2]))
    assert not np.array_equal(raster[0], raster[2])
