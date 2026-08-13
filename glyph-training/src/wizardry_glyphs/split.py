from __future__ import annotations
import random
from collections import defaultdict

def grouped_split(rows, seed=0, ratios=(0.7, 0.15, 0.15)):
    groups = defaultdict(list)
    for row in rows:
        key = row.get("split_group") or row.get("session_group") or row.get("author_group") or row.get("seed_id")
        groups[key].append(row)
    keys = list(groups); random.Random(seed).shuffle(keys)
    total = len(rows); targets = [total * r for r in ratios]; out = {"train": [], "calibration": [], "test": []}; counts = [0,0,0]
    for key in keys:
        idx = min(range(3), key=lambda i: counts[i] / max(targets[i], 1e-9))
        name = ("train", "calibration", "test")[idx]; out[name].extend(groups[key]); counts[idx] += len(groups[key])
    return out
