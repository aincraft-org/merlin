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
        if not isinstance(lineage, str) or not lineage.strip():
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
    deficiencies = [(label, folds + 1, len(gs)) for label, gs in by_label.items() if len(gs) < folds + 1]
    if deficiencies:
        details = "; ".join(f"label {label!r}: required {required}, actual {actual}" for label, required, actual in sorted(deficiencies, key=lambda x: str(x[0])))
        raise ValueError(f"insufficient lineage groups: {details}")
    test, folds_out = [], [[] for _ in range(folds)]
    rng = random.Random(seed)
    for label in sorted(by_label, key=str):
        gs = list(by_label[label]); rng.shuffle(gs)
        reserve = min(max(1, round(len(gs) * test_ratio)), len(gs) - folds)
        test.extend(row for _, group in gs[:reserve] for row in group)
        counts = [0] * folds
        for _, group in gs[reserve:]:
            i = min(range(folds), key=lambda i: (counts[i], i)); folds_out[i].extend(group); counts[i] += len(group)
    return {"test": test, "folds": folds_out}


def validate_partition_isolation(partitions):
    if not partitions:
        raise ValueError("partitions must not be empty")
    corpus_labels = {row["label"] for partition in partitions for row in partition}
    lineage_partition = {}
    lineage_labels = {}
    for partition_index, partition in enumerate(partitions):
        labels = {row["label"] for row in partition}
        missing = corpus_labels - labels
        if missing:
            raise ValueError(f"partition missing corpus label(s): {sorted(missing, key=str)}")
        for row in partition:
            lineage = row.get("lineage_group")
            if not isinstance(lineage, str) or not lineage.strip():
                raise ValueError("row requires nonempty lineage_group")
            prior_label = lineage_labels.setdefault(lineage, row["label"])
            if prior_label != row["label"]:
                raise ValueError(f"lineage_group {lineage!r} contains multiple labels")
            prior = lineage_partition.setdefault(lineage, partition_index)
            if prior != partition_index:
                raise ValueError(f"lineage_group overlap: {lineage!r}")
