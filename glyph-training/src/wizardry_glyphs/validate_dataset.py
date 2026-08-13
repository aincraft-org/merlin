"""Hard data-collection gate for glyph training."""
from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from .schema import load_examples

POSITIVE_MIN_SEEDS = 3
PLAYER_MIN = 100

@dataclass(frozen=True)
class ValidationResult:
    ok: bool
    deficits: list[str]

    def __bool__(self):
        return self.ok


def _raw_records(path: Path) -> list[tuple[str, dict]]:
    if not path.exists() or path.is_dir():
        return []
    out = []
    for line in path.read_text().splitlines(keepends=True):
        if not line.strip():
            continue
        out.append((line, json.loads(line)))
    return out


def _manifest_hashes(manifest: dict) -> dict[str, str]:
    value = manifest.get("seeds", manifest.get("reviews", {}))
    if isinstance(value, list):
        return {str(x.get("example_id")): str(x.get("sha256", "")) for x in value if isinstance(x, dict)}
    return {str(k): str(v) for k, v in value.items()} if isinstance(value, dict) else {}


def validate_dataset(catalog: Iterable[str] | Path, seeds: Path, review: Path, player_data: Path) -> ValidationResult:
    if isinstance(catalog, Path):
        catalog_obj = json.loads(catalog.read_text())
        labels = catalog_obj.get("labels", catalog_obj.get("concepts", []))
        labels = [x.get("id", x) if isinstance(x, dict) else x for x in labels]
    else:
        labels = list(catalog)
    if "reject" not in labels:
        labels.append("reject")
    positives = [x for x in labels if x != "reject"]
    deficits: list[str] = []
    seed_raw = _raw_records(seeds)
    try:
        seed_examples = load_examples(seeds) if seed_raw else []
    except (ValueError, json.JSONDecodeError) as exc:
        deficits.append(f"seeds: invalid records ({exc})")
        seed_examples = []
    manifest = {}
    if review.exists() and review.is_file():
        try: manifest = json.loads(review.read_text())
        except json.JSONDecodeError: deficits.append("review: invalid JSON")
    hashes = _manifest_hashes(manifest)
    by_label = {label: [] for label in positives}
    for ex in seed_examples:
        if ex.label in by_label: by_label[ex.label].append(ex)
    for label in positives:
        count = len(by_label[label])
        if count < POSITIVE_MIN_SEEDS:
            deficits.append(f"{label}: missing {POSITIVE_MIN_SEEDS - count} reviewed seeds")
        groups = {ex.author_group for ex in by_label[label]}
        if len(groups) < min(count, POSITIVE_MIN_SEEDS):
            deficits.append(f"{label}: missing independent seed author groups")
        for raw, record in (item for item in seed_raw if item[1].get("label") == label):
            expected = hashlib.sha256(raw.encode()).hexdigest()
            if hashes.get(record.get("example_id")) != expected:
                deficits.append(f"{label}: review hash mismatch for {record.get('example_id')}")
    try:
        player_examples = load_examples(player_data) if player_data.exists() and player_data.is_file() else []
    except (ValueError, json.JSONDecodeError) as exc:
        deficits.append(f"player-data: invalid records ({exc})")
        player_examples = []
    counts = {label: 0 for label in labels}
    for ex in player_examples:
        if ex.label not in counts:
            continue
        if ex.source == "player" and ex.consent is True:
            counts[ex.label] += 1
        elif ex.source == "reject" and ex.label == "reject":
            counts["reject"] += 1
    for label in positives:
        if counts[label] < PLAYER_MIN:
            deficits.append(f"{label}: missing {PLAYER_MIN - counts[label]} consented player examples")
    largest = max((counts[label] for label in positives), default=0)
    if counts.get("reject", 0) < largest:
        deficits.append(f"reject: missing {largest - counts.get('reject', 0)} examples")
    return ValidationResult(not deficits, deficits)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--seeds", type=Path, required=True)
    parser.add_argument("--review", type=Path, required=True)
    parser.add_argument("--player-data", type=Path, required=True)
    args = parser.parse_args(argv)
    result = validate_dataset(args.catalog, args.seeds, args.review, args.player_data)
    if result.ok:
        return 0
    for deficit in result.deficits:
        print(deficit)
    return 2

if __name__ == "__main__":
    raise SystemExit(main())
