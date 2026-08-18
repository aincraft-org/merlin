import hashlib
import json
from pathlib import Path

import pytest

from wizardry_glyphs.schema import LABELS
from wizardry_glyphs.train import _resolve_device, main


def _stroke(label_index, variant):
    x = 4 + (label_index * 9) % 110
    y = 5 + (variant % 110)
    return [{"points": [{"x": x, "y": y}, {"x": min(127, x + 5), "y": min(127, y + 6)}], "brush_width": 1, "started_at_millis": 0}]


def _record(example_id, label, source, group, strokes, consent=None):
    return {
        "schema_version": "glyph-dataset-v1", "example_id": example_id, "label": label,
        "source": source, "independent_source": f"fixture-source:{label}:{group}", "lineage_group": f"{source}:{label}:{group}",
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
            seeds.append(_record(f"seed-{label}-{variant}", label, "canonical", f"seed-group-{label}-{variant}", _stroke(label_index, variant)))
        for sample in range(100):
            group = f"player-group-{label}-{sample % 10}"
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


def test_cuda_device_is_required_when_configured(monkeypatch):
    import torch
    monkeypatch.setattr(torch.cuda, "is_available", lambda: False)
    with pytest.raises(RuntimeError, match="CUDA training requested but CUDA is unavailable"):
        _resolve_device({"device": "cuda"}, torch)


def test_cpu_device_remains_available():
    import torch
    assert _resolve_device({"device": "cpu"}, torch) == torch.device("cpu")


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
    assert manifest["input_schema"]["raster"]["shape"] == [None, 3, 64, 64]
    assert manifest["metrics"]["count"] > 0
    assert manifest["calibration"]["temperature"] > 0
    assert manifest["files"]["model.onnx"]
    assert manifest["training_device"] == "cpu"
    assert manifest["gpu_name"] is None
    assert manifest["hyperparameters"]["epochs"] == 1
    assert manifest["hyperparameters"]["training_augmentations"] == 0
    assert manifest["profile"] == "production"
    assert manifest["source"] == "reviewed-player"
    assert (artifact / "model.pt").is_file()
    assert manifest["files"]["model.pt"]
    import torch
    checkpoint = torch.load(artifact / "model.pt", map_location="cpu", weights_only=True)
    assert checkpoint
    import onnx
    assert [item.version for item in onnx.load(artifact / "model.onnx").opset_import if item.domain in ("", "ai.onnx")] == [17]


def test_synthetic_development_rejects_release_ready_manifest(tmp_path, capsys):
    config = _complete_config(tmp_path)
    value = json.loads(config.read_text())
    value["profile"] = "synthetic-development"
    value["generated_corpus"] = value.pop("seeds")
    manifest = tmp_path / "synthetic-manifest.json"
    corpus = tmp_path / value["generated_corpus"]
    counts = {label: 0 for label in LABELS}
    groups = {label: [] for label in LABELS}
    for record in map(json.loads, corpus.read_text().splitlines()):
        counts[record["label"]] += 1
        groups[record["label"]].append(record["split_group"])
    manifest.write_text(json.dumps({
        "profile": "synthetic-development", "source": "synthetic", "release_ready": True,
        "counts": counts, "groups": {label: sorted(set(items)) for label, items in groups.items()},
        "corpus_sha256": hashlib.sha256(corpus.read_bytes()).hexdigest(),
    }))
    value["generated_manifest"] = manifest.name
    config.write_text(json.dumps(value))
    assert main(["--config", str(config)]) == 2
    assert "release_ready must be false" in capsys.readouterr().out

def test_checkpoint_is_reloadable_in_fresh_cpu_process(tmp_path):
    config = _complete_config(tmp_path)
    assert main(["--config", str(config)]) in (0, 3)
    script = """
import torch, json
from wizardry_glyphs.model import VectorClassifier, RasterClassifier, FusedClassifier
checkpoint = torch.load('artifact/model.pt', map_location='cpu', weights_only=True)
constructors = {'vector': VectorClassifier, 'raster': RasterClassifier, 'fused': FusedClassifier}
model = constructors[checkpoint['model']](checkpoint['classes'], checkpoint['embedding_dim'])
if checkpoint['model'] == 'vector':
    output = model(torch.zeros(1,64,32,8), torch.zeros(1,64,32))
elif checkpoint['model'] == 'raster':
    output = model(torch.zeros(1,3,64,64))
else:
    output = model(torch.zeros(1,64,32,8), torch.zeros(1,64,32), torch.zeros(1,3,64,64))
assert output.device.type == 'cpu'
"""
    import subprocess, sys
    result = subprocess.run([sys.executable, "-c", script], cwd=tmp_path, env={"PYTHONPATH": str(Path(__file__).parents[1] / "src")}, capture_output=True, text=True)
    assert result.returncode == 0, result.stderr
def test_main_sealed_evaluation_after_calibration_and_selection(tmp_path, monkeypatch):
    import wizardry_glyphs.train as train
    config = _complete_config(tmp_path)
    events = []
    original_rank = train.rank_candidates
    monkeypatch.setattr(train, "rank_candidates", lambda candidates: (events.append("selection") or original_rank(candidates)))
    original_calibrate = train.calibrate_temperature
    monkeypatch.setattr(train, "calibrate_temperature", lambda logits, labels: (events.append("calibration") or original_calibrate(logits, labels)))
    original_sealed = train.evaluate_sealed_once
    def sealed(*args, **kwargs):
        events.append("sealed")
        return original_sealed(*args, **kwargs)
    monkeypatch.setattr(train, "evaluate_sealed_once", sealed)
    assert main(["--config", str(config)]) in (0, 3)
    assert events.count("sealed") == 1
    assert events.index("selection") < events.index("calibration") < events.index("sealed")

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
