"""Ink-mixing experiment generator for rune reference shapes.

Produces a development-only corpus whose LABELS are the four element inks
(flame/frost/arcane/physical) plus reject. The six rune shapes are reference
geometry only: each shape is drawn in a single element ink (the semantic label)
and in controlled mixed-ink variants where every stroke carries a recorded
element. This keeps the element labels semantically meaningful and does not
introduce rune names as classifier labels.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
from collections import Counter
from pathlib import Path
from typing import Any

from .element import ELEMENTS
from .schema import load_examples

PROFILE = "rune-ink-experiment-v1"
SCHEMA_VERSION = "glyph-dataset-v1"
ELEMENT_LABELS = ("fire", "frost", "arcane", "physical")
LABELS = (*ELEMENT_LABELS, "reject")
MIN_TEMPLATES = 1
FINGERPRINT_QUANTIZATION = 1e-3
PROVENANCE_CONFLICT = "independent_source conflict within lineage"

# Canonical element ink keys recorded on strokes. `fire` is the model label and
# ink key; `flame` is a color alias for the same ink (schema accepts both), so
# only the canonical key is used here to keep mixed-ink variants truly distinct.
INK_NAMES = ("fire", "frost", "arcane", "physical")


def _validate_strokes(strokes):
    if not isinstance(strokes, list) or not strokes:
        return "strokes must be a nonempty list"
    for stroke_index, stroke in enumerate(strokes):
        if not isinstance(stroke, list) or not stroke:
            return f"stroke {stroke_index} must be a nonempty list"
        for point_index, point in enumerate(stroke):
            if not isinstance(point, (list, tuple)) or len(point) != 2:
                return f"stroke {stroke_index} point {point_index} must contain exactly two numbers"
            if any(not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value) for value in point):
                return f"stroke {stroke_index} point {point_index} must contain exactly two finite numbers"
    return None


def _catalog(path: Path):
    raw = path.read_bytes()
    value = json.loads(raw)
    if not isinstance(value, dict) or not isinstance(value.get("glyphs"), dict):
        raise ValueError("catalog must contain glyphs object")
    if value.get("status") != "reference-only-unlabeled":
        raise ValueError("rune catalog must be reference-only-unlabeled")
    return value, hashlib.sha256(raw).hexdigest()


def _fingerprint(strokes):
    validated = _validate_strokes(strokes)
    if validated is not None:
        raise ValueError(validated)
    points_by_stroke = [[(float(x), float(y)) for x, y in stroke] for stroke in strokes]
    all_points = [point for stroke in points_by_stroke for point in stroke]
    scale = max(
        max(x for x, y in all_points) - min(x for x, y in all_points),
        max(y for x, y in all_points) - min(y for x, y in all_points),
        1.0,
    )
    quant = lambda value: round(value / scale / 0.05) * 0.05
    descriptors = []
    for index, points in enumerate(points_by_stroke):
        distances = [math.hypot(b[0] - a[0], b[1] - a[1]) for point_index, a in enumerate(points) for b in points[point_index + 1:]]
        cross = [math.hypot(b[0] - a[0], b[1] - a[1]) for other in points_by_stroke[index + 1:] for a in points for b in other]
        turns = [abs(math.atan2((b[0] - a[0]) * (c[1] - b[1]) - (b[1] - a[1]) * (c[0] - a[0]), (b[0] - a[0]) * (c[0] - a[0]) + (b[1] - a[1]) * (c[1] - a[1]))) for a, b, c in zip(points, points[1:], points[2:])]
        descriptors.append((len(points), tuple(quant(distance) for distance in distances), tuple(quant(distance) for distance in cross), tuple(quant(turn) for turn in turns), quant(math.hypot(points[-1][0] - points[0][0], points[-1][1] - points[0][1]))))
    return hashlib.sha256(json.dumps(tuple(descriptors), separators=(",", ":")).encode()).hexdigest()


def _templates(catalog):
    deficiencies = []
    result = {}
    glyphs = catalog.get("glyphs")
    if not isinstance(glyphs, dict):
        raise ValueError("catalog must contain glyphs object")
    for rune_id, entry in glyphs.items():
        templates = entry.get("templates") if isinstance(entry, dict) else None
        if not isinstance(templates, list):
            templates = []
        ids = [t.get("id") if isinstance(t, dict) else None for t in templates]
        sources = [t.get("independent_source") if isinstance(t, dict) else None for t in templates]
        valid = []
        geometry_issues = []
        for template in templates:
            if not isinstance(template, dict) or not isinstance(template.get("id"), str) or not template["id"].strip():
                continue
            error = _validate_strokes(template.get("strokes"))
            if error is not None:
                geometry_issues.append(error)
                continue
            valid.append(template)
        if geometry_issues:
            deficiencies.append(f"{rune_id}: invalid geometry: " + ", ".join(geometry_issues))
        if not valid:
            deficiencies.append(f"{rune_id}: requires at least one valid template")
        if any(not isinstance(i, str) or not i.strip() for i in ids):
            deficiencies.append(f"{rune_id}: template IDs must be non-blank")
        if len(set(i for i in ids if isinstance(i, str))) != len(ids):
            deficiencies.append(f"{rune_id}: template IDs must be unique")
        if any(not isinstance(s, str) or not s.strip() for s in sources):
            deficiencies.append(f"{rune_id}: independent_source must be non-blank")
        if len(set(s for s in sources if isinstance(s, str))) != len(sources):
            deficiencies.append(f"{rune_id}: independent_source values must be unique")
        result[rune_id] = valid
    if deficiencies:
        raise ValueError("invalid catalog prerequisites:\n" + "\n".join(deficiencies))
    return result


def _points(strokes, seed, variant):
    rng = random.Random(seed * 1009 + variant * 9176)
    tx, ty = rng.uniform(-4, 4), rng.uniform(-4, 4)
    scale = 1 + rng.uniform(-0.035, 0.035)
    angle = rng.uniform(-math.radians(8), math.radians(8))
    ca, sa = math.cos(angle), math.sin(angle)
    result = []
    for stroke in strokes:
        transformed = []
        for x, y in stroke:
            xx, yy = (x - 64) * scale, (y - 64) * scale
            transformed.append([
                round(min(127.5, max(0.5, xx * ca - yy * sa + 64 + tx)), 4),
                round(min(127.5, max(0.5, xx * sa + yy * ca + 64 + ty)), 4),
            ])
        result.append(transformed)
    return result

def _record(example_id, label, strokes, lineage_group, seed_id, source,
            independent_source, author_group, stroke_inks, **metadata):
    if len(strokes) != len(stroke_inks):
        raise ValueError("stroke ink count must match stroke count")
    painted = []
    for stroke, ink in zip(strokes, stroke_inks):
        item = {
            "points": [{"x": x, "y": y} for x, y in stroke],
            "brush_width": 6.0,
            "started_at_millis": 0,
            "element": ink,
        }
        painted.append(item)
    generation = {
        "profile": PROFILE,
        "kind": "ink-variant" if "ink_kind" in metadata else (
            "seed-variant" if source == "synthetic" else "balanced-reject"
        ),
    }
    if "ink_kind" in metadata:
        generation["ink_kind"] = metadata["ink_kind"]
    record = {
        "schema_version": SCHEMA_VERSION,
        "example_id": example_id,
        "label": label,
        "source": source,
        "independent_source": independent_source,
        "lineage_group": lineage_group,
        "seed_id": seed_id,
        "author_group": author_group,
        "session_group": seed_id,
        "split_group": lineage_group,
        "consent": None,
        "strokes": painted,
        "generation": generation,
    }
    record.update({key: value for key, value in metadata.items() if key != "ink_kind"})
    return record


def _ink_variants(strokes, label, seed, variant, mix_count):
    """Return baseline and controlled mixed-ink assignments.

    `fire` is the canonical label and ink key. The schema also accepts the
    color alias `flame`, but this generator never treats those aliases as
    different colors.
    """
    rng = random.Random(seed * 7919 + variant * 6151)
    n = len(strokes)
    variants = [([label] * n, "single")]
    if n >= 2:
        other = [ink for ink in INK_NAMES if ink != label]
        for _ in range(mix_count):
            ink = rng.choice(other)
            slot = rng.randrange(n)
            stroke_inks = [label] * n
            stroke_inks[slot] = ink
            variants.append((stroke_inks, "mixed"))
    return variants


def generate_corpus(
    catalog_path: Path,
    output_dir: Path,
    *,
    seed_variants=3,
    derivatives_per_label=40,
    mix_count=2,
    reject_count=None,
):
    catalog, geometry_hash = _catalog(catalog_path)
    templates = _templates(catalog)
    output_dir.mkdir(parents=True, exist_ok=True)
    reject_count = reject_count if reject_count is not None else len(ELEMENT_LABELS) * derivatives_per_label
    records = []

    for label in ELEMENT_LABELS:
        for rune_id, rune_templates in templates.items():
            for index, template in enumerate(rune_templates):
                lineage = f"rune:{rune_id}:{template['id']}"
                seed = f"rune:{rune_id}:{index}"
                provenance = template["independent_source"]
                author_group = f"synthetic-author:{rune_id}:{index}"

                for variant in range(seed_variants):
                    strokes = _points(template["strokes"], 17 + index, variant)
                    stroke_inks = [label] * len(strokes)
                    records.append(_record(
                        f"{label}:{rune_id}:seed:{index}:{variant}",
                        label,
                        strokes,
                        lineage,
                        seed,
                        "synthetic",
                        provenance,
                        author_group,
                        stroke_inks,
                        ink_kind="single",
                    ))

                for derivative in range(derivatives_per_label):
                    variant_strokes = _points(
                        template["strokes"],
                        101 + index * max(1, derivatives_per_label) + derivative,
                        derivative + seed_variants + 1,
                    )
                    for mix_index, (stroke_inks, kind) in enumerate(
                        _ink_variants(template["strokes"], label, 101 + derivative, derivative, mix_count)
                    ):
                        records.append(_record(
                            f"{label}:{rune_id}:derivative:{index}:{derivative}:{kind}:{mix_index}",
                            label,
                            variant_strokes,
                            lineage,
                            seed,
                            "synthetic",
                            provenance,
                            author_group,
                            stroke_inks,
                            ink_kind=kind,
                        ))

    for index in range(reject_count):
        rng = random.Random(701 + index)
        scribble = [[
            [20 + rng.uniform(-5, 5), 20 + rng.uniform(-5, 5)],
            [100 + rng.uniform(-5, 5), 100 + rng.uniform(-5, 5)],
            [30 + rng.uniform(-5, 5), 90 + rng.uniform(-5, 5)],
            [90 + rng.uniform(-5, 5), 30 + rng.uniform(-5, 5)],
        ]]
        strokes = _points(scribble, 701 + index, index)
        stroke_inks = [rng.choice(INK_NAMES) for _ in strokes]
        records.append(_record(
            f"reject:seed:{index}",
            "reject",
            strokes,
            "reject:scribble",
            f"reject:{index}",
            "reject",
            "rune-author:reject:0",
            "reject-author:scribble",
            stroke_inks,
            reject_family="scribbles",
        ))

    jsonl = output_dir / "corpus.jsonl"
    jsonl.write_text("".join(json.dumps(r, sort_keys=True, separators=(",", ":")) + "\n" for r in records))
    corpus_hash = hashlib.sha256(jsonl.read_bytes()).hexdigest()
    counts = Counter(r["label"] for r in records)
    groups = {label: sorted({r["split_group"] for r in records if r["label"] == label}) for label in LABELS}
    lineages = {label: sorted({r["lineage_group"] for r in records if r["label"] == label}) for label in LABELS}
    authors = {label: sorted({r["author_group"] for r in records if r["label"] == label}) for label in LABELS}
    manifest = {
        "profile": PROFILE,
        "source": "synthetic",
        "release_ready": False,
        "catalog_version": catalog.get("catalog_version"),
        "geometry_sha256": geometry_hash,
        "corpus_sha256": corpus_hash,
        "record_count": len(records),
        "seed_variants_per_label": seed_variants,
        "derivatives_per_seed": derivatives_per_label,
        "mix_count": mix_count,
        "reject_count": reject_count,
        "counts": dict(counts),
        "groups": groups,
        "lineages": lineages,
        "lineage_counts": {label: len(lineages[label]) for label in LABELS},
        "author_groups": authors,
        "provenance": _provenance_map(records),
    }
    (output_dir / "manifest.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n")
    return manifest


def _provenance_map(records):
    result = {}
    for record in records:
        label_map = result.setdefault(record["label"], {})
        lineage = record["lineage_group"]
        source = record["independent_source"]
        prior = label_map.get(lineage)
        if prior is not None and prior != hashlib.sha256(source.encode()).hexdigest():
            raise ValueError(f"{PROVENANCE_CONFLICT}: {record['label']}:{lineage}")
        label_map[lineage] = hashlib.sha256(source.encode()).hexdigest()
    return {label: dict(sorted(values.items())) for label, values in result.items()}


def validate_development_corpus(path: Path, manifest_path: Path | None = None):
    try:
        examples = load_examples(path)
    except ValueError as exc:
        return [str(exc)]
    errors = []
    if manifest_path is not None:
        try:
            manifest = json.loads(manifest_path.read_text())
        except (OSError, json.JSONDecodeError) as exc:
            return [f"invalid manifest: {exc}"]
        if manifest.get("profile") != PROFILE or manifest.get("source") != "synthetic" or manifest.get("release_ready") is not False:
            errors.append("manifest is not development-only")
        if manifest.get("record_count") != len(examples):
            errors.append("manifest record_count mismatch")
        if manifest.get("corpus_sha256") != hashlib.sha256(path.read_bytes()).hexdigest():
            errors.append("manifest corpus_sha256 mismatch")
        if manifest.get("counts") != dict(Counter(e.label for e in examples)):
            errors.append("manifest counts mismatch")
        groups = {l: sorted({e.split_group for e in examples if e.label == l}) for l in LABELS}
        lineages = {l: sorted({e.lineage_group for e in examples if e.label == l}) for l in LABELS}
        authors = {l: sorted({e.author_group for e in examples if e.label == l}) for l in LABELS}
        if manifest.get("groups") != groups:
            errors.append("manifest groups mismatch")
        if manifest.get("lineages") != lineages:
            errors.append("manifest lineages mismatch")
        if manifest.get("lineage_counts") != {l: len(lineages[l]) for l in LABELS}:
            errors.append("manifest lineage_counts mismatch")
        if manifest.get("author_groups") != authors:
            errors.append("manifest author_groups mismatch")
        try:
            expected_provenance = _provenance_map([{"label": e.label, "lineage_group": e.lineage_group, "independent_source": e.independent_source} for e in examples])
        except ValueError as exc:
            errors.append(str(exc))
            expected_provenance = {}
        if manifest.get("provenance") != expected_provenance:
            errors.append("manifest provenance mismatch")
    rune_ids = set()
    for e in examples:
        if not isinstance(e.independent_source, str) or not e.independent_source.strip():
            errors.append(f"{e.example_id}: missing independent_source")
        if e.source not in {"synthetic", "reject"}:
            errors.append(f"{e.example_id}: non-development source")
        if e.generation is None or e.generation.get("profile") != PROFILE:
            errors.append(f"{e.example_id}: missing development profile")
        if e.label in ELEMENT_LABELS:
            if not e.strokes:
                errors.append(f"{e.example_id}: element example has no strokes")
                continue
            ink_values = [stroke.element for stroke in e.strokes]
            if any(ink is None for ink in ink_values):
                errors.append(f"{e.example_id}: every stroke must record its ink")
            canonical = ["fire" if ink == "flame" else ink for ink in ink_values]
            kind = e.generation.get("ink_kind") if e.generation is not None else None
            if kind == "single" and any(ink != e.label for ink in canonical):
                errors.append(f"{e.example_id}: single-ink record does not match label")
            if kind == "mixed":
                if not any(ink == e.label for ink in canonical):
                    errors.append(f"{e.example_id}: mixed record has no primary ink")
                if not any(ink != e.label for ink in canonical):
                    errors.append(f"{e.example_id}: mixed record has no distinct secondary ink")
            for ink in canonical:
                if ink not in ELEMENT_LABELS:
                    errors.append(f"{e.example_id}: unsupported canonical ink {ink}")
        elif e.label != "reject":
            errors.append(f"{e.example_id}: unexpected experiment label")
        if e.independent_source.startswith("rune-author:"):
            rune_ids.add(e.independent_source)
    if len(rune_ids) < 6:
        errors.append(f"corpus must contain six rune sources, found {len(rune_ids)}")
    return errors


def main(argv=None):
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
