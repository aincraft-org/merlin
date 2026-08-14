import json
from pathlib import Path

import pytest

from wizardry_glyphs.dev_corpus import POSITIVE_LABELS, generate_corpus, validate_development_corpus

LABELS = (*POSITIVE_LABELS, "reject")


def _shapes():
    return [[[[10,10],[110,110]]],[[[10,10],[110,10],[110,110]]],[[[10,10],[110,10],[110,110],[10,110],[10,10]]],[[[10,10],[110,110]],[[10,110],[110,10]]],[[[60,10],[60,110]],[[10,60],[110,60]],[[20,20],[100,100]]],[[[10,10],[110,10],[110,110],[10,110],[10,10]],[[45,45],[75,75]]]]


def _catalog(path: Path, counts: dict[str, int], *, mutate=None) -> Path:
    glyphs = {}
    for label in LABELS:
        templates = [{"id": f"{label}-template-{index}", "independent_source": f"fixture-source-{index}", "strokes": _shapes()[index]} for index in range(counts.get(label, 0))]
        if mutate: mutate(label, templates)
        glyphs[label] = {"templates": templates}
    path.write_text(json.dumps({"catalog_version": "fixture", "glyphs": glyphs}))
    return path


def test_deficient_catalog_reports_every_label_before_output(tmp_path):
    catalog = _catalog(tmp_path / "catalog.json", {label: 1 for label in LABELS})
    with pytest.raises(ValueError) as excinfo:
        generate_corpus(catalog, tmp_path / "out", derivatives_per_label=0, reject_count=0)
    message = str(excinfo.value)
    assert "at least 6 explicit independent templates" in message
    assert all(label in message for label in LABELS)
    assert not (tmp_path / "out" / "corpus.jsonl").exists()


def test_blank_and_duplicate_ids_are_rejected(tmp_path):
    def mutate(label, templates):
        if label == "heal": templates[0]["id"] = " "
        if label == "reject": templates[1]["id"] = templates[0]["id"]
    with pytest.raises(ValueError, match="non-blank") as excinfo:
        generate_corpus(_catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS}, mutate=mutate), tmp_path / "out")
    assert "reject" in str(excinfo.value) and "unique" in str(excinfo.value)


def test_normalized_duplicate_geometry_is_rejected(tmp_path):
    def mutate(label, templates):
        if label == "damage": templates[1]["strokes"] = [[[20, 20], [220, 220]]]
    with pytest.raises(ValueError, match="normalized geometry fingerprints") as excinfo:
        generate_corpus(_catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS}, mutate=mutate), tmp_path / "out")
    assert "damage" in str(excinfo.value)
 
def test_transformed_jittered_geometry_is_rejected(tmp_path):
    def mutate(label, templates):
        if label == "damage":
            base = templates[0]["strokes"]
            templates[1]["strokes"] = [
                [[-y + 73.0 + (0.0002 if index % 2 else 0.0), x + 41.0] for index, (x, y) in enumerate(stroke)]
                for stroke in base
            ]
            templates[2]["strokes"] = [
                [[x * 1.7 - 20.0 + (0.0003 if index % 2 else 0.0), y * 1.7 + 13.0] for index, (x, y) in enumerate(stroke)]
                for stroke in base
            ]
    with pytest.raises(ValueError, match="normalized geometry fingerprints") as excinfo:
        generate_corpus(_catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS}, mutate=mutate), tmp_path / "out")
    assert "damage" in str(excinfo.value)

def test_independent_source_must_be_unique_and_nonblank(tmp_path):
    def mutate(label, templates):
        if label == "damage":
            templates[0]["independent_source"] = " "
            templates[1]["independent_source"] = templates[2]["independent_source"]
    with pytest.raises(ValueError) as excinfo:
        generate_corpus(_catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS}, mutate=mutate), tmp_path / "out")
    message = str(excinfo.value)
    assert "damage" in message and "independent_source" in message

def test_generated_record_contains_provenance_and_schema_contract(tmp_path):
    schema = json.loads((Path(__file__).parents[1] / "dataset" / "schema-v1.json").read_text())
    catalog = _catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS})
    generate_corpus(catalog, tmp_path / "out", seed_variants=0, derivatives_per_label=0, reject_count=0)
    record = json.loads((tmp_path / "out" / "corpus.jsonl").read_text().splitlines()[0])
    assert record["independent_source"].strip()
    assert "independent_source" in schema["required"]
    assert schema["properties"]["independent_source"]["pattern"] == ".*\\S.*"
    for invalid in ({key: value for key, value in record.items() if key != "independent_source"}, dict(record, independent_source=" \t")):
        assert "independent_source" not in invalid or not invalid["independent_source"].strip()
    from wizardry_glyphs.dev_corpus import _fingerprint
    base = _shapes()[2]
    transformed = [
        [[-y + 73.0, x + 41.0] for x, y in stroke]
        for stroke in base
    ]
    assert _fingerprint(base) == _fingerprint(transformed)


def test_malformed_geometry_is_aggregated_by_label(tmp_path):
    def mutate(label, templates):
        if label == "damage":
            templates[0]["strokes"] = [[]]
        if label == "heal":
            templates[0]["strokes"] = [[[1, 2, 3]]]
        if label == "push":
            templates[0]["strokes"] = [[["bad", 2]]]
        if label == "fire":
            templates[0]["strokes"] = [[[float("nan"), 2]]]
    with pytest.raises(ValueError) as excinfo:
        generate_corpus(_catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS}, mutate=mutate), tmp_path / "out")
    message = str(excinfo.value)
    assert all(label in message for label in ("damage", "heal", "push", "fire"))
    assert "invalid geometry" in message



def test_manifest_structure_tampering_is_detected(tmp_path):
    catalog = _catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS})
    generate_corpus(catalog, tmp_path / "out", seed_variants=0, derivatives_per_label=0, reject_count=0)
    manifest_path = tmp_path / "out" / "manifest.json"
    manifest = json.loads(manifest_path.read_text())
    for field in ("groups", "lineages", "lineage_counts"):
        tampered = dict(manifest)
        tampered[field] = {}
        manifest_path.write_text(json.dumps(tampered))
        assert any(field in error for error in validate_development_corpus(tmp_path / "out" / "corpus.jsonl", manifest_path))

def test_provenance_conflict_helper_rejects_lineage_collision():
    from wizardry_glyphs.dev_corpus import _provenance_map
    with pytest.raises(ValueError, match="conflict"):
        _provenance_map([
            {"label": "damage", "lineage_group": "lineage", "independent_source": "source-a"},
            {"label": "damage", "lineage_group": "lineage", "independent_source": "source-b"},
        ])

def test_validator_reports_provenance_conflict_without_traceback(tmp_path):
    catalog = _catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS})
    generate_corpus(catalog, tmp_path / "out", seed_variants=0, derivatives_per_label=0, reject_count=0)
    corpus_path = tmp_path / "out" / "corpus.jsonl"
    rows = [json.loads(line) for line in corpus_path.read_text().splitlines()]
    rows[1]["independent_source"] = "tampered-source"
    rows[1]["lineage_group"] = rows[0]["lineage_group"]
    corpus_path.write_text("".join(json.dumps(row) + "\n" for row in rows))
    errors = validate_development_corpus(corpus_path, tmp_path / "out" / "manifest.json")
    assert any("independent_source conflict" in error for error in errors)
def test_valid_catalog_emits_stable_lineages_and_manifest(tmp_path):
    catalog = _catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS})
    manifest = generate_corpus(catalog, tmp_path / "out", seed_variants=1, derivatives_per_label=1, reject_count=2)
    rows = [json.loads(line) for line in (tmp_path / "out" / "corpus.jsonl").read_text().splitlines()]
    assert all(row["lineage_group"] for row in rows)
    for label in LABELS:
        base = [row for row in rows if row["label"] == label and ":seed:" in row["example_id"]]
        assert len({row["lineage_group"] for row in base}) == 6
        assert all(row["lineage_group"] in {item["lineage_group"] for item in base} for row in rows if row["label"] == label and ":derivative:" in row["example_id"])
    assert manifest["lineage_counts"] == {label: 6 for label in LABELS}
    assert validate_development_corpus(tmp_path / "out" / "corpus.jsonl", tmp_path / "out" / "manifest.json") == []
