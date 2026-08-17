from __future__ import annotations

import hashlib
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).parent
CORPUS = ROOT / "dataset/generated/dev-basic-v1/corpus.jsonl"
MANIFEST = ROOT / "dataset/generated/dev-basic-v1/manifest.json"


def migrate() -> None:
    migrated = []
    with CORPUS.open() as handle:
        for line in handle:
            rec = json.loads(line)
            if "lineage_group" not in rec:
                rec["lineage_group"] = rec["split_group"]
            if "independent_source" not in rec:
                rec["independent_source"] = rec["seed_id"]
            migrated.append(rec)

    CORPUS.write_text(
        "".join(
            json.dumps(r, sort_keys=True, separators=(",", ":")) + "\n"
            for r in migrated
        )
    )
    corpus_sha = hashlib.sha256(CORPUS.read_bytes()).hexdigest()

    from wizardry_glyphs.schema import load_examples

    examples = load_examples(CORPUS)
    print(f"loaded {len(examples)} examples")

    mf = json.loads(MANIFEST.read_text())
    mf["corpus_sha256"] = corpus_sha
    mf["record_count"] = len(examples)
    mf["counts"] = dict(Counter(e.label for e in examples))
    groups = {
        label: sorted({e.split_group for e in examples if e.label == label})
        for label in mf["counts"]
    }
    mf["groups"] = groups
    lineages = {
        label: sorted({e.lineage_group for e in examples if e.label == label})
        for label in mf["counts"]
    }
    mf["lineages"] = lineages
    mf["lineage_counts"] = {label: len(value) for label, value in lineages.items()}
    mf["author_groups"] = {
        label: sorted({e.author_group for e in examples if e.label == label})
        for label in mf["counts"]
    }

    provenance: dict[str, dict[str, str]] = {}
    for example in examples:
        prior = provenance.setdefault(example.label, {}).get(example.lineage_group)
        hashed = hashlib.sha256(example.independent_source.encode()).hexdigest()
        if prior is not None and prior != hashed:
            raise ValueError(f"independent_source conflict within lineage: {example.label}:{example.lineage_group}")
        provenance[example.label][example.lineage_group] = hashed
    mf["provenance"] = {label: dict(sorted(value.items())) for label, value in provenance.items()}

    MANIFEST.write_text(json.dumps(mf, indent=2, sort_keys=True) + "\n")

    from wizardry_glyphs.dev_corpus import validate_development_corpus

    errors = validate_development_corpus(CORPUS, MANIFEST)
    print("validation errors:", errors if errors else "none")
    if errors:
        raise SystemExit(2)


if __name__ == "__main__":
    migrate()
