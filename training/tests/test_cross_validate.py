import inspect
import statistics

import pytest

from wizardry_glyphs.cross_validate import (
    aggregate_fold_metrics,
    rank_candidates,
    run_cross_validation,
)


def metric(value, labels=("a", "b")):
    return {
        "macro_f1": value,
        "per_class": [
            {"label": label, "f1": value + index / 10}
            for index, label in enumerate(labels)
        ],
    }


def test_aggregate_metrics_preserves_raw_and_uses_sample_stdev():
    metrics = [metric(0.5), metric(0.7), metric(0.9)]
    result = aggregate_fold_metrics(metrics)
    assert result["macro_f1_mean"] == pytest.approx(0.7)
    assert result["macro_f1_stdev"] == pytest.approx(statistics.stdev([0.5, 0.7, 0.9]))
    assert result["macro_f1_min"] == 0.5
    assert result["folds"] == metrics
    assert result["per_class"] == [
        {"label": "a", "f1_mean": pytest.approx(0.7)},
        {"label": "b", "f1_mean": pytest.approx(0.8)},
    ]


def test_aggregate_rejects_empty_or_inconsistent_labels():
    with pytest.raises(ValueError, match="empty"):
        aggregate_fold_metrics([])
    with pytest.raises(ValueError, match="label"):
        aggregate_fold_metrics([metric(0.5), metric(0.6, labels=("b", "a"))])


def test_rank_candidates_uses_mean_stdev_parameters_runtime():
    candidates = [
        {"id": "slow", "macro_f1_mean": 0.8, "macro_f1_stdev": 0.1, "parameters": 1, "runtime": 3},
        {"id": "params", "macro_f1_mean": 0.8, "macro_f1_stdev": 0.1, "parameters": 2, "runtime": 1},
        {"id": "winner", "macro_f1_mean": 0.8, "macro_f1_stdev": 0.05, "parameters": 9, "runtime": 9},
    ]
    assert rank_candidates(candidates)["id"] == "winner"
    assert rank_candidates([candidates[0], candidates[1]])["id"] == "slow"


def test_runner_has_no_sealed_test_and_keeps_validation_out_of_augmentation():
    assert "test" not in inspect.signature(run_cross_validation).parameters
    folds = [[{"id": index}] for index in range(5)]
    seen = []

    def augment(train):
        ids = [row["id"] for row in train]
        seen.append(("augment", ids))
        return train + [{"id": "augmented"}]

    def train(candidate, train):
        ids = [row["id"] for row in train]
        seen.append(("train", ids))
        return {"model": object(), "parameters": 42}

    def evaluate(model, validation):
        seen.append(("evaluate", {row["id"] for row in validation}))
        return metric(0.5 + validation[0]["id"] / 100)

    result = run_cross_validation(folds, [{"id": "candidate"}], train, evaluate, augment)
    assert len(result) == 1
    assert result[0]["folds"] == [
        metric(0.5 + index / 100) for index in range(5)
    ]
    assert result[0]["parameters"] == 42
    for index in range(5):
        assert ("evaluate", {index}) in seen
        expected_train = [fold_id for fold_id in range(5) if fold_id != index]
        augment_ids = next(ids for kind, ids in seen if kind == "augment" and index not in ids)
        train_ids = next(ids for kind, ids in seen if kind == "train" and index not in ids)
        assert train_ids == expected_train + ["augmented"]
def test_runner_accepts_configured_fold_count():
    folds = [[{"id": index}] for index in range(3)]
    result = run_cross_validation(
        folds,
        [{"id": "candidate"}],
        lambda candidate, rows: {"model": object(), "parameters": len(rows)},
        lambda model, rows: metric(0.5),
        lambda rows: rows,
    )
    assert len(result[0]["folds"]) == 3

def test_runner_passes_validation_rows_when_train_accepts_them():
    folds = [[{"id": index}] for index in range(3)]
    seen = []

    def train(candidate, train, validation):
        seen.append((len(train), len(validation)))
        return {"model": object(), "parameters": 1}

    def evaluate(model, validation):
        return metric(0.5)

    result = run_cross_validation(folds, [{}], train, evaluate, lambda rows: rows)
    assert len(seen) == 3
    assert all(train_len == 2 and val_len == 1 for train_len, val_len in seen)
