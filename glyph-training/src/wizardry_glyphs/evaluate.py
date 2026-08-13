from __future__ import annotations

import math
import numpy as np


def _softmax(logits: np.ndarray, temperature: float) -> np.ndarray:
    scaled = np.asarray(logits, dtype=np.float64) / temperature
    scaled -= scaled.max(axis=1, keepdims=True)
    exponent = np.exp(scaled)
    return exponent / exponent.sum(axis=1, keepdims=True)


def _nll(logits: np.ndarray, labels: np.ndarray, temperature: float) -> float:
    probabilities = _softmax(logits, temperature)
    selected = probabilities[np.arange(labels.size), labels]
    return float(-np.log(np.clip(selected, 1e-12, 1)).mean())


def calibrate_temperature(logits, labels) -> float:
    logits = np.asarray(logits, dtype=np.float64)
    labels = np.asarray(labels, dtype=np.int64)
    if logits.ndim != 2 or labels.shape != (logits.shape[0],) or labels.size == 0:
        raise ValueError("invalid calibration data")
    candidates = np.exp(np.linspace(math.log(0.25), math.log(4.0), 121))
    losses = np.array([_nll(logits, labels, float(value)) for value in candidates])
    return float(candidates[int(losses.argmin())])


def select_thresholds(logits, labels, reject_id: int, temperature: float) -> tuple[float, float]:
    probabilities = _softmax(np.asarray(logits), temperature)
    labels = np.asarray(labels, dtype=np.int64)
    ordered = np.sort(probabilities, axis=1)
    top = ordered[:, -1]
    margin = ordered[:, -1] - ordered[:, -2]
    predicted = probabilities.argmax(axis=1)
    correct_positive = (predicted == labels) & (labels != reject_id)
    if not correct_positive.any():
        return 1.0, 1.0
    # Conservative lower decile of held-out correct positives; never emit a zero release gate.
    threshold = max(1e-6, float(np.quantile(top[correct_positive], 0.10)))
    margin_threshold = max(1e-6, float(np.quantile(margin[correct_positive], 0.10)))
    return threshold, margin_threshold


def evaluate(logits, labels, temperature=1.0, *, reject_id=None, top_threshold=0.0, margin=0.0):
    logits = np.asarray(logits, dtype=np.float64)
    labels = np.asarray(labels, dtype=np.int64)
    probabilities = _softmax(logits, float(temperature))
    predicted = probabilities.argmax(axis=1)
    ordered = np.sort(probabilities, axis=1)
    accepted = (ordered[:, -1] >= top_threshold) & ((ordered[:, -1] - ordered[:, -2]) >= margin)
    if reject_id is not None:
        accepted &= predicted != reject_id
    classes = logits.shape[1]
    confusion = np.zeros((classes, classes), dtype=np.int64)
    for expected, actual in zip(labels, predicted):
        confusion[expected, actual] += 1
    per_class = []
    for label in range(classes):
        true_positive = confusion[label, label]
        precision_denominator = confusion[:, label].sum()
        recall_denominator = confusion[label, :].sum()
        per_class.append({
            "label": label,
            "precision": float(true_positive / precision_denominator) if precision_denominator else 0.0,
            "recall": float(true_positive / recall_denominator) if recall_denominator else 0.0,
            "count": int(recall_denominator),
        })
    reject_false_accept = 0.0
    if reject_id is not None and (labels == reject_id).any():
        reject_false_accept = float(accepted[labels == reject_id].mean())
    accepted_correct = accepted & (predicted == labels)
    return {
        "accuracy": float((predicted == labels).mean()),
        "count": int(labels.size),
        "temperature": float(temperature),
        "coverage": float(accepted.mean()),
        "accepted_precision": float(accepted_correct.sum() / accepted.sum()) if accepted.any() else 0.0,
        "reject_false_accept_rate": reject_false_accept,
        "confusion": confusion.tolist(),
        "per_class": per_class,
    }
