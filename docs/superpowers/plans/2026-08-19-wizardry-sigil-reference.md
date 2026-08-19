# Wizardry Sigil Reference Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the synthetic starter geometry with an original, reviewed 23-positive-label sigil reference set and make production partitioning writer-, source-, and lineage-safe.

**Architecture:** Keep explicit canonical polylines in `catalog-geometry-v1.json`, with six independently authored templates per positive label and no drawable `reject` template. Reuse the existing `author_group` field as the pseudonymous writer identifier, set player `split_group` at writer scope, and strengthen grouped splitting so every writer, independent source, and lineage is assigned to exactly one partition.

**Tech Stack:** Python 3.12, JSON/JSONL, NumPy, PyTorch training tooling, pytest, HTML glyph studio.

## Global Constraints

- All 23 positive labels require explicit geometry; `reject` remains a heterogeneous negative class.
- Each canonical symbol uses one to four logical strokes.
- Each positive label requires at least six structurally distinct template lineages; transformed copies do not count.
- No pair may differ only by rotation, reflection, stroke order, color, or a tiny mark.
- Historic sources guide visual grammar only; final geometry is independently authored and includes source/license notes.
- Player records use pseudonymous `author_group`; no real-world identity enters the corpus.
- Training, calibration, and sealed test partitions must be disjoint by `author_group`, `independent_source`, and `lineage_group`.
- Synthetic development artifacts remain `release_ready: false`.

---

### Task 1: Author and Validate the 23-Label Geometry Catalog

**Files:**
- Modify: `training/catalog-geometry-v1.json`
- Modify: `training/src/wizardry_glyphs/dev_corpus.py`
- Test: `training/tests/test_dev_corpus.py`

**Interfaces:**
- Consumes: `dev_corpus._templates(catalog: dict) -> dict[str, list[dict]]` and the canonical concepts in `docs/superpowers/specs/2026-08-19-wizardry-sigil-reference-design.md`.
- Produces: catalog entries with `intent`, `ambiguity_risks`, `reference_sources`, and six or more templates containing `id`, `independent_source`, and `strokes`; `_templates` rejects excess stroke count and missing license provenance.

- [ ] **Step 1: Add failing catalog contract tests**

Add tests that load the real geometry catalog and enforce the approved vocabulary:

```python
def test_reference_catalog_covers_positive_labels_without_drawable_reject():
    catalog = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    assert set(catalog["glyphs"]) == set(POSITIVE_LABELS) | {"reject"}
    assert catalog["glyphs"]["reject"]["templates"] == []
    for label in POSITIVE_LABELS:
        entry = catalog["glyphs"][label]
        assert len(entry["templates"]) >= 6
        assert entry["reference_sources"]
        assert all(source["url"] and source["license"] for source in entry["reference_sources"])
        assert all(1 <= len(template["strokes"]) <= 4 for template in entry["templates"])


def test_reference_templates_are_not_transformed_duplicate_fingerprints():
    catalog = json.loads((ROOT / "catalog-geometry-v1.json").read_text())
    templates = _templates(catalog)
    for label, values in templates.items():
        assert len({_fingerprint(value["strokes"]) for value in values}) >= MIN_TEMPLATES
```

- [ ] **Step 2: Run the focused tests and confirm they fail**

Run: `cd training && .venv/bin/python -m pytest tests/test_dev_corpus.py -q`

Expected: FAIL because current entries lack `reference_sources`, current topology does not follow the approved concepts, or `reject` still has canonical templates.

- [ ] **Step 3: Strengthen `_templates` validation**

Require one to four strokes and provenance for positive entries while permitting no canonical `reject` templates:

```python
if label == "reject":
    if templates:
        issues.append("reject must not define a canonical drawable template")
    result[label] = []
    continue
sources_meta = entry.get("reference_sources")
if not isinstance(sources_meta, list) or not sources_meta:
    issues.append("reference_sources must be a nonempty list")
elif any(not isinstance(item, dict) or not item.get("url") or not item.get("license") for item in sources_meta):
    issues.append("each reference source requires url and license")
if any(not 1 <= len(template.get("strokes", [])) <= 4 for template in valid):
    issues.append("templates require one to four logical strokes")
```

Adjust reject corpus generation to synthesize heterogeneous negatives from independent reject-generation recipes rather than iterating canonical reject templates. Each recipe must receive a stable reject lineage and `independent_source`.

- [ ] **Step 4: Replace the positive catalog geometry**

For every positive label, encode six independently authored variants of the specification concept. Keep variant topology stable while varying control points and proportions deliberately; do not produce variants with global transforms. Add this metadata shape to each entry:

```json
"reference_sources": [
  {
    "url": "https://commons.wikimedia.org/wiki/File:Alchemy_symbols.jpg",
    "license": "Public domain mark 1.0",
    "use": "visual grammar only; no traced geometry"
  },
  {
    "url": "https://commons.wikimedia.org/wiki/File:Medeltida_runor.svg",
    "license": "Public domain dedication",
    "use": "stroke economy only; no copied rune assignment"
  }
]
```

For `reject`, use:

```json
"reject": {
  "intent": "heterogeneous non-glyph input",
  "ambiguity_risks": [],
  "reference_sources": [],
  "templates": []
}
```

- [ ] **Step 5: Run catalog and corpus tests**

Run: `cd training && .venv/bin/python -m pytest tests/test_dev_corpus.py tests/test_catalog_expansion.py -q`

Expected: PASS.

- [ ] **Step 6: Generate and validate the development corpus**

Run:

```bash
cd training
rm -rf /tmp/wizardry-sigil-corpus
.venv/bin/python -m wizardry_glyphs.dev_corpus catalog-geometry-v1.json /tmp/wizardry-sigil-corpus
```

Expected: exit 0; JSON output contains `"valid": true`, all 24 label counts, at least six lineages for each positive label, and generated manifest `release_ready: false`.

- [ ] **Step 7: Commit the geometry unit**

```bash
git add training/catalog-geometry-v1.json training/src/wizardry_glyphs/dev_corpus.py training/tests/test_dev_corpus.py
git commit -m "feat: define original Wizardry sigil references"
```

---

### Task 2: Enforce Writer-Level Dataset Provenance

**Files:**
- Modify: `training/dataset/schema-v1.json`
- Modify: `training/src/wizardry_glyphs/schema.py`
- Modify: `training/src/wizardry_glyphs/validate_dataset.py`
- Test: `training/tests/test_schema.py`
- Test: `training/tests/test_validate_dataset.py`

**Interfaces:**
- Consumes: existing `GlyphExample.author_group`, `session_group`, `split_group`, `lineage_group`, and `independent_source` fields.
- Produces: validation rule that player and player-authored reject records have `split_group == author_group`; canonical/synthetic compatibility is retained.

- [ ] **Step 1: Add failing player provenance tests**

```python
def test_player_split_group_must_match_pseudonymous_author(tmp_path):
    value = record(
        source="player",
        author_group="writer-17",
        split_group="session-3",
        consent=True,
    )
    with pytest.raises(ValueError, match="player split_group must match author_group"):
        load_examples(write_jsonl(tmp_path, value))


def test_player_writer_group_is_accepted_without_real_identity(tmp_path):
    value = record(
        source="player",
        author_group="writer-17",
        split_group="writer-17",
        session_group="session-3",
        consent=True,
    )
    example = load_examples(write_jsonl(tmp_path, value))[0]
    assert example.author_group == example.split_group == "writer-17"
```

Add a dataset-gate test where two player rows share `author_group` but use different `split_group`; assert validation reports the same provenance violation.

- [ ] **Step 2: Run tests and confirm they fail**

Run: `cd training && .venv/bin/python -m pytest tests/test_schema.py tests/test_validate_dataset.py -q`

Expected: FAIL because the current schema accepts session-level player splits.

- [ ] **Step 3: Add the semantic schema invariant**

In `GlyphExample.__post_init__`, add:

```python
if self.source == "player" and self.split_group != self.author_group:
    raise ValueError("player split_group must match author_group")
```

Keep JSON Schema structural rather than encoding cross-field equality. Add a `$comment` documenting that semantic equality is enforced by `schema.py`:

```json
"$comment": "For source=player, runtime validation requires split_group == author_group."
```

Ensure `validate_dataset` surfaces the loader error unchanged so bad private corpora fail before model construction.

- [ ] **Step 4: Run provenance tests**

Run: `cd training && .venv/bin/python -m pytest tests/test_schema.py tests/test_validate_dataset.py -q`

Expected: PASS.

- [ ] **Step 5: Commit the provenance unit**

```bash
git add training/dataset/schema-v1.json training/src/wizardry_glyphs/schema.py training/src/wizardry_glyphs/validate_dataset.py training/tests/test_schema.py training/tests/test_validate_dataset.py
git commit -m "fix: keep player records grouped by writer"
```

---

### Task 3: Make Cross-Validation Writer-, Source-, and Lineage-Safe

**Files:**
- Modify: `training/src/wizardry_glyphs/split.py`
- Modify: `training/src/wizardry_glyphs/train.py`
- Test: `training/tests/test_grouped_cross_validation.py`
- Test: `training/tests/test_training_protocol.py`

**Interfaces:**
- Consumes: row dictionaries containing `label`, `lineage_group`, `author_group`, `independent_source`, and `split_group`.
- Produces: `grouped_cross_validation_split(rows, folds=5, test_ratio=.15, seed=0) -> {"test": list, "folds": list[list]}` whose connected provenance components never cross partitions; `validate_partition_isolation(partitions)` validates all three identities.

- [ ] **Step 1: Add failing multi-label writer and source tests**

```python
def test_cross_validation_keeps_multi_label_writer_in_one_partition():
    rows = make_provenance_rows()
    result = grouped_cross_validation_split(rows, folds=2, seed=7)
    partitions = [result["test"], *result["folds"]]
    writer_locations = {}
    for index, partition in enumerate(partitions):
        for row in partition:
            writer_locations.setdefault(row["author_group"], set()).add(index)
    assert all(len(locations) == 1 for locations in writer_locations.values())


def test_isolation_rejects_independent_source_overlap():
    with pytest.raises(ValueError, match="independent_source overlap"):
        validate_partition_isolation([
            [{"label": "a", "lineage_group": "a-1", "author_group": "w-1", "independent_source": "source-x"}],
            [{"label": "a", "lineage_group": "a-2", "author_group": "w-2", "independent_source": "source-x"}],
        ])
```

`make_provenance_rows()` must provide at least three writer components containing every label so test and both folds can each contain the complete label set.

- [ ] **Step 2: Run focused tests and confirm they fail**

Run: `cd training && .venv/bin/python -m pytest tests/test_grouped_cross_validation.py tests/test_training_protocol.py -q`

Expected: FAIL because current grouping only uses `lineage_group`.

- [ ] **Step 3: Build connected provenance components**

Add a union-find helper that joins any rows sharing writer, source, or lineage:

```python
def _provenance_components(rows):
    keys = ("author_group", "independent_source", "lineage_group")
    parent = list(range(len(rows)))

    def find(index):
        while parent[index] != index:
            parent[index] = parent[parent[index]]
            index = parent[index]
        return index

    def union(left, right):
        left, right = find(left), find(right)
        if left != right:
            parent[right] = left

    seen = {key: {} for key in keys}
    for index, row in enumerate(rows):
        for key in keys:
            value = row.get(key)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"row requires nonempty {key}")
            prior = seen[key].setdefault(value, index)
            union(index, prior)
    components = defaultdict(list)
    for index, row in enumerate(rows):
        components[find(index)].append(row)
    return list(components.values())
```

Assign whole components to test/folds while balancing per-label row counts. Before assignment, require at least `folds + 1` components containing each label. A component may contain multiple labels because one writer should contribute several glyph classes.

- [ ] **Step 4: Extend isolation validation**

For each of `lineage_group`, `author_group`, and `independent_source`, track the first partition index and reject reuse:

```python
for key in ("lineage_group", "author_group", "independent_source"):
    value = row.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"row requires nonempty {key}")
    prior = identity_partition[key].setdefault(value, partition_index)
    if prior != partition_index:
        raise ValueError(f"{key} overlap: {value!r}")
```

Preserve the existing complete-label check and mixed-label lineage rejection.

- [ ] **Step 5: Carry provenance into training rows**

Update `_rows` and `_augment_training_rows` in `train.py` to copy `author_group`, `session_group`, and `split_group` explicitly. Generated augmentation must inherit, not synthesize, those fields.

- [ ] **Step 6: Run split and protocol tests**

Run: `cd training && .venv/bin/python -m pytest tests/test_grouped_cross_validation.py tests/test_training_protocol.py tests/test_preprocess_augment.py -q`

Expected: PASS.

- [ ] **Step 7: Commit the partitioning unit**

```bash
git add training/src/wizardry_glyphs/split.py training/src/wizardry_glyphs/train.py training/tests/test_grouped_cross_validation.py training/tests/test_training_protocol.py training/tests/test_preprocess_augment.py
git commit -m "fix: isolate glyph evaluation by writer and source"
```

---

### Task 4: Add Collection and Collision Review Output

**Files:**
- Modify: `training/src/wizardry_glyphs/studio.html`
- Modify: `training/src/wizardry_glyphs/studio.py`
- Test: `training/tests/test_studio.py`
- Create: `training/review-collision-groups-v1.json`

**Interfaces:**
- Consumes: canonical templates from `catalog-geometry-v1.json` and existing studio export behavior.
- Produces: player JSONL records with pseudonymous writer/session provenance and a machine-readable collision review manifest covering all five specified groups.

- [ ] **Step 1: Add failing studio export tests**

Add an export test asserting that a player record created for writer `writer-17` and session `session-3` contains:

```python
assert record["source"] == "player"
assert record["author_group"] == "writer-17"
assert record["split_group"] == "writer-17"
assert record["session_group"] == "session-3"
assert record["lineage_group"] == "redraw:writer-17:fire:fire-template-0"
assert record["seed_id"] == "fire-template-0"
assert record["independent_source"] == "writer:writer-17"
assert record["consent"] is True
```

Also assert the studio refuses export when writer ID is empty or consent is not affirmed.

- [ ] **Step 2: Run studio tests and confirm they fail**

Run: `cd training && .venv/bin/python -m pytest tests/test_studio.py -q`

Expected: FAIL because current export does not enforce the complete writer-level record.

- [ ] **Step 3: Add explicit collection fields and export construction**

Add required controls for pseudonymous writer ID, session ID, canonical seed, and consent. Construct the exported record using this function contract in `studio.py`:

```python
def player_record(*, label, strokes, writer_id, session_id, seed_id, consent):
    if not writer_id.strip():
        raise ValueError("writer ID is required")
    if not session_id.strip():
        raise ValueError("session ID is required")
    if consent is not True:
        raise ValueError("player consent is required")
    return {
        "schema_version": "glyph-dataset-v1",
        "source": "player",
        "independent_source": f"writer:{writer_id}",
        "lineage_group": f"redraw:{writer_id}:{label}:{seed_id}",
        "seed_id": seed_id,
        "author_group": writer_id,
        "session_group": session_id,
        "split_group": writer_id,
        "consent": True,
        "strokes": strokes,
    }
```

Retain the studio's existing unique `example_id` creation and element-ink capture.

- [ ] **Step 4: Create the collision review manifest**

Write `training/review-collision-groups-v1.json` with these exact groups:

```json
{
  "version": "glyph-collision-review-v1",
  "render_size": [64, 64],
  "mode": "monochrome",
  "groups": [
    ["fire", "physical", "frost"],
    ["push", "target-ray", "attacker"],
    ["cooldown", "periodic", "repeat"],
    ["self", "target", "area"],
    ["damage", "on-hit", "on-hurt"]
  ]
}
```

Add a test that loads the file, asserts each named group, and verifies every label exists in the catalog.

- [ ] **Step 5: Run studio tests**

Run: `cd training && .venv/bin/python -m pytest tests/test_studio.py -q`

Expected: PASS.

- [ ] **Step 6: Manually smoke-test the collection surface**

Run: `cd training && .venv/bin/python -m wizardry_glyphs.studio`

Exercise: enter a pseudonymous writer ID, choose one canonical seed, draw a glyph, consent, and export it. Verify the resulting JSONL reloads with:

```bash
cd training
.venv/bin/python -c "from wizardry_glyphs.schema import load_examples; print(load_examples('PATH_TO_EXPORT')[0])"
```

Expected: one valid `GlyphExample`; `author_group == split_group`; seed and lineage preserved.

- [ ] **Step 7: Commit the collection unit**

```bash
git add training/src/wizardry_glyphs/studio.html training/src/wizardry_glyphs/studio.py training/tests/test_studio.py training/review-collision-groups-v1.json
git commit -m "feat: capture writer-safe glyph redraws"
```

---

### Task 5: Verify the Full Training Protocol

**Files:**
- Modify only if verification identifies a defect in files from Tasks 1–4.

**Interfaces:**
- Consumes: final geometry catalog, generated corpus, provenance-aware schema, provenance-safe splitter, and collection output.
- Produces: direct evidence that development training exports a valid non-release bundle and all tests pass.

- [ ] **Step 1: Run the complete Python suite**

Run: `cd training && .venv/bin/python -m pytest -q`

Expected: all tests PASS; ONNX exporter warnings may remain but no failures.

- [ ] **Step 2: Regenerate the checked development corpus**

Run:

```bash
cd training
rm -rf dataset/generated/dev-basic-v1
.venv/bin/python -m wizardry_glyphs.dev_corpus catalog-geometry-v1.json dataset/generated/dev-basic-v1
```

Expected: exit 0 and `"valid": true`.

- [ ] **Step 3: Run a one-epoch CUDA end-to-end smoke training**

Create `/tmp/wizardry-sigil-smoke.json` from `train-explore.json` with `epochs: 1`, `folds: 2`, `batch_size: 256`, and `output: /tmp/wizardry-sigil-smoke`, then run:

```bash
cd training
.venv/bin/python -m wizardry_glyphs.train --config /tmp/wizardry-sigil-smoke.json
```

Expected: exit 3 because the profile is synthetic development; output contains `model.onnx`, `model.pt`, `manifest.json`, `history.json`, and `sha256sums.json`; manifest records CUDA/RTX 3080 and `release_ready: false`.

- [ ] **Step 4: Validate exported ONNX and manifest metadata**

Run:

```bash
cd training
.venv/bin/python -c "import json,onnx; from pathlib import Path; p=Path('/tmp/wizardry-sigil-smoke'); m=json.loads((p/'manifest.json').read_text()); onnx.checker.check_model(onnx.load(p/'model.onnx')); assert m['release_ready'] is False; assert m['profile']=='synthetic-development'; print(m['metrics'])"
```

Expected: exit 0 and printed sealed synthetic metrics.

- [ ] **Step 5: Review repository state and commit any verification-only correction separately**

Run: `git status --short && git diff --check`

Expected: only intentional user work remains. If verification required a correction, stage only that behavior and its tests, then commit with a message describing that single correction; do not fold it into an unrelated prior commit.
