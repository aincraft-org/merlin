import hashlib
import json
from pathlib import Path

import pytest

from wizardry_glyphs.schema import LABELS
from wizardry_glyphs.train import main


def _stroke(label_index, variant):
    x = 4 + label_index * 9
    y = 5 + variant
    return [{"points": [{"x": x, "y": y}, {"x": min(127, x + 5), "y": min(127, y + 6)}], "brush_width": 1, "started_at_millis": 0}]


def _record(example_id, label, source, group, strokes, consent=None):
    return {
        "schema_version": "glyph-dataset-v1", "example_id": example_id, "label": label,
        "source": source, "lineage_group": f"{source}:{label}:{group}",
        "seed_id": example_id if source == "canonical" else None,
        "author_group": group, "session_group": group, "split_group": group,
        "consent": consent, "strokes": strokes,
    }


def _write_jsonl(path, records):
    path.write_text("".join(json.dumps(record, sort_keys=True) + "\n" for record in records))


def _complete_config(tmp_path):
    positives = list(LABELS[:-1])
    seeds, players = [], []
    for label_index, label in enumerate(positives):
        for variant in range(3):
            seeds.append(_record(f"seed-{label}-{variant}", label, "canonical", f"seed-group-{variant}", _stroke(label_index, variant)))
        for sample in range(100):
            group = f"player-group-{sample % 10}"
            players.append(_record(f"player-{label}-{sample}", label, "player", group, _stroke(label_index, sample % 3), True))
    for sample in range(100):
        group = f"reject-group-{sample % 10}"
        players.append(_record(f"reject-{sample}", "reject", "reject", group, _stroke(11, sample % 3)))
    seeds_path = tmp_path / "seeds.jsonl"
    players_path = tmp_path / "players.jsonl"
    _write_jsonl(seeds_path, seeds)
    _write_jsonl(players_path, players)
    hashes = {}
    for line, record in zip(seeds_path.read_text().splitlines(), seeds):
        hashes[record["example_id"]] = hashlib.sha256((line + "\n").encode()).hexdigest()
    review = tmp_path / "review.json"
    review.write_text(json.dumps({"catalog_version": "glyph-catalog-v1", "reviewers": ["test"], "seeds": hashes}))
    catalog = tmp_path / "catalog.json"
    catalog.write_text(json.dumps({"labels": list(LABELS)}))
    preprocessing = tmp_path / "preprocessing-v1.json"
    preprocessing.write_text("{}")
    config = tmp_path / "train.json"
    config.write_text(json.dumps({
        "catalog": catalog.name, "seeds": seeds_path.name, "review": review.name,
        "player_data": players_path.name, "output": "artifact", "epochs": 1,
        "embedding_dim": 4, "seed": 7, "min_accuracy": 0.0,
        "max_reject_false_accept_rate": 1.0,
    }))
    return config


def test_complete_corpus_trains_and_exports_java_compatible_bundle(tmp_path):
    config = _complete_config(tmp_path)
    assert main(["--config", str(config)]) in (0, 3)
    artifact = tmp_path / "artifact"
    manifest = json.loads((artifact / "manifest.json").read_text())
    assert (artifact / "model.onnx").is_file()
    assert manifest["schema_id"] == "glyph-bundle-v1"
    assert manifest["labels"] == list(LABELS)
    assert manifest["input_schema"]["vectors"]["shape"] == [None, 64, 32, 8]
    assert manifest["input_schema"]["mask"]["shape"] == [None, 64, 32]
    assert manifest["input_schema"]["raster"]["shape"] == [None, 1, 64, 64]
    assert manifest["metrics"]["count"] > 0
    assert manifest["calibration"]["temperature"] > 0
    assert manifest["files"]["model.onnx"]


def test_export_failure_preserves_previous_artifact(tmp_path, monkeypatch):
    config = _complete_config(tmp_path)
    artifact = tmp_path / "artifact"
    artifact.mkdir()
    (artifact / "sentinel").write_text("previous")
    import wizardry_glyphs.export
    monkeypatch.setattr(wizardry_glyphs.export, "export_bundle", lambda *args, **kwargs: (_ for _ in ()).throw(RuntimeError("export failed")))
    with pytest.raises(RuntimeError, match="export failed"):
        main(["--config", str(config)])
    assert (artifact / "sentinel").read_text() == "previous"
