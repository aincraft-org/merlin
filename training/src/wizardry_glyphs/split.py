from __future__ import annotations

import random
from collections import defaultdict


def _group_key(row):
    return row.get("split_group") or row.get("session_group") or row.get("author_group") or row.get("seed_id")

def grouped_split(rows, seed=0, ratios=(0.7, 0.15, 0.15)):
    groups = defaultdict(list)
    group_labels = {}
    for row in rows:
        if "label" not in row:
            raise ValueError("row requires label")
        key = _group_key(row)
        prior = group_labels.setdefault(key, row["label"])
        if prior != row["label"]:
            raise ValueError(f"split group {key!r} contains multiple labels")
        groups[key].append(row)
    by_label = defaultdict(list)
    for key, members in groups.items():
        by_label[group_labels[key]].append((key, members))
    names = ("train", "calibration", "test")
    output = {name: [] for name in names}
    rng = random.Random(seed)
    for label in sorted(by_label, key=str):
        values = by_label[label]
        rng.shuffle(values)
        if len(values) >= 3:
            assigned = {name: [] for name in names}
            for name, item in zip(names, values[:3]):
                assigned[name].append(item)
            for item in values[3:]:
                totals = {name: sum(len(rows) for _, rows in assigned[name]) for name in names}
                total_after = sum(totals.values()) + len(item[1])
                targets = {name: total_after * ratio for name, ratio in zip(names, ratios)}
                choice = min(names, key=lambda name: totals[name] / max(targets[name], 1e-9))
                assigned[choice].append(item)
            for name in names:
                for _, members in assigned[name]:
                    output[name].extend(members)
        else:
            for index, (_, members) in enumerate(values):
                output[names[index]].extend(members)
    return output


def _validated_groups(rows):
    groups = defaultdict(list); labels = set()
    for row in rows:
        if "label" not in row: raise ValueError("row requires label")
        lineage = row.get("lineage_group")
        if not isinstance(lineage, str) or not lineage.strip(): raise ValueError("row requires nonempty lineage_group")
        groups[lineage].append(row); labels.add(row["label"])
    for lineage, group_rows in groups.items():
        if len({row["label"] for row in group_rows}) > 1: raise ValueError(f"lineage_group {lineage!r} has mixed labels")
    return groups, labels


def grouped_cross_validation_split(rows, *, folds=5, test_ratio=0.15, seed=0):
    if folds < 2: raise ValueError("folds must be at least 2")
    if not 0 < test_ratio < 1: raise ValueError("test_ratio must be between 0 and 1")
    groups, _ = _validated_groups(rows); by_label = defaultdict(list)
    for lineage, group_rows in groups.items(): by_label[group_rows[0]["label"]].append((lineage, group_rows))
    deficiencies = [(label, folds + 1, len(values)) for label, values in by_label.items() if len(values) < folds + 1]
    if deficiencies:
        details = "; ".join(f"label {label!r}: required {required}, actual {actual}" for label, required, actual in sorted(deficiencies, key=lambda value: str(value[0])))
        raise ValueError(f"insufficient lineage groups: {details}")
    test, folds_out = [], [[] for _ in range(folds)]; rng = random.Random(seed)
    for label in sorted(by_label, key=str):
        values = list(by_label[label]); rng.shuffle(values)
        reserve = min(max(1, round(len(values) * test_ratio)), len(values) - folds)
        test.extend(row for _, group in values[:reserve] for row in group)
        counts = [0] * folds
        for _, group in values[reserve:]:
            index = min(range(folds), key=lambda i: (counts[i], i)); folds_out[index].extend(group); counts[index] += len(group)
    return {"test": test, "folds": folds_out}


def validate_partition_isolation(partitions):
    if not partitions: raise ValueError("partitions must not be empty")
    corpus_labels = {row["label"] for partition in partitions for row in partition}; lineage_partition = {}; lineage_labels = {}
    for partition_index, partition in enumerate(partitions):
        missing = corpus_labels - {row["label"] for row in partition}
        if missing: raise ValueError(f"partition missing corpus label(s): {sorted(missing, key=str)}")
        for row in partition:
            lineage = row.get("lineage_group")
            if not isinstance(lineage, str) or not lineage.strip(): raise ValueError("row requires nonempty lineage_group")
            if lineage_labels.setdefault(lineage, row["label"]) != row["label"]: raise ValueError(f"lineage_group {lineage!r} contains multiple labels")
            if lineage_partition.setdefault(lineage, partition_index) != partition_index: raise ValueError(f"lineage_group overlap: {lineage!r}")
