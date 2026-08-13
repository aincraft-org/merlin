from __future__ import annotations

import argparse
import hashlib
import json
import random
import shutil
import tempfile
from pathlib import Path

import numpy as np

from .evaluate import calibrate_temperature, evaluate, select_thresholds
from .preprocess import preprocess_example
from .schema import LABELS, load_examples
from .split import grouped_split
from .validate_dataset import validate_dataset


def _resolve(base: Path, value: str | Path) -> Path:
    path = Path(value)
    return path if path.is_absolute() else base / path


def _rows(examples):
    rows = []
    for example in examples:
        arrays = preprocess_example(example)
        rows.append({"example": example, **arrays, "label": example.label, "split_group": example.split_group})
    return rows


def _tensor(rows, torch):
    return (
        torch.from_numpy(np.stack([row["vectors"] for row in rows])).float(),
        torch.from_numpy(np.stack([row["mask"] for row in rows])).float(),
        torch.from_numpy(np.stack([row["raster"] for row in rows])).float(),
    )


def _fit(model, rows, labels, config, torch):
    label_to_id = {label: index for index, label in enumerate(labels)}
    inputs = _tensor(rows, torch)
    expected = torch.tensor([label_to_id[row["label"]] for row in rows], dtype=torch.long)
    optimizer = torch.optim.Adam(model.parameters(), lr=float(config.get("learning_rate", 1e-3)))
    model.train()
    for _ in range(max(1, int(config.get("epochs", 2)))):
        optimizer.zero_grad(set_to_none=True)
        loss = torch.nn.functional.cross_entropy(model(*inputs), expected)
        loss.backward()
        optimizer.step()
    return model.eval()


def _logits(model, rows, torch):
    with torch.no_grad():
        return model(*_tensor(rows, torch)).cpu().numpy()


def _sha256(*paths: Path) -> str:
    digest = hashlib.sha256()
    for path in paths:
        digest.update(path.read_bytes())
    return digest.hexdigest()


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args(argv)
    config_path = Path(args.config)
    config = json.loads(config_path.read_text())
    base = config_path.parent
    catalog = _resolve(base, config["catalog"])
    seeds = _resolve(base, config["seeds"])
    review = _resolve(base, config["review"])
    players = _resolve(base, config.get("player_data", "player-data"))
    output = _resolve(base, config["output"])
    catalog_object = json.loads(catalog.read_text()) if catalog.exists() else {}
    labels = [item.get("id", item) if isinstance(item, dict) else item for item in catalog_object.get("labels", [])]
    if labels != list(LABELS):
        print("catalog does not match the supported glyph labels")
        return 2
    gate = validate_dataset(labels, seeds, review, players)
    if not gate.ok:
        for deficit in gate.deficits:
            print(deficit)
        return 2

    import torch
    from .export import export_bundle
    from .model import FusedClassifier, RasterClassifier, VectorClassifier

    seed = int(config.get("seed", 0))
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    rows = _rows(load_examples(seeds) + load_examples(players))
    splits = grouped_split(rows, seed=seed)
    if any(not splits[name] for name in ("train", "calibration", "test")):
        print("grouped dataset split produced an empty partition")
        return 2

    embedding_dim = int(config.get("embedding_dim", 16))
    constructors = {
        "vector": lambda: VectorClassifier(len(labels), embedding_dim),
        "raster": lambda: RasterClassifier(len(labels), embedding_dim),
        "fused": lambda: FusedClassifier(len(labels), embedding_dim),
    }
    trained = {}
    test_scores = {}
    label_to_id = {label: index for index, label in enumerate(labels)}
    test_labels = np.array([label_to_id[row["label"]] for row in splits["test"]], dtype=np.int64)
    for name, constructor in constructors.items():
        torch.manual_seed(seed)
        model = constructor()
        if name == "vector":
            class Adapter(torch.nn.Module):
                def __init__(self, inner): super().__init__(); self.inner = inner
                def forward(self, vectors, mask, raster): return self.inner(vectors, mask)
            model = Adapter(model)
        elif name == "raster":
            class Adapter(torch.nn.Module):
                def __init__(self, inner): super().__init__(); self.inner = inner
                def forward(self, vectors, mask, raster): return self.inner(raster)
            model = Adapter(model)
        model = _fit(model, splits["train"], labels, config, torch)
        trained[name] = model
        test_scores[name] = evaluate(_logits(model, splits["test"], torch), test_labels)["accuracy"]
    selected_name = max(test_scores, key=lambda name: (test_scores[name], name == "fused"))
    selected = trained[selected_name]
    calibration_logits = _logits(selected, splits["calibration"], torch)
    calibration_labels = np.array([label_to_id[row["label"]] for row in splits["calibration"]], dtype=np.int64)
    temperature = calibrate_temperature(calibration_logits, calibration_labels)
    top_threshold, margin = select_thresholds(calibration_logits, calibration_labels, label_to_id["reject"], temperature)
    test_logits = _logits(selected, splits["test"], torch)
    metrics = evaluate(test_logits, test_labels, temperature, reject_id=label_to_id["reject"], top_threshold=top_threshold, margin=margin)
    metrics["baseline_accuracy"] = test_scores
    metrics["selected_model"] = selected_name
    release_ready = (
        metrics["count"] > 0
        and metrics["accuracy"] >= float(config.get("min_accuracy", 0.0))
        and metrics["reject_false_accept_rate"] <= float(config.get("max_reject_false_accept_rate", 1.0))
        and top_threshold > 0
        and margin > 0
    )
    metadata = {
        "model_id": f"{selected_name}-glyph-v1",
        "catalog_id": _sha256(catalog),
        "preprocessing_id": _sha256(base / "preprocessing-v1.json"),
        "training_id": _sha256(config_path),
        "dataset_id": _sha256(seeds, players),
        "temperature": temperature,
        "top_threshold": top_threshold,
        "margin": margin,
        "metrics": metrics,
        "release_ready": release_ready,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix=output.name + ".", dir=output.parent))
    backup = output.with_name(output.name + ".previous")
    try:
        golden_rows = splits["test"][:1]
        export_bundle(selected, _tensor(golden_rows, torch), temporary, labels, metadata=metadata)
        if backup.exists():
            shutil.rmtree(backup)
        if output.exists():
            output.replace(backup)
        try:
            temporary.replace(output)
        except Exception:
            if backup.exists():
                backup.replace(output)
            raise
        if backup.exists():
            shutil.rmtree(backup)
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    return 0 if release_ready else 3


if __name__ == "__main__":
    raise SystemExit(main())
