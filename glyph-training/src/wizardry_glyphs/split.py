from __future__ import annotations

import random
from collections import defaultdict


def grouped_split(rows, seed=0, ratios=(0.7, 0.15, 0.15)):
    groups = defaultdict(list)
    for row in rows:
        key = row.get("split_group") or row.get("session_group") or row.get("author_group") or row.get("seed_id")
        groups[key].append(row)
    keys = list(groups)
    random.Random(seed).shuffle(keys)
    total = len(rows)
    targets = [total * r for r in ratios]
    out = {"train": [], "calibration": [], "test": []}
    counts = [0, 0, 0]
    for key in keys:
        idx = min(range(3), key=lambda i: counts[i] / max(targets[i], 1e-9))
        name = ("train", "calibration", "test")[idx]
        out[name].extend(groups[key])
        counts[idx] += len(groups[key])
    return out


def _validated_groups(rows):
    groups = defaultdict(list)
    labels = set()
    for row in rows:
        if "label" not in row:
            raise ValueError("row requires label")
        lineage = row.get("lineage_group")
        if not lineage:
            raise ValueError("row requires nonempty lineage_group")
        groups[lineage].append(row)
        labels.add(row["label"])
    for lineage, group_rows in groups.items():
        group_labels = {row["label"] for row in group_rows}
        if len(group_labels) > 1:
            raise ValueError(f"lineage_group {lineage!r} has mixed labels")
    return groups, labels


def grouped_cross_validation_split(rows, *, folds=5, test_ratio=0.15, seed=0):
    if folds < 2:
        raise ValueError("folds must be at least 2")
    if not 0 < test_ratio < 1:
        raise ValueError("test_ratio must be between 0 and 1")
    groups, _ = _validated_groups(rows)
    by_label = defaultdict(list)
    for lineage, group_rows in groups.items():
        by_label[group_rows[0]["label"]].append((lineage, group_rows))
    for label, label_groups in by_label.items():
        if len(label_groups) < folds + 1:
            raise ValueError(f"label {label!r}: required 6 independent lineages (required 6), actual count {len(label_groups)}")
    test, folds_out = [], [[] for _ in range(folds)]
    rng = random.Random(seed)
    for label in sorted(by_label, key=str):
        groups_for_label = list(by_label[label])
        rng.shuffle(groups_for_label)
        reserve = min(max(1, round(len(groups_for_label) * test_ratio)), len(groups_for_label) - folds)
        test.extend(row for _, group in groups_for_label[:reserve] for row in group)
        counts = [0] * folds
        for _, group in groups_for_label[reserve:]:
            index = min(range(folds), key=lambda i: (counts[i], i))
            folds_out[index].extend(group)
            counts[index] += len(group)
    return {"test": test, "folds": folds_out}


def validate_partition_isolation(partitions):
    if not partitions:
        raise ValueError("partitions must not be empty")
    corpus_labels = {row["label"] for partition in partitions for row in partition}
    seen = set()
    for partition in partitions:
        labels = {row["label"] for row in partition}
        missing = corpus_labels - labels
        if missing:
            raise ValueError(f"partition missing corpus label(s): {sorted(missing, key=str)}")
        for row in partition:
            lineage = row.get("lineage_group")
            if lineage in seen:
                raise ValueError(f"lineage_group overlap: {lineage!r}")
            seen.add(lineage)
