"""Development-only synthetic corpus generation and validation."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from collections import Counter
from pathlib import Path
from typing import Any

from .schema import GlyphExample, GlyphPointData, GlyphStrokeData, load_examples

PROFILE = "synthetic-development"
SCHEMA_VERSION = "glyph-dataset-v1"
POSITIVE_LABELS = ("target-ray", "damage", "heal", "push", "cooldown", "self", "target", "physical", "fire", "frost", "arcane")
LABELS = (*POSITIVE_LABELS, "reject")
MIN_TEMPLATES = 6


def _catalog(path: Path) -> tuple[dict[str, Any], str]:
    raw = path.read_bytes()
    value = json.loads(raw)
    if not isinstance(value, dict) or not isinstance(value.get("glyphs"), dict):
        raise ValueError("catalog must contain glyphs object")
    return value, hashlib.sha256(raw).hexdigest()


def _templates(catalog: dict[str, Any]) -> tuple[dict[str, list[dict[str, Any]]], list[str]]:
    deficiencies: list[str] = []
    result: dict[str, list[dict[str, Any]]] = {}
    glyphs = catalog["glyphs"]
    for label in LABELS:
        entry = glyphs.get(label)
        templates = entry.get("templates") if isinstance(entry, dict) else None
        if not isinstance(templates, list):
            templates = []
        valid = [template for template in templates if isinstance(template, dict) and isinstance(template.get("strokes"), list) and template["strokes"]]
        result[label] = valid
        if len(valid) < MIN_TEMPLATES:
            deficiencies.append(f"{label}: requires at least {MIN_TEMPLATES} explicit independent templates (found {len(valid)})")
    if deficiencies:
        raise ValueError("invalid catalog prerequisites:\n" + "\n".join(deficiencies))
    return result, []


def _points(strokes: list[list[list[float]]], seed: int, variant: int) -> list[list[list[float]]]:
    rng = random.Random(seed * 1009 + variant * 9176)
    tx, ty = rng.uniform(-4, 4), rng.uniform(-4, 4)
    scale = 1 + rng.uniform(-.035, .035)
    angle = rng.uniform(-math.radians(8), math.radians(8))
    ca, sa = math.cos(angle), math.sin(angle)
    result = []
    for stroke in strokes:
        transformed = []
        for x, y in stroke:
            xx, yy = (x - 64) * scale, (y - 64) * scale
            transformed.append([round(min(127.5, max(.5, xx * ca - yy * sa + 64 + tx)), 4), round(min(127.5, max(.5, xx * sa + yy * ca + 64 + ty)), 4)])
        result.append(transformed)
    return result


def _record(example_id: str, label: str, strokes: list[list[list[float]]], lineage_group: str, seed_id: str, split_group: str, source: str = "synthetic") -> dict[str, Any]:
    return {"schema_version": SCHEMA_VERSION, "example_id": example_id, "label": label, "source": source,
            "lineage_group": lineage_group, "seed_id": seed_id, "author_group": "synthetic-development", "session_group": seed_id,
            "split_group": split_group, "consent": None,
            "strokes": [{"points": [{"x": x, "y": y} for x, y in stroke], "brush_width": 6.0, "started_at_millis": 0} for stroke in strokes],
            "generation": {"profile": PROFILE, "kind": "seed-variant" if source == "synthetic" else "balanced-reject"}}
from collections import Counter
from pathlib import Path
from typing import Any

from .schema import GlyphExample, GlyphPointData, GlyphStrokeData, load_examples

PROFILE = "synthetic-development"
SCHEMA_VERSION = "glyph-dataset-v1"
POSITIVE_LABELS = ("target-ray", "damage", "heal", "push", "cooldown", "self", "target", "physical", "fire", "frost", "arcane")


def _catalog(path: Path) -> tuple[dict[str, Any], str]:
    raw = path.read_bytes()
    value = json.loads(raw)
    if not isinstance(value, dict) or not isinstance(value.get("glyphs"), dict):
        raise ValueError("catalog must contain glyphs object")
    return value, hashlib.sha256(raw).hexdigest()


def _points(strokes: list[list[list[float]]], seed: int, variant: int) -> list[list[list[float]]]:
    rng = random.Random(seed * 1009 + variant * 9176)
    tx, ty = rng.uniform(-4, 4), rng.uniform(-4, 4)
    scale = 1 + rng.uniform(-.035, .035)
    angle = rng.uniform(-math.radians(8), math.radians(8))
    ca, sa = math.cos(angle), math.sin(angle)
    result = []
    for stroke in strokes:
        transformed = []
        for x, y in stroke:
            xx, yy = (x - 64) * scale, (y - 64) * scale
            transformed.append([round(min(127.5, max(.5, xx * ca - yy * sa + 64 + tx)), 4), round(min(127.5, max(.5, xx * sa + yy * ca + 64 + ty)), 4)])
        result.append(transformed)
    return result




def generate_corpus(catalog_path: Path, output_dir: Path, *, seed_variants: int = 3, derivatives_per_label: int = 100, reject_count: int | None = None) -> dict[str, Any]:
    catalog, geometry_hash = _catalog(catalog_path)
    templates, _ = _templates(catalog)
    output_dir.mkdir(parents=True, exist_ok=True)
    reject_count = reject_count if reject_count is not None else len(POSITIVE_LABELS) * derivatives_per_label
    records: list[dict[str, Any]] = []
    for label in LABELS:
        count = reject_count if label == "reject" else derivatives_per_label
        for template_index, template in enumerate(templates[label]):
            lineage_group = f"catalog:{label}:{template['id']}"
            seed_id = f"geometry:{label}:{template_index}"
            split_group = lineage_group
            source = "reject" if label == "reject" else "synthetic"
            records.append(_record(f"{label}:seed:{template_index}", label, _points(template["strokes"], 17 + template_index, template_index), lineage_group, seed_id, split_group, source))
            for index in range(count):
                records.append(_record(f"{label}:derivative:{template_index}:{index}", label, _points(template["strokes"], 101 + template_index * max(1, count) + index, index + seed_variants), lineage_group, seed_id, split_group, source))
    jsonl = output_dir / "corpus.jsonl"
    jsonl.write_text("".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in records), encoding="utf-8")
    corpus_hash = hashlib.sha256(jsonl.read_bytes()).hexdigest()
    counts = Counter((row["source"], row["label"]) for row in records)
    label_counts = {label: sum(count for (source, counted_label), count in counts.items() if counted_label == label) for label in LABELS}
    groups = {label: sorted({row["split_group"] for row in records if row["label"] == label}) for label in LABELS}
    lineages = {label: sorted({row["lineage_group"] for row in records if row["label"] == label}) for label in LABELS}
    manifest = {"profile": PROFILE, "source": "synthetic", "release_ready": False, "catalog_version": catalog.get("catalog_version"), "geometry_sha256": geometry_hash,
                "corpus_sha256": corpus_hash, "record_count": len(records), "seed_variants_per_label": seed_variants, "derivatives_per_seed": derivatives_per_label,
                "reject_count": reject_count, "counts": label_counts, "source_counts": {f"{source}:{label}": count for (source, label), count in sorted(counts.items())},
                "groups": groups, "lineages": lineages, "lineage_counts": {label: len(lineages[label]) for label in LABELS}}
    (output_dir / "manifest.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return manifest


def validate_development_corpus(path: Path, manifest_path: Path | None = None) -> list[str]:
    errors: list[str] = []
    try:
        examples = load_examples(path)
    except ValueError as exc:
        return [str(exc)]
    manifest = None
    if manifest_path is not None:
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            return [f"invalid manifest: {exc}"]
        if manifest.get("profile") != PROFILE or manifest.get("source") != "synthetic" or manifest.get("release_ready") is not False:
            errors.append("manifest is not development-only")
        if manifest.get("record_count") != len(examples):
            errors.append("manifest record_count mismatch")
        if manifest.get("corpus_sha256") != hashlib.sha256(path.read_bytes()).hexdigest():
            errors.append("manifest corpus_sha256 mismatch")
        if manifest.get("counts") != dict(Counter(example.label for example in examples)):
            errors.append("manifest counts mismatch")
        actual_groups = {label: sorted({example.split_group for example in examples if example.label == label}) for label in LABELS}
        if manifest.get("groups") != actual_groups:
            errors.append("manifest groups mismatch")
        actual_lineages = {label: sorted({example.lineage_group for example in examples if example.label == label}) for label in LABELS}
        if manifest.get("lineages") != actual_lineages:
            errors.append("manifest lineages mismatch")
        if manifest.get("lineage_counts") != {label: len(actual_lineages[label]) for label in LABELS}:
            errors.append("manifest lineage_counts mismatch")
    for example in examples:
        if example.source not in {"synthetic", "reject"}:
            errors.append(f"{example.example_id}: non-development source")
        if example.generation is None or example.generation.get("profile") != PROFILE:
            errors.append(f"{example.example_id}: missing development profile")
        if example.source == "synthetic" and example.seed_id is None:
            errors.append(f"{example.example_id}: missing seed_id")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args(argv)
    manifest = generate_corpus(args.catalog, args.output)
    errors = validate_development_corpus(args.output / "corpus.jsonl", args.output / "manifest.json")
    print(json.dumps({"manifest": manifest, "valid": not errors, "errors": errors}, sort_keys=True))
    return 0 if not errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
