from __future__ import annotations

import inspect
import statistics
import time
from collections.abc import Callable, Sequence
from typing import Any

def _per_class_values(metrics: Sequence[dict[str, Any]]) -> tuple[list[Any], list[list[float]]]:
    first = metrics[0].get("per_class")
    if not isinstance(first, list) or not first:
        raise ValueError("metrics must include non-empty per-class label metrics")
    labels = [entry.get("label") for entry in first]
    if any(label is None for label in labels):
        raise ValueError("per-class metrics require a label")
    values: list[list[float]] = [[] for _ in labels]
    for metric in metrics:
        per_class = metric.get("per_class")
        if not isinstance(per_class, list) or [entry.get("label") for entry in per_class] != labels:
            raise ValueError("inconsistent per-class labels")
        for index, entry in enumerate(per_class):
            if "f1" not in entry:
                raise ValueError("per-class metrics require f1")
            values[index].append(float(entry["f1"]))
    return labels, values


def aggregate_fold_metrics(metrics: list[dict]) -> dict:
    if not metrics:
        raise ValueError("cannot aggregate empty metrics")
    try:
        macro_f1 = [float(metric["macro_f1"]) for metric in metrics]
    except (KeyError, TypeError, ValueError) as exc:
        raise ValueError("metrics require macro_f1") from exc
    labels, class_values = _per_class_values(metrics)
    return {
        "macro_f1_mean": statistics.mean(macro_f1),
        "macro_f1_stdev": statistics.stdev(macro_f1) if len(macro_f1) > 1 else 0.0,
        "macro_f1_min": min(macro_f1),
        "folds": list(metrics),
        "per_class": [
            {"label": label, "f1_mean": statistics.mean(values)}
            for label, values in zip(labels, class_values)
        ],
    }


def rank_candidates(candidates: list[dict]) -> dict:
    if not candidates:
        raise ValueError("cannot rank empty candidates")
    return min(
        candidates,
        key=lambda candidate: (
            -float(candidate["macro_f1_mean"]),
            float(candidate["macro_f1_stdev"]),
            int(candidate["parameters"]),
            float(candidate["runtime"]),
        ),
    )


def _call_train(train_fold: Callable, candidate: dict, train_rows: list[Any], validation_rows: list[Any]) -> Any:
    sig = inspect.signature(train_fold)
    if len(sig.parameters) >= 3:
        return train_fold(candidate, train_rows, validation_rows)
    return train_fold(candidate, train_rows)

def _parameter_count(trained: Any) -> int:
    if isinstance(trained, dict):
        for key in ("parameters", "parameter_count", "num_parameters"):
            if key in trained:
                return int(trained[key])
    for key in ("parameters", "parameter_count", "num_parameters"):
        if hasattr(trained, key):
            return int(getattr(trained, key))
    raise ValueError("train_fold result must provide parameter count")


def run_cross_validation(
    folds: Sequence[Sequence[Any]],
    candidates: Sequence[dict],
    train_fold: Callable,
    evaluate_fold: Callable,
    augment_fold: Callable,
) -> list[dict]:
    if len(folds) < 2:
        raise ValueError("cross-validation requires at least two folds")
    results = []
    for candidate in candidates:
        fold_metrics = []
        parameters = None
        started = time.perf_counter()
        for validation_index, validation in enumerate(folds):
            training = [row for index, fold in enumerate(folds) if index != validation_index for row in fold]
            augmented = augment_fold(training)
            trained = _call_train(train_fold, candidate, augmented, validation)
            parameters = _parameter_count(trained) if parameters is None else parameters
            model = trained.get("model") if isinstance(trained, dict) else trained
            fold_metrics.append(evaluate_fold(model, validation))
        aggregate = aggregate_fold_metrics(fold_metrics)
        aggregate.update({"candidate": candidate, "parameters": parameters, "runtime": time.perf_counter() - started})
        results.append(aggregate)
    return results
