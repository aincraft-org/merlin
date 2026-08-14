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
