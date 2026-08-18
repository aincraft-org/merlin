from __future__ import annotations

import argparse
import hashlib
import json
import random
import shutil
import tempfile
from pathlib import Path

import numpy as np

from .augment import augment_example
from .cross_validate import rank_candidates, run_cross_validation
from .evaluate import calibrate_temperature, evaluate, select_thresholds
from .preprocess import preprocess_example
from .schema import LABELS, load_examples
from .split import grouped_cross_validation_split, validate_partition_isolation
from .validate_dataset import validate_dataset


def _resolve(base: Path, value: str | Path) -> Path:
    path = Path(value)
    return path if path.is_absolute() else base / path


def _resolve_device(config, torch):
    requested = str(config.get("device", "cpu"))
    if requested not in {"cpu", "cuda"}:
        raise ValueError(f"unsupported training device: {requested}")
    if requested == "cuda" and not torch.cuda.is_available():
        raise RuntimeError("CUDA training requested but CUDA is unavailable")
    return torch.device(requested)


def _rows(examples):
    return [{"example": example, **preprocess_example(example), "label": example.label,
             "lineage_group": example.lineage_group, "independent_source": example.independent_source,
             **({"split_group": example.split_group} if hasattr(example, "split_group") else {})}
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


def _augment_training_rows(rows, count: int, seed: int):
    if count <= 0:
        return list(rows)
    expanded = list(rows)
    for row_index, row in enumerate(rows):
        for augmentation_index in range(count):
            transformed = augment_example(row["example"], seed * 1_000_003 + row_index * count + augmentation_index)
            expanded.append({
                "example": transformed,
                **preprocess_example(transformed),
                "label": row["label"],
                "lineage_group": row.get("lineage_group"),
                "independent_source": row.get("independent_source"),
                "split_group": row.get("split_group"),
            })
    return expanded


def _augment_rows(rows, count, augment_fn):
    if count <= 0:
        return list(rows)
    augmented = list(rows)
    for index in range(count):
        for row in rows:
            transformed = augment_fn(row["example"], index)
            augmented.append({"example": transformed,
                              **{key: value for key, value in row.items() if key not in {"example", "vectors", "mask", "raster"}},
                              **preprocess_example(transformed)})
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


def _tensor(rows, torch, device=None):
    tensors = tuple(torch.from_numpy(np.stack([row[name] for row in rows])).float()
                    for name in ("vectors", "mask", "raster"))
    return tuple(value.to(device) for value in tensors) if device is not None else tensors


def _make_optimizer(model, config, torch):
    name = str(config.get("optimizer", "adam")).strip().lower()
    learning_rate = float(config.get("learning_rate", 1e-3))
    parameters = model.parameters()
    if name == "adam":
        return torch.optim.Adam(parameters, lr=learning_rate)
    if name == "adamw":
        return torch.optim.AdamW(parameters, lr=learning_rate, weight_decay=float(config.get("weight_decay", 0.01)))
    if name == "sgd":
        return torch.optim.SGD(parameters, lr=learning_rate, momentum=float(config.get("momentum", 0.9)))
    if name == "rmsprop":
        return torch.optim.RMSprop(parameters, lr=learning_rate)
    raise ValueError(f"unsupported optimizer: {name}")


def _fit(model, rows, labels, config, torch, device=None, validation_rows=None):
    if device is None:
        device = torch.device("cpu")
    model = model.to(device)
    label_to_id = {label: index for index, label in enumerate(labels)}
    all_labels = torch.tensor([label_to_id[row["label"]] for row in rows], dtype=torch.long)
    counts = torch.bincount(all_labels, minlength=len(labels)).float()
    class_weights = all_labels.numel() / (len(labels) * counts.clamp_min(1.0))
    class_weights = class_weights.to(device)
    optimizer = _make_optimizer(model, config, torch)
    selection = str(config.get("selection_metric", "val_macro_f1"))
    max_epochs = max(1, int(config.get("epochs", 2)))
    patience = int(config.get("patience", 0))
    batch_size = max(1, int(config.get("batch_size") or len(rows)))
    smoothing = float(config.get("label_smoothing", 0.0))
    validate = validation_rows is not None and len(validation_rows) > 0
    val_labels = None
    if validate:
        val_labels = np.array([label_to_id[row["label"]] for row in validation_rows], dtype=np.int64)
    history = []
    best_state = None
    best_metric = -float("inf")
    wait = 0
    order = list(range(len(rows)))
    rng = random.Random(int(config.get("seed", 0)))
    for epoch in range(1, max_epochs + 1):
        model.train()
        rng.shuffle(order)
        running = 0.0
        seen = 0
        for start in range(0, len(order), batch_size):
            batch = [rows[index] for index in order[start:start + batch_size]]
            expected = torch.tensor([label_to_id[row["label"]] for row in batch], dtype=torch.long, device=device)
            optimizer.zero_grad(set_to_none=True)
            loss_t = torch.nn.functional.cross_entropy(model(*_tensor(batch, torch, device)), expected, weight=class_weights, label_smoothing=smoothing)
            loss_t.backward()
            optimizer.step()
            running += float(loss_t.item()) * len(batch)
            seen += len(batch)
        train_loss = running / max(seen, 1)
        if validate:
            model.eval()
            val_logits = _logits(model, validation_rows, torch, device, batch_size)
            val_expected = torch.tensor(val_labels, dtype=torch.long)
            val_loss = float(torch.nn.functional.cross_entropy(torch.from_numpy(val_logits), val_expected).item())
            val_metrics = evaluate(val_logits, val_labels)
            val_macro_f1 = val_metrics["macro_f1"]
            history.append({"epoch": epoch, "train_loss": train_loss, "val_loss": val_loss, "val_macro_f1": val_macro_f1})
            metric = val_macro_f1 if selection != "val_loss" else -val_loss
            if metric > best_metric + 1e-9:
                best_metric = metric
                best_state = {k: v.detach().clone() for k, v in model.state_dict().items()}
                wait = 0
            else:
                wait += 1
                if 0 < patience <= wait:
                    break
        else:
            history.append({"epoch": epoch, "train_loss": train_loss})
    if best_state is not None:
        model.load_state_dict(best_state)
    return {"model": model.eval(), "history": history} if validate else model.eval()


def _public_export_inputs(torch):
    return (torch.zeros(1, 64, 32, 8), torch.zeros(1, 64, 32), torch.zeros(1, 3, 64, 64))


def _logits(model, rows, torch, device=None, batch_size=None):
    if not rows:
        return np.zeros((0, 0), dtype=np.float32)
    chunk = max(1, int(batch_size or len(rows)))
    pieces = []
    with torch.no_grad():
        for start in range(0, len(rows), chunk):
            pieces.append(model(*_tensor(rows[start:start + chunk], torch, device)).detach().cpu().numpy())
    return np.concatenate(pieces, axis=0)


def _select_model(calibration_scores):
    return max(calibration_scores, key=lambda name: (calibration_scores[name]["macro_f1"], name == "fused"))


def _sha256(*paths: Path) -> str:
    digest = hashlib.sha256()
    for path in paths:
        digest.update(path.read_bytes())
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


def evaluate_sealed_once(model, rows, labels, torch, *, temperature=1.0, reject_id=None, top_threshold=0.0, margin=0.0, on_evaluate=None, device=None, batch_size=None):
    if on_evaluate is not None:
        on_evaluate("sealed")
    expected = np.array([labels[row["label"]] for row in rows], dtype=np.int64)
    return evaluate(_logits(model, rows, torch, device, batch_size), expected, temperature, reject_id=reject_id, top_threshold=top_threshold, margin=margin)


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


def _development_validation(labels, corpus: Path, manifest_path: Path):
    deficits = []
    try:
        manifest = json.loads(manifest_path.read_text())
    except (OSError, json.JSONDecodeError) as exc:
        return None, [f"generated manifest: invalid JSON ({exc})"]
    if manifest.get("profile") != "synthetic-development": deficits.append("generated manifest: profile must be synthetic-development")
    if manifest.get("source") != "synthetic": deficits.append("generated manifest: source must be synthetic")
    if manifest.get("release_ready") is not False: deficits.append("generated manifest: release_ready must be false")
    try: examples = load_examples(corpus)
    except (ValueError, OSError) as exc: return manifest, [f"generated corpus: invalid records ({exc})"]
    positives = [label for label in labels if label != "reject"]
    expected_counts = manifest.get("counts")
    if not isinstance(expected_counts, dict): deficits.append("generated manifest: counts required"); expected_counts = {}
    actual_counts = {label: 0 for label in labels}; groups = {label: set() for label in labels}; seeds = {label: set() for label in labels}
    for example in examples:
        if example.source not in {"synthetic", "reject"}: deficits.append(f"{example.example_id}: non-synthetic source"); continue
        if example.source == "synthetic" and (example.generation is None or example.generation.get("profile") != "synthetic-development"): deficits.append(f"{example.example_id}: generation profile must be synthetic-development")
        if example.label not in actual_counts: deficits.append(f"{example.example_id}: unsupported label"); continue
        actual_counts[example.label] += 1; groups[example.label].add(example.split_group)
        if example.label != "reject": seeds[example.label].add(example.seed_id)
    if expected_counts != actual_counts: deficits.append(f"generated manifest: counts mismatch (expected {expected_counts}, actual {actual_counts})")
    if any(actual_counts[label] < 100 for label in positives + ["reject"]): deficits.append("generated corpus: requires at least 100 examples per label including reject")
    actual_groups = {label: sorted(values) for label, values in groups.items()}
    if not isinstance(manifest.get("groups"), dict) or manifest["groups"] != actual_groups: deficits.append("generated manifest: groups mismatch")
    if any(len(seeds[label]) < 3 for label in positives): deficits.append("generated corpus: requires at least 3 seed variants per positive label")
    if manifest.get("corpus_sha256") != _sha256(corpus): deficits.append("generated manifest: corpus_sha256 mismatch")
    return manifest, deficits


def main(argv=None):
    parser = argparse.ArgumentParser(); parser.add_argument("--config", required=True); args = parser.parse_args(argv)
    config_path = Path(args.config); config = json.loads(config_path.read_text()); base = config_path.parent
    profile = config.get("profile", "production")
    catalog = _resolve(base, config["catalog"]); output = _resolve(base, config["output"])
    catalog_object = json.loads(catalog.read_text()) if catalog.exists() else {}
    labels = [item.get("id", item) if isinstance(item, dict) else item for item in catalog_object.get("labels", [])]
    if labels != list(LABELS): print("catalog does not match the supported glyph labels"); return 2
    preprocessing = _resolve(base, config.get("preprocessing", "preprocessing-v1.json"))
    if profile == "synthetic-development":
        corpus = _resolve(base, config.get("generated_corpus", config.get("seeds", ""))); manifest_path = _resolve(base, config.get("generated_manifest", config.get("review", "")))
        manifest, deficits = _development_validation(labels, corpus, manifest_path)
        if deficits:
            for deficit in deficits: print(deficit)
            return 2
        seeds = players = corpus; dataset_id = _sha256(corpus, manifest_path)
    elif profile == "production":
        seeds = _resolve(base, config["seeds"]); review = _resolve(base, config["review"]); players = _resolve(base, config.get("player_data", "player-data"))
        gate = validate_dataset(labels, seeds, review, players)
        if not gate.ok:
            for deficit in gate.deficits: print(deficit)
            return 2
        manifest = None; dataset_id = _sha256(seeds, players)
    else:
        print(f"unsupported training profile: {profile}"); return 2
    try:
        seed, folds, test_ratio = _validate_config(config)
        examples = load_examples(seeds) + ([] if profile == "synthetic-development" else load_examples(players))
        rows = _rows(examples); _validate_rows(rows)
        partitions = grouped_cross_validation_split(rows, folds=folds, test_ratio=test_ratio, seed=seed)
        validate_partition_isolation([partitions["test"], *partitions["folds"]])
    except (TypeError, ValueError, OverflowError) as exc:
        print(exc); return 2
    import torch
    from .export import export_bundle
    from .model import FusedClassifier, RasterClassifier, VectorClassifier
    device = _resolve_device(config, torch); gpu_name = torch.cuda.get_device_name(device) if device.type == "cuda" else None
    embedding_dim = int(config.get("embedding_dim", 16)); constructors = {"vector": lambda: VectorClassifier(len(labels), embedding_dim), "raster": lambda: RasterClassifier(len(labels), embedding_dim), "fused": lambda: FusedClassifier(len(labels), embedding_dim)}
    candidates = [dict(value) for value in (config.get("candidates") or [{"model": name} for name in constructors])]
    unknown = sorted({candidate.get("model") for candidate in candidates} - constructors.keys())
    if unknown: print(f"unknown candidate model(s): {', '.join(map(str, unknown))}"); return 2
    label_to_id = {label: index for index, label in enumerate(labels)}
    infer_batch = int(config.get("batch_size") or 256)
    def train_fold(candidate, training_rows, validation_rows=None):
        torch.manual_seed(seed); model = _adapt(candidate["model"], constructors[candidate["model"]], torch)
        fit_result = _fit(model, _augment_training_rows(training_rows, int(config.get("training_augmentations", 0)), seed), labels, {**config, **candidate}, torch, device, validation_rows=validation_rows)
        if isinstance(fit_result, dict):
            model = fit_result["model"]
            history = fit_result.get("history", [])
        else:
            history = []
        return {"model": model, "parameters": sum(parameter.numel() for parameter in model.parameters()), "history": history}
    def evaluate_fold(model, validation_rows):
        expected = np.array([label_to_id[row["label"]] for row in validation_rows], dtype=np.int64)
        return evaluate(_logits(model, validation_rows, torch, device, infer_batch), expected)
    cv_results = run_cross_validation(partitions["folds"], candidates, train_fold, evaluate_fold, lambda value: value)
    winner = rank_candidates(cv_results); selected_candidate = winner["candidate"]
    final_train, calibration_rows = _calibration_split(partitions["folds"]); selected_info = train_fold(selected_candidate, final_train, calibration_rows); selected = selected_info["model"]; final_history = selected_info.get("history", [])
    calibration_labels = np.array([label_to_id[row["label"]] for row in calibration_rows], dtype=np.int64); calibration_logits = _logits(selected, calibration_rows, torch, device, infer_batch)
    temperature = calibrate_temperature(calibration_logits, calibration_labels); top_threshold, margin = select_thresholds(calibration_logits, calibration_labels, label_to_id["reject"], temperature)
    test_rows = partitions["test"]; test_labels = np.array([label_to_id[row["label"]] for row in test_rows], dtype=np.int64)
    metrics = evaluate_sealed_once(selected, test_rows, label_to_id, torch, temperature=temperature, reject_id=label_to_id["reject"], top_threshold=top_threshold, margin=margin, device=device, batch_size=infer_batch)
    metrics["profile"] = profile; metrics["source"] = "synthetic" if profile == "synthetic-development" else "reviewed-player"
    release_ready = profile == "production" and metrics["count"] > 0 and metrics["accuracy"] >= float(config.get("min_accuracy", 0.0)) and metrics["reject_false_accept_rate"] <= float(config.get("max_reject_false_accept_rate", 1.0)) and top_threshold > 0 and margin > 0
    metadata = {"model": selected_candidate["model"], "embedding_dim": embedding_dim, "model_id": f'{selected_candidate["model"]}-glyph-v1', "catalog_id": _sha256(catalog), "preprocessing_id": _sha256(preprocessing), "training_id": _sha256(config_path), "dataset_id": dataset_id, "temperature": temperature, "top_threshold": top_threshold, "margin": margin, "metrics": metrics, "release_ready": release_ready, "selected_candidate": selected_candidate, "cross_validation": {"candidates": cv_results, "winner": winner}, "training_device": device.type, "gpu_name": gpu_name, "profile": profile, "source": "synthetic" if profile == "synthetic-development" else "reviewed-player", "hyperparameters": {"seed": seed, "epochs": int(config.get("epochs", 2)), "embedding_dim": embedding_dim, "learning_rate": float(config.get("learning_rate", 1e-3)), "optimizer": str(config.get("optimizer", "adam")), "momentum": config.get("momentum"), "weight_decay": config.get("weight_decay"), "label_smoothing": float(config.get("label_smoothing", 0.0)), "training_augmentations": int(config.get("training_augmentations", 0)), "batch_size": int(config.get("batch_size") or 0)}}
    best_epoch = max((h for h in final_history), key=lambda h: h.get("val_macro_f1", -float("inf")))["epoch"] if final_history else None
    metadata["best_epoch"] = best_epoch
    metadata["training_history"] = final_history
    if manifest is not None: metadata["synthetic_manifest"] = {"profile": manifest.get("profile"), "counts": manifest.get("counts"), "groups": manifest.get("groups"), "corpus_sha256": manifest.get("corpus_sha256")}
    output.parent.mkdir(parents=True, exist_ok=True); temporary = Path(tempfile.mkdtemp(prefix=output.name + ".", dir=output.parent)); backup = output.with_name(output.name + ".previous"); stale = backup.with_name(backup.name + ".stale")
    try:
        selected = selected.to("cpu"); export_bundle(selected, _public_export_inputs(torch), temporary, labels, metadata=metadata)
        if final_history:
            (temporary / "history.json").write_text(json.dumps({"history": final_history, "best_epoch": best_epoch}, indent=2, sort_keys=True) + "\n")
        if stale.exists(): shutil.rmtree(stale)
        if backup.exists(): backup.replace(stale)
        if output.exists(): output.replace(backup)
        try: temporary.replace(output)
        except Exception:
            if backup.exists(): backup.replace(output)
            raise
        if stale.exists(): shutil.rmtree(stale)
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True); raise
    return 0 if release_ready else 3


if __name__ == "__main__": raise SystemExit(main())
