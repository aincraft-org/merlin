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
        seen.append(("augment", {row["id"] for row in train}))
        return train + [{"id": "augmented"}]

    def train(candidate, train):
        seen.append(("train", {row["id"] for row in train}))
        return {"model": object(), "parameters": 42}

    def evaluate(model, validation):
        seen.append(("evaluate", {row["id"] for row in validation}))
        return metric(0.5 + validation[0]["id"] / 100)

    result = run_cross_validation(folds, [{"id": "candidate"}], train, evaluate, augment)
    assert len(result) == 1
    assert result[0]["folds"]
    assert result[0]["parameters"] == 42
    for index in range(5):
        assert ("evaluate", {index}) in seen
        train_ids = next(ids for kind, ids in seen if kind == "train" and index not in ids)
        assert index not in train_ids
        assert "augmented" in train_ids
