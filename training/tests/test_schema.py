import json
import math
from dataclasses import FrozenInstanceError

import pytest

from wizardry_glyphs.schema import GlyphExample, GlyphPointData, GlyphStrokeData, load_examples


LABEL = "fire"


def record(**overrides):
    value = {
        "schema_version": "glyph-dataset-v1",
        "example_id": "ex-1",
        "label": LABEL,
        "source": "canonical",
        "independent_source": "fixture-source:cast:0",
        "lineage_group": "template:cast:0",
        "seed_id": "seed-1",
        "author_group": "reviewers",
        "session_group": "session-1",
        "split_group": "split-1",
        "consent": True,
        "strokes": [
            {"points": [{"x": 1.0, "y": 2.0}, {"x": 3.0, "y": 4.0}], "brush_width": 1.0, "started_at_millis": 0}
        ],
    }
    value.update(overrides)
    return value


def write_jsonl(tmp_path, *records):
    path = tmp_path / "examples.jsonl"
    path.write_text("".join(json.dumps(item) + "\n" for item in records))
    return path


def test_valid_record_loads_as_immutable_dataclasses(tmp_path):
    examples = load_examples(write_jsonl(tmp_path, record()))
    assert isinstance(examples[0], GlyphExample)
    assert isinstance(examples[0].strokes[0], GlyphStrokeData)
    assert isinstance(examples[0].strokes[0].points[0], GlyphPointData)
    with pytest.raises((FrozenInstanceError, AttributeError)):
        examples[0].label = "water"
    with pytest.raises(AttributeError):
        examples[0].strokes.append(None)

def test_missing_lineage_group_rejected(tmp_path):
    value = record()
    del value["lineage_group"]
    with pytest.raises(ValueError, match="missing required field lineage_group"):
        load_examples(write_jsonl(tmp_path, value))


def test_lineage_group_exposed(tmp_path):
    example = load_examples(write_jsonl(tmp_path, record()))[0]
    assert example.lineage_group == "template:cast:0"


def test_empty_lineage_group_rejected(tmp_path):
    with pytest.raises(ValueError, match="lineage_group"):
        load_examples(write_jsonl(tmp_path, record(lineage_group="")))

def test_whitespace_lineage_group_rejected(tmp_path):
    with pytest.raises(ValueError, match="lineage_group"):
        load_examples(write_jsonl(tmp_path, record(lineage_group=" \t")))
 


@pytest.mark.parametrize(
    ("field", "value", "message"),
    [
        ("label", "unknown", "label"),
        ("source", "unknown", "source"),
        ("strokes", [], "strokes"),
        ("consent", "yes", "consent"),
    ],
)
def test_invalid_basic_fields_rejected(tmp_path, field, value, message):
    with pytest.raises(ValueError, match=message):
        load_examples(write_jsonl(tmp_path, record(**{field: value})))


def test_player_example_requires_consent(tmp_path):
    with pytest.raises(ValueError, match="consent"):
        load_examples(write_jsonl(tmp_path, record(source="player", consent=None)))


def test_synthetic_example_requires_seed_id(tmp_path):
    with pytest.raises(ValueError, match="seed_id"):
        load_examples(write_jsonl(tmp_path, record(source="synthetic", seed_id=None)))

def test_reject_source_cannot_satisfy_positive_label(tmp_path):
    with pytest.raises(ValueError, match="reject source"):
        load_examples(write_jsonl(tmp_path, record(label="fire", source="reject")))


def test_reject_label_requires_reject_or_player_source(tmp_path):
    with pytest.raises(ValueError, match="reject label"):
        load_examples(write_jsonl(tmp_path, record(label="reject", source="canonical")))


def test_duplicate_ids_rejected(tmp_path):
    with pytest.raises(ValueError, match="duplicate.*example_id"):
        load_examples(write_jsonl(tmp_path, record(), record()))


def test_nonfinite_and_out_of_bounds_points_rejected(tmp_path):
    for point in ({"x": math.nan, "y": 1}, {"x": 128, "y": 1}, {"x": 1, "y": -0.01}):
        bad = record(strokes=[{"points": [point], "brush_width": 1, "started_at_millis": 0}])
        with pytest.raises(ValueError, match="point"):
            load_examples(write_jsonl(tmp_path, bad))


def test_stroke_and_point_limits_rejected(tmp_path):
    too_many_strokes = record(strokes=[{"points": [{"x": 1, "y": 1}], "brush_width": 1, "started_at_millis": 0}] * 65)
    with pytest.raises(ValueError, match="strokes"):
        load_examples(write_jsonl(tmp_path, too_many_strokes))
    too_many_points = record(strokes=[{"points": [{"x": 1, "y": 1}] * 257, "brush_width": 1, "started_at_millis": 0}])
    with pytest.raises(ValueError, match="points"):
        load_examples(write_jsonl(tmp_path, too_many_points))


def test_invalid_json_rejected(tmp_path):
    path = tmp_path / "bad.jsonl"
    path.write_text("not json\n")
    with pytest.raises(ValueError, match="line 1"):
        load_examples(path)


def test_stroke_element_is_optional_and_loaded(tmp_path):
    bare = load_examples(write_jsonl(tmp_path, record()))[0]
    assert bare.strokes[0].element is None
    painted = record(strokes=[{
        "points": [{"x": 1.0, "y": 2.0}, {"x": 3.0, "y": 4.0}],
        "brush_width": 1.0,
        "started_at_millis": 0,
        "element": "fire",
    }])
    example = load_examples(write_jsonl(tmp_path, painted))[0]
    assert example.strokes[0].element == "fire"
