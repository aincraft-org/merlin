"""Compare shipped training-loop optimizers on a fixed grouped split."""
from __future__ import annotations

import json
import time
from pathlib import Path

import numpy as np

from .evaluate import evaluate
from .experiment_report import write_f1_overlay, write_loss_curve, write_loss_overlay, write_report
from .model import FusedClassifier
from .schema import LABELS
from .split import grouped_cross_validation_split, validate_partition_isolation
from .train import _calibration_split, _fit, _logits, _validate_rows


def _best_epoch(history):
    if not history:
        return None
    return max(history, key=lambda row: row.get("val_macro_f1", -float("inf")))["epoch"]


def run_optimizer_sweep(rows, labels, configs, output: Path, *, seed=1729, folds=5, test_ratio=0.15, device="cpu", torch=None):
    if torch is None:
        import torch
    _validate_rows(rows)
    partitions = grouped_cross_validation_split(rows, folds=folds, test_ratio=test_ratio, seed=seed)
    validate_partition_isolation([partitions["test"], *partitions["folds"]])
    training_rows, calibration_rows = _calibration_split(partitions["folds"])
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    resolved = torch.device(device)
    experiments = []
    for config in configs:
        name = config["name"]
        train_config = {key: value for key, value in config.items() if key != "name"}
        torch.manual_seed(seed)
        model = FusedClassifier(len(labels), int(train_config.get("embedding_dim", 32))).to(resolved)
        started = time.perf_counter()
        fit = _fit(model, training_rows, labels, train_config, torch, resolved, validation_rows=calibration_rows)
        seconds = time.perf_counter() - started
        trained = fit["model"]
        history = fit["history"]
        calibration_labels = np.array([labels.index(row["label"]) for row in calibration_rows], dtype=np.int64)
        calibration = evaluate(_logits(trained, calibration_rows, torch, resolved), calibration_labels)
        row = {
            "name": name,
            "config": train_config,
            "calibration_macro_f1": float(calibration["macro_f1"]),
            "calibration_metrics": calibration,
            "history": history,
            "best_epoch": _best_epoch(history),
            "parameters": int(sum(parameter.numel() for parameter in trained.parameters())),
            "seconds": seconds,
            "test_metrics": None,
            "optimizer": str(train_config.get("optimizer", "adam")),
        }
        experiments.append(row)
        write_loss_curve(output / f"loss-curve-{name}.svg", history, title=name, best_epoch=row["best_epoch"])
    winner = max(experiments, key=lambda row: (row["calibration_macro_f1"], -row["parameters"], -row["seconds"]))
    winner_model_config = {key: value for key, value in winner["config"].items()}
    torch.manual_seed(seed)
    winner_model = FusedClassifier(len(labels), int(winner_model_config.get("embedding_dim", 32))).to(resolved)
    winner_fit = _fit(winner_model, training_rows, labels, winner_model_config, torch, resolved, validation_rows=calibration_rows)
    test_labels = np.array([labels.index(row["label"]) for row in partitions["test"]], dtype=np.int64)
    winner["test_metrics"] = evaluate(_logits(winner_fit["model"], partitions["test"], torch, resolved), test_labels)
    write_loss_overlay(output / "loss-overlay.svg", experiments)
    stable = [row for row in experiments if max((item.get("val_loss", 0.0) for item in row["history"]), default=0.0) < 10]
    if stable:
        write_loss_overlay(output / "loss-overlay-stable.svg", stable)
    write_f1_overlay(output / "f1-overlay.svg", experiments)
    write_report(output, experiments, list(labels), winner=winner["name"])
    (output / "sweep.json").write_text(json.dumps({
        "winner": winner["name"],
        "selection": "calibration_macro_f1",
        "experiments": experiments,
    }, indent=2, sort_keys=True) + "\n")
    return {"winner": winner["name"], "experiments": experiments}


if __name__ == "__main__":
    raise SystemExit("use the experiment script entrypoint")
