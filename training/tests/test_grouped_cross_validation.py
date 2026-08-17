import pytest

from wizardry_glyphs.split import grouped_cross_validation_split, validate_partition_isolation


def make_rows(groups=(6, 7), labels=("a", "b")):
    rows = []
    for label, count in zip(labels, groups):
        for group_index in range(count):
            for sample in range(group_index + 1):
                rows.append({"label": label, "lineage_group": f"{label}-{group_index}", "id": f"{label}-{group_index}-{sample}"})
    return rows


def test_invalid_rows_and_parameters():
    with pytest.raises(ValueError, match="lineage_group"):
        grouped_cross_validation_split([{"label": "a"}])
    with pytest.raises(ValueError, match="lineage_group"):
        grouped_cross_validation_split([{"label": "a", "lineage_group": ""}])
    with pytest.raises(ValueError, match="lineage_group"):
        grouped_cross_validation_split([{"label": "a", "lineage_group": "   "}])
    with pytest.raises(ValueError, match="mixed labels"):
        grouped_cross_validation_split([
            {"label": "a", "lineage_group": "shared"},
            {"label": "b", "lineage_group": "shared"},
        ])
    with pytest.raises(ValueError, match=r"label \x27a\x27.*required 6.*actual 5"):
        grouped_cross_validation_split(make_rows(groups=(5, 6)))
    with pytest.raises(ValueError, match="folds"):
        grouped_cross_validation_split(make_rows(), folds=1)
    with pytest.raises(ValueError, match="test_ratio"):
        grouped_cross_validation_split(make_rows(), test_ratio=1.0)


def test_stratified_complete_deterministic_group_folds():
    rows = make_rows()
    first = grouped_cross_validation_split(rows, seed=19)
    second = grouped_cross_validation_split(rows, seed=19)
    other = grouped_cross_validation_split(rows, seed=20)
    assert first == second
    assert first != other
    assert len(first["folds"]) == 5
    assert all({row["label"] for row in fold} == {"a", "b"} for fold in first["folds"])
    partitions = [first["test"], *first["folds"]]
    validate_partition_isolation(partitions)
    locations = {}
    for partition_index, partition in enumerate(partitions):
        for row in partition:
            locations.setdefault(row["id"], set()).add(partition_index)
    assert all(len(indexes) == 1 for indexes in locations.values())
    input_by_lineage = {}
    for row in rows:
        input_by_lineage.setdefault(row["lineage_group"], set()).add(row["id"])
    output_by_lineage = {}
    for partition_index, partition in enumerate(partitions):
        for row in partition:
            output_by_lineage.setdefault(row["lineage_group"], set()).add((partition_index, row["id"]))
    assert {lineage: {row_id for _, row_id in entries} for lineage, entries in output_by_lineage.items()} == input_by_lineage
    assert all(len({partition_index for partition_index, _ in entries}) == 1 for entries in output_by_lineage.values())


def test_isolation_validator_rejects_overlap_and_missing_label():
    with pytest.raises(ValueError, match="overlap"):
        validate_partition_isolation([
            [{"label": "a", "lineage_group": "g"}],
            [{"label": "a", "lineage_group": "g"}],
        ])
    with pytest.raises(ValueError, match="missing corpus label"):
        validate_partition_isolation([
            [{"label": "a", "lineage_group": "a1"}, {"label": "b", "lineage_group": "b1"}],
            [{"label": "a", "lineage_group": "a2"}],
        ])
    with pytest.raises(ValueError, match="multiple labels"):
        validate_partition_isolation([
            [
                {"label": "a", "lineage_group": "shared"},
                {"label": "b", "lineage_group": "shared"},
            ],
        ])
