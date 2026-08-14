from __future__ import annotations

import argparse
import hashlib
import json
import random
import shutil
import tempfile
from pathlib import Path

import numpy as np

from .cross_validate import rank_candidates, run_cross_validation
from .evaluate import calibrate_temperature, evaluate, select_thresholds
from .preprocess import preprocess_example
from .schema import LABELS, load_examples
from .split import grouped_cross_validation_split, validate_partition_isolation
from .validate_dataset import validate_dataset


def _resolve(base: Path, value: str | Path) -> Path:
    path = Path(value)
    return path if path.is_absolute() else base / path


def _rows(examples):
    return [{"example": example, **preprocess_example(example), "label": example.label,
             "lineage_group": example.lineage_group, "independent_source": example.independent_source}
            for example in examples]
def _validate_rows(rows):
    provenance = {}
    for row in rows:
        label = row.get("label")
        lineage = row.get("lineage_group")
        source = row.get("independent_source")
        if not isinstance(lineage, str) or not lineage.strip():
            raise ValueError("row requires nonempty lineage_group")
        if not isinstance(source, str) or not source.strip():
            raise ValueError("row requires nonempty independent_source")
        key = (label, lineage)
        prior = provenance.get(key)
        if prior is not None and prior != source:
            raise ValueError("independent_source conflict within lineage")
        provenance[key] = source
        if any(prior_lineage == lineage and prior_label != label for prior_label, prior_lineage in provenance):
            raise ValueError("lineage_group contains multiple labels")


def _augment_rows(rows, count, augment_example):
    if count <= 0:
        return list(rows)
    augmented = list(rows)
    for index in range(count):
        for row in rows:
            transformed = augment_example(row["example"], index)
            augmented.append({"example": transformed, **{key: value for key, value in row.items() if key != "example"}})
    return augmented


def _validate_config(config):
    try:
        seed = config.get("seed", 0)
        folds = config.get("folds", 5)
        ratio = config.get("test_ratio", .15)
        if isinstance(seed, bool) or not isinstance(seed, int):
            raise ValueError("seed must be an integer")
        if isinstance(folds, bool) or not isinstance(folds, int) or folds < 2:
            raise ValueError("folds must be at least 2")
        ratio = float(ratio)
        if not np.isfinite(ratio) or not 0 < ratio < 1:
            raise ValueError("test_ratio must be finite and between 0 and 1")
    except (TypeError, ValueError, OverflowError) as exc:
        if isinstance(exc, ValueError) and str(exc).startswith(("seed", "folds", "test_ratio")):
            raise
        raise ValueError("invalid training configuration") from exc
    return seed, folds, ratio


def _tensor(rows, torch):
    return tuple(torch.from_numpy(np.stack([row[name] for row in rows])).float()
                 for name in ("vectors", "mask", "raster"))


def _fit(model, rows, labels, config, torch):
    label_to_id = {label: index for index, label in enumerate(labels)}
    expected = torch.tensor([label_to_id[row["label"]] for row in rows], dtype=torch.long)
    optimizer = torch.optim.Adam(model.parameters(), lr=float(config.get("learning_rate", 1e-3)))
    model.train()
    for _ in range(max(1, int(config.get("epochs", 2)))):
        optimizer.zero_grad(set_to_none=True)
        loss = torch.nn.functional.cross_entropy(model(*_tensor(rows, torch)), expected)
        loss.backward(); optimizer.step()
    return model.eval()


def _public_export_inputs(torch):
    return (
        torch.zeros(1, 64, 32, 8),
        torch.zeros(1, 64, 32),
        torch.zeros(1, 1, 64, 64),
    )


def _logits(model, rows, torch):
    with torch.no_grad(): return model(*_tensor(rows, torch)).cpu().numpy()


def _sha256(*paths: Path) -> str:
    digest = hashlib.sha256()
    for path in paths: digest.update(path.read_bytes())
    return digest.hexdigest()


def _adapt(name, constructor, torch):
    model = constructor()
    if name == "vector":
        class Adapter(torch.nn.Module):
            def __init__(self, inner): super().__init__(); self.inner = inner
            def forward(self, vectors, mask, raster): return self.inner(vectors, mask)
        return Adapter(model)
    if name == "raster":
        class Adapter(torch.nn.Module):
            def __init__(self, inner): super().__init__(); self.inner = inner
            def forward(self, vectors, mask, raster): return self.inner(raster)
        return Adapter(model)
    return model


def evaluate_sealed_once(model, rows, labels, torch, *, temperature=1.0, reject_id=None, top_threshold=0.0, margin=0.0, on_evaluate=None):
    if on_evaluate is not None:
        on_evaluate("sealed")
    expected = np.array([labels[row["label"]] for row in rows], dtype=np.int64)
    return evaluate(_logits(model, rows, torch), expected, temperature, reject_id=reject_id, top_threshold=top_threshold, margin=margin)
def _calibration_split(folds):
    grouped = {}
    for row in (row for fold in folds for row in fold):
        grouped.setdefault(row["label"], {}).setdefault(row["lineage_group"], []).append(row)
    training, calibration = [], []
    for label in sorted(grouped):
        chosen = sorted(grouped[label])[0]
        calibration.extend(grouped[label][chosen])
        training.extend(row for lineage, members in grouped[label].items() if lineage != chosen for row in members)
    validate_partition_isolation([training, calibration])
    return training, calibration


def _partition_hash(rows):
    values = sorted(f'{row["label"]}:{row["lineage_group"]}' for row in rows)
    return hashlib.sha256("\n".join(values).encode()).hexdigest()


def main(argv=None):
    parser = argparse.ArgumentParser(); parser.add_argument("--config", required=True); args = parser.parse_args(argv)
    config_path = Path(args.config); config = json.loads(config_path.read_text()); base = config_path.parent
    catalog = _resolve(base, config["catalog"]); seeds = _resolve(base, config["seeds"]); review = _resolve(base, config["review"])
    players = _resolve(base, config.get("player_data", "player-data")); output = _resolve(base, config["output"])
    catalog_object = json.loads(catalog.read_text()) if catalog.exists() else {}
    labels = [item.get("id", item) if isinstance(item, dict) else item for item in catalog_object.get("labels", [])]
    if labels != list(LABELS): print("catalog does not match the supported glyph labels"); return 2
    gate = validate_dataset(labels, seeds, review, players)
    if not gate.ok:
        for deficit in gate.deficits: print(deficit)
        return 2
    try:
        seed, folds, test_ratio = _validate_config(config)
        examples = load_examples(seeds) + load_examples(players)
        rows = _rows(examples)
        _validate_rows(rows)
        partitions = grouped_cross_validation_split(rows, folds=folds, test_ratio=test_ratio, seed=seed)
        validate_partition_isolation([partitions["test"], *partitions["folds"]])
    except (TypeError, ValueError, OverflowError) as exc:
        print(exc)
        return 2
    import torch
    from .export import export_bundle
    from .model import FusedClassifier, RasterClassifier, VectorClassifier
    embedding_dim = int(config.get("embedding_dim", 16))
    constructors = {"vector": lambda: VectorClassifier(len(labels), embedding_dim), "raster": lambda: RasterClassifier(len(labels), embedding_dim), "fused": lambda: FusedClassifier(len(labels), embedding_dim)}
    candidates = [dict(value) for value in (config.get("candidates") or [{"model": name} for name in constructors])]
    label_to_id = {label: index for index, label in enumerate(labels)}

    def train_fold(candidate, training_rows):
        name = candidate["model"]; torch.manual_seed(seed); model = _adapt(name, constructors[name], torch)
        augmented = _augment_rows(training_rows, int(config.get("training_augmentations", 0)), lambda example, variant: example)
        model = _fit(model, augmented, labels, {**config, **candidate}, torch)
        return {"model": model, "parameters": sum(parameter.numel() for parameter in model.parameters())}
    def evaluate_fold(model, validation_rows):
        expected = np.array([label_to_id[row["label"]] for row in validation_rows], dtype=np.int64)
        return evaluate(_logits(model, validation_rows, torch), expected)

    cv_results = run_cross_validation(partitions["folds"], candidates, train_fold, evaluate_fold, lambda value: value)
    winner = rank_candidates(cv_results); selected_candidate = winner["candidate"]
    final_train, calibration_rows = _calibration_split(partitions["folds"])
    selected = train_fold(selected_candidate, final_train)["model"]
    calibration_labels = np.array([label_to_id[row["label"]] for row in calibration_rows], dtype=np.int64)
    calibration_logits = _logits(selected, calibration_rows, torch)
    temperature = calibrate_temperature(calibration_logits, calibration_labels)
    top_threshold, margin = select_thresholds(calibration_logits, calibration_labels, label_to_id["reject"], temperature)
    test_rows = partitions["test"]; test_labels = np.array([label_to_id[row["label"]] for row in test_rows], dtype=np.int64)
    metrics = evaluate(_logits(selected, test_rows, torch), test_labels, temperature, reject_id=label_to_id["reject"], top_threshold=top_threshold, margin=margin)
    synthetic = all(example.source in {"canonical", "synthetic", "reject"} for example in examples)
    warning = "perfect synthetic score indicates benchmark saturation, not generalization" if any(float(fold.get("macro_f1", 0)) == 1.0 for candidate in cv_results for fold in candidate.get("folds", [])) or metrics.get("macro_f1") == 1.0 else None
    release_ready = not synthetic and metrics["count"] > 0 and metrics["accuracy"] >= float(config.get("min_accuracy", 0)) and metrics["reject_false_accept_rate"] <= float(config.get("max_reject_false_accept_rate", 1)) and top_threshold > 0 and margin > 0
    metadata = {"model": selected_candidate["model"], "embedding_dim": embedding_dim, "model_id": f'{selected_candidate["model"]}-glyph-v1', "catalog_id": _sha256(catalog), "preprocessing_id": _sha256(base / "preprocessing-v1.json"), "training_id": _sha256(config_path), "dataset_id": _sha256(seeds, players), "temperature": temperature, "top_threshold": top_threshold, "margin": margin, "metrics": metrics, "release_ready": release_ready, "selected_candidate": selected_candidate, "cross_validation": {"candidates": cv_results, "winner": winner}, "partition": {"seed": seed, "fold_hashes": [_partition_hash(fold) for fold in partitions["folds"]], "test_hash": _partition_hash(test_rows), "calibration_hash": _partition_hash(calibration_rows), "train_lineages": len({row["lineage_group"] for row in final_train}), "calibration_lineages": len({row["lineage_group"] for row in calibration_rows}), "test_lineages": len({row["lineage_group"] for row in test_rows})}, "warning": warning}
    output.parent.mkdir(parents=True, exist_ok=True); temporary = Path(tempfile.mkdtemp(prefix=output.name + ".", dir=output.parent)); backup = output.with_name(output.name + ".previous")
    try:
        export_bundle(selected, _public_export_inputs(torch), temporary, labels, metadata=metadata)
        if output.exists(): output.replace(backup)
        try: temporary.replace(output)
        except Exception:
            if backup.exists(): backup.replace(output)
            raise
        if backup.exists(): shutil.rmtree(backup)
    except Exception: shutil.rmtree(temporary, ignore_errors=True); raise
    return 0 if release_ready else 3


if __name__ == "__main__": raise SystemExit(main())
