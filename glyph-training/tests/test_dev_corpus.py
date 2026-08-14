import json
from pathlib import Path

import pytest

from wizardry_glyphs.dev_corpus import POSITIVE_LABELS, generate_corpus, validate_development_corpus

LABELS = (*POSITIVE_LABELS, "reject")


def _shape(offset: int) -> list[list[list[int]]]:
    return [[[10 + offset, 10], [110 - offset, 110]], [[10, 110 - offset], [110, 10 + offset]]]


def _catalog(path: Path, counts: dict[str, int]) -> Path:
    glyphs = {}
    for label in LABELS:
        glyphs[label] = {
            "templates": [
                {"id": f"{label}-template-{index}", "strokes": _shape(index)}
                for index in range(counts.get(label, 0))
            ]
        }
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


def test_valid_catalog_emits_stable_lineages_and_manifest(tmp_path):
    catalog = _catalog(tmp_path / "catalog.json", {label: 6 for label in LABELS})
    manifest = generate_corpus(catalog, tmp_path / "out", seed_variants=1, derivatives_per_label=1, reject_count=2)
    rows = [json.loads(line) for line in (tmp_path / "out" / "corpus.jsonl").read_text().splitlines()]
    assert all(row["lineage_group"] for row in rows)
    for label in LABELS:
        base = [row for row in rows if row["label"] == label and ":seed:" in row["example_id"]]
        assert len({row["lineage_group"] for row in base}) == 6
        for row in rows:
            if row["label"] == label and ":derivative:" in row["example_id"]:
                assert row["lineage_group"] in {item["lineage_group"] for item in base}
    assert manifest["lineages"] == {label: sorted({row["lineage_group"] for row in rows if row["label"] == label}) for label in LABELS}
    assert manifest["lineage_counts"] == {label: 6 for label in LABELS}
    assert validate_development_corpus(tmp_path / "out" / "corpus.jsonl", tmp_path / "out" / "manifest.json") == []
