import pytest


def test_calibration_split_keeps_complete_lineages_disjoint():
    from wizardry_glyphs.train import _calibration_split
    folds = []
    for fold in range(5):
        rows = []
        for label in ("a", "b"):
            rows.extend({"label": label, "lineage_group": f"{label}-{fold}-{lineage}", "id": f"{label}-{fold}-{lineage}-{sample}"} for lineage in range(2) for sample in range(2))
        folds.append(rows)
    training, calibration = _calibration_split(folds)
    train_groups = {row["lineage_group"] for row in training}
    calibration_groups = {row["lineage_group"] for row in calibration}
    assert train_groups.isdisjoint(calibration_groups)
    assert {row["label"] for row in training} == {"a", "b"}
    assert {row["label"] for row in calibration} == {"a", "b"}


def test_rows_carries_lineage_and_provenance(monkeypatch):
    from types import SimpleNamespace
    import wizardry_glyphs.train as train
    monkeypatch.setattr(train, "preprocess_example", lambda example: {"vectors": 1, "mask": 2, "raster": 3})
    example = SimpleNamespace(label="cast", lineage_group="lineage", independent_source="source")
    assert train._rows([example])[0] == {"example": example, "vectors": 1, "mask": 2, "raster": 3, "label": "cast", "lineage_group": "lineage", "independent_source": "source"}
def test_provenance_conflicts_rejected_before_model_construction():
    from wizardry_glyphs.train import _validate_rows
    rows = [
        {"label": "a", "lineage_group": "lineage", "independent_source": "source-a"},
        {"label": "a", "lineage_group": "lineage", "independent_source": "source-b"},
    ]
    with pytest.raises(ValueError, match="independent_source"):
        _validate_rows(rows)

def test_config_validation_is_clean_and_strict():
    from wizardry_glyphs.train import _validate_config
    for config in ({"seed": "bad"}, {"folds": 1}, {"test_ratio": 0}, {"test_ratio": float("nan")}):
        with pytest.raises(ValueError):
            _validate_config(config)
def test_sealed_evaluation_spy_runs_once_after_calibration():
    import torch
    from wizardry_glyphs.train import evaluate_sealed_once
    events = []
    class Model:
        def __call__(self, *inputs):
            return torch.tensor([[3.0, 1.0]])
    rows = [{"label": "a", "vectors": torch.zeros(1, 64, 32, 8).numpy(), "mask": torch.zeros(1,64,32).numpy(), "raster": torch.zeros(1,1,64,64).numpy()}]
    labels = {"a": 0}
    events.append("calibration")
    result = evaluate_sealed_once(Model(), rows, labels, torch, on_evaluate=events.append)
    assert events == ["calibration", "sealed"]
    assert result["count"] == 1
