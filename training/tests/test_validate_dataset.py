import hashlib
import json
from pathlib import Path

from wizardry_glyphs.validate_dataset import validate_dataset

LABELS = ["target-ray", "damage", "heal", "push", "cooldown", "self", "target", "physical", "fire", "frost", "arcane"]


def _draft():
    return [{"points": [{"x": 1, "y": 1}, {"x": 2, "y": 2}], "brush_width": 1, "started_at_millis": 0}]


def _record(example_id, label, source="player", group=None, seed_id=None, consent=True):
    source = "canonical" if source == "seed" else source
    return {"schema_version": "glyph-dataset-v1", "example_id": example_id, "label": label, "source": source,
            "independent_source": f"fixture-source:{label}:{group or example_id}",
            "lineage_group": f"{source}:{label}:{group or example_id}", "seed_id": seed_id, "author_group": group or example_id, "session_group": group or example_id,
            "split_group": group or example_id, "consent": consent, "strokes": _draft()}
 
def _write_jsonl(path, records):
    path.write_text("".join(json.dumps(r, sort_keys=True) + "\n" for r in records))




def _complete(tmp_path):
    seeds, players = [], []
    for label in LABELS:
        for i in range(3):
            seeds.append(_record(f"seed-{label}-{i}", label, "seed", f"author-{label}-{i}"))
        for i in range(100):
            players.append(_record(f"player-{label}-{i}", label, "player", f"player-{i}", consent=True))
    for i in range(100):
        players.append(_record(f"reject-{i}", "reject", "reject", f"reject-{i}", consent=None))
    seed_path = tmp_path / "seeds.jsonl"
    player_path = tmp_path / "players.jsonl"
    _write_jsonl(seed_path, seeds)
    _write_jsonl(player_path, players)
    hashes = {}
    for line, record in zip(seed_path.read_text().splitlines(), seeds):
        hashes[record["example_id"]] = hashlib.sha256((line + "\n").encode()).hexdigest()
    review_path = tmp_path / "review.json"
    review_path.write_text(json.dumps({"catalog_version": "glyph-catalog-v1", "reviewed_at": "2026-01-01T00:00:00Z", "reviewers": ["r1", "r2"], "seeds": hashes}))
    return seed_path, review_path, player_path


def test_complete_fixture_passes(tmp_path):
    seeds, review, players = _complete(tmp_path)
    result = validate_dataset(LABELS, seeds, review, players)
    assert result.ok
    assert result.deficits == []

def test_complete_catalog_file_fixture_passes(tmp_path):
    seeds, review, players = _complete(tmp_path)
    catalog = tmp_path / "catalog.json"
    catalog.write_text(json.dumps({"version": "glyph-catalog-v1", "labels": LABELS + ["reject"]}))
    assert validate_dataset(catalog, seeds, review, players).ok


def test_empty_fixture_reports_all_labels_and_sources(tmp_path):
    seeds = tmp_path / "seeds.jsonl"; seeds.write_text("")
    players = tmp_path / "players.jsonl"; players.write_text("")
    review = tmp_path / "review.json"
    review.write_text(json.dumps({"catalog_version": "glyph-catalog-v1", "reviewers": [], "seeds": {}}))
    result = validate_dataset(LABELS, seeds, review, players)
    assert not result.ok
    text = "\n".join(result.deficits)
    assert "reject:" not in text
    for label in LABELS:
        assert f"{label}: missing 3 reviewed seeds" in text


def test_stale_review_and_grouping_are_reported(tmp_path):
    seeds, review, players = _complete(tmp_path)
    records = [json.loads(line) for line in seeds.read_text().splitlines()]
    records[1]["author_group"] = records[0]["author_group"]
    _write_jsonl(seeds, records)
    result = validate_dataset(LABELS, seeds, review, players)
    assert not result.ok
    assert any("independent seed author groups" in d for d in result.deficits)
    assert any("review hash mismatch" in d for d in result.deficits)
