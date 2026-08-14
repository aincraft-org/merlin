import json
from pathlib import Path

import pytest

from wizardry_glyphs.dev_corpus import POSITIVE_LABELS, generate_corpus, validate_development_corpus

LABELS = (*POSITIVE_LABELS, "reject")


def _shapes():
    return [[[[10,10],[110,110]]],[[[10,110],[110,10]]],[[[10,10],[110,10],[110,110]]],[[[10,10],[60,110],[110,10]]],[[[10,10],[110,10]],[[60,20],[60,110]]],[[[10,10],[110,10],[110,110],[10,110],[10,10]]]]


def _catalog(path: Path, counts: dict[str, int], *, mutate=None) -> Path:
    glyphs = {}
    for label in LABELS:
        templates = [{"id": f"{label}-template-{index}", "strokes": _shapes()[index]} for index in range(counts.get(label, 0))]
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
