
import copy

def _assert_target_template(template):
    strokes = template["strokes"]
    assert len(strokes) == 4
    stroke0, south, west, east = strokes
    assert len(stroke0) == 7
    assert all(len(stroke) == 2 for stroke in strokes[1:])
    north, top, right, bottom, left, close, terminal = stroke0
    assert close == top == terminal
    assert abs(north[0] - sum(p[0] for p in (top, right, bottom, left)) / 4) < 1e-6
    assert north[1] < top[1]
    assert left[0] < right[0] and top[1] < bottom[1]
    turns = [(b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-b[0])
             for a,b,c in zip((top,right,bottom,left),(right,bottom,left,top),(bottom,left,top,right))]
    assert all(t > 0 for t in turns) or all(t < 0 for t in turns)
    for stroke, axis, increasing in ((south, 0, True), (west, 1, False), (east, 1, True)):
        start, end = stroke
        assert start[1-axis] == end[1-axis]
        assert (end[axis] > start[axis]) is increasing

def test_target_template_rejects_north_endpoint_contamination():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    malformed = copy.deepcopy(geometry["glyphs"]["target"]["templates"][0])
    malformed["strokes"][0].pop(0)
    with pytest.raises(AssertionError):
        _assert_target_template(malformed)

def test_target_template_rejects_inserted_center_bar_segment():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    malformed = copy.deepcopy(geometry["glyphs"]["target"]["templates"][0])
    malformed["strokes"][0].insert(2, [64.0, 64.0])
    with pytest.raises(AssertionError):
        _assert_target_template(malformed)
import json
from pathlib import Path

import pytest

from wizardry_glyphs.dev_corpus import generate_corpus, validate_development_corpus
from wizardry_glyphs.schema import LABELS

ROOT = Path(__file__).resolve().parents[1]
COMPOSITION_LABELS = (
    "on-hit", "on-hurt", "on-use", "periodic",
    "if-health", "if-undead", "if-outdoors",
    "shield", "attacker", "area", "repeat", "charges",
)


def _assert_target_template(template):
    strokes = template["strokes"]
    assert len(strokes) == 4
    stroke0, south, west, east = strokes
    assert len(stroke0) == 7
    assert all(len(stroke) == 2 for stroke in strokes[1:])
    north, top, right, bottom, left, top_close, terminal_top = stroke0
    assert top_close == top == terminal_top
    diamond = (top, right, bottom, left)
    cx = sum(point[0] for point in diamond) / 4
    assert abs(north[0] - cx) < 1e-6
    assert north[1] < top[1] - 1e-6
    assert left[0] < right[0] and top[1] < bottom[1]
    turns = []
    for first, second, third in zip(diamond, diamond[1:] + diamond[:1], diamond[2:] + diamond[:2]):
        turns.append(
            (second[0] - first[0]) * (third[1] - second[1])
            - (second[1] - first[1]) * (third[0] - second[0])
        )
    assert all(turn > 0 for turn in turns) or all(turn < 0 for turn in turns)
    for stroke, axis, increasing in (
        (south, 0, True), (west, 1, False), (east, 1, True),
    ):
        start, end = stroke
        other = 1 - axis
        assert start[other] == end[other]
        assert (end[axis] > start[axis]) is increasing


def test_target_template_grammar_rejects_north_endpoint_contamination():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    malformed = copy.deepcopy(geometry["glyphs"]["target"]["templates"][0])
    malformed["strokes"][0].pop(0)
    with pytest.raises(AssertionError):
        _assert_target_template(malformed)


def test_target_template_grammar_rejects_inserted_center_bar_segment():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    malformed = copy.deepcopy(geometry["glyphs"]["target"]["templates"][0])
    malformed["strokes"][0].insert(2, [64.0, 64.0])
    with pytest.raises(AssertionError):
        _assert_target_template(malformed)




def test_schema_and_catalog_include_composition_roles():
    catalog = json.loads((ROOT / "catalog-v1.json").read_text())
    ids = [item["id"] for item in catalog["labels"]]
    assert ids == list(LABELS)
    assert ids[-1] == "reject"
    for label in COMPOSITION_LABELS:
        assert label in ids


def test_canonical_topologies_have_required_connectivity_and_counts():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    target = geometry["glyphs"]["target"]["templates"][0]
    _assert_target_template(target)
    assert len(geometry["reference_sources"]) >= 3
    for source in geometry["reference_sources"]:
        assert source["url"].startswith("https://")
        assert "public domain" in source["license"].lower()
    for label in LABELS[:-1]:
        entry = geometry["glyphs"][label]
        assert 1 <= entry["logical_strokes"] <= 4
        assert all(1 <= len(template["strokes"]) <= 4 for template in entry["templates"])
        from wizardry_glyphs.dev_corpus import _fingerprint
        assert len({_fingerprint(template["strokes"]) for template in entry["templates"]}) >= 6
    reject = geometry["glyphs"]["reject"]
    assert reject["templates"] == []
    assert reject["reject_metadata"] == {
        "label": "reject",
        "canonical_concept": "no canonical positive sigil",
        "logical_strokes": None,
        "distinguishing_structure": "heterogeneous non-glyph input",
    }
    assert {recipe["family"] for recipe in reject["reject_recipes"]} == {
        "blank-near-blank", "accidental-taps", "partial-positives",
        "malformed-closures", "ambiguous-blends", "scribbles", "ordinary-doodles",
    }


def test_if_outdoors_is_roofless_horizon_with_three_rays():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    for template in geometry["glyphs"]["if-outdoors"]["templates"]:
        strokes = template["strokes"]
        assert len(strokes) == 4
        assert strokes[0][0][1] == strokes[0][-1][1]
        assert all(stroke[0][1] > stroke[1][1] for stroke in strokes[1:])

def test_canonical_topologies_have_required_connectivity_and_counts():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    target = geometry["glyphs"]["target"]["templates"][0]
    _assert_target_template(target)
    on_hit = geometry["glyphs"]["on-hit"]["templates"][0]["strokes"]
    assert len(on_hit) == 4 and len(on_hit[3]) >= 3
    periodic = geometry["glyphs"]["periodic"]["templates"][0]["strokes"]
    assert len(periodic) == 3 and len(periodic[0]) >= 13
    area = geometry["glyphs"]["area"]["templates"][0]["strokes"]
    assert len(area) == 4 and all(len(stroke) >= 3 for stroke in area[1:])
    charges = geometry["glyphs"]["charges"]["templates"][0]["strokes"]
    assert geometry["glyphs"]["charges"]["logical_strokes"] == len(charges) == 3
    assert all(len(stroke) >= 5 for stroke in charges)
def test_geometry_catalog_has_six_independent_templates_per_label():
    geometry = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    for label in LABELS:
        templates = geometry["glyphs"][label]["templates"]
        assert len(templates) >= 6, label
        ids = [item["id"] for item in templates]
        sources = [item["independent_source"] for item in templates]
        assert len(set(ids)) == len(ids)
        assert len(set(sources)) == len(sources)


def test_geometry_catalog_generates_a_valid_development_corpus(tmp_path):
    manifest = generate_corpus(
        ROOT / "catalog-geometry-v1.json",
        tmp_path / "out",
        seed_variants=0,
        derivatives_per_label=0,
        reject_count=0,
    )
    errors = validate_development_corpus(tmp_path / "out" / "corpus.jsonl", tmp_path / "out" / "manifest.json")
    assert errors == []
    assert manifest["lineage_counts"][COMPOSITION_LABELS[0]] >= 6
    assert all(manifest["lineage_counts"][label] >= 6 for label in LABELS)


def test_training_config_records_selected_optimizer():
    config = json.loads((ROOT / "train-dev-basic-v1.json").read_text())
    assert config["optimizer"] == "rmsprop"
    assert config["learning_rate"] == 0.003
    assert config["epochs"] == 100
    assert config["embedding_dim"] == 32
