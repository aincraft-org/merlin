# Glyph Stratified Group Cross-Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement lineage-isolated five-fold model selection with a sealed final test set and independently generated synthetic lineages.

**Architecture:** `schema.py` owns explicit lineage identity, `split.py` performs deterministic sealed-test and fold assignment, and a focused `cross_validate.py` module runs candidate folds and aggregates validation metrics without receiving sealed-test rows. `train.py` orchestrates final calibration, retraining, single test evaluation, and export. The development corpus generator supplies at least six genuinely distinct templates per label.

**Tech Stack:** Python 3.12, PyTorch, NumPy, pytest, ONNX, ONNX Runtime, JSON/JSONL.

## Global Constraints

- Five cross-validation folds and at least six independent lineages per label.
- `lineage_group` is mandatory; cross-validation never falls back to sample, seed, or split IDs.
- Every derivative and augmentation retains its parent lineage.
- Candidate selection cannot receive or evaluate sealed-test rows.
- Augmentation occurs only after each training partition is formed.
- Synthetic scores never claim real-player generalization; perfect scores emit benchmark-saturation warnings.
- Fail corpus feasibility checks before constructing any model.

---

### Task 1: Explicit lineage schema and propagation

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/schema.py`
- Modify: `glyph-training/src/wizardry_glyphs/augment.py`
- Modify: `glyph-training/tests/test_schema.py`
- Modify: `glyph-training/tests/test_preprocess_augment.py`

**Interfaces:**
- Produces: `GlyphExample.lineage_group: str` and required JSON field `lineage_group`.
- Consumes: Existing immutable `GlyphExample` construction and `augment_example(example, seed)`.

- [ ] **Step 1: Add failing schema tests**

Add tests asserting that loading a record without `lineage_group` raises `ValueError("missing required field lineage_group")`, that a valid value is exposed as `example.lineage_group`, and that empty lineage values are rejected.

- [ ] **Step 2: Add failing augmentation test**

Construct a `GlyphExample` with `lineage_group="template:cast:0"`, augment it, and assert the output retains that exact value while receiving a distinct example and seed identity.

- [ ] **Step 3: Run focused tests and confirm failure**

Run: `uv run pytest tests/test_schema.py tests/test_preprocess_augment.py -q`
Expected: failures because `GlyphExample` does not yet expose or require `lineage_group`.

- [ ] **Step 4: Implement the schema field**

Add `lineage_group: str` to `GlyphExample`, validate it with the existing nonempty text validator, add it to the required JSON fields, and pass `raw["lineage_group"]` through `_parse_example`. Update all direct test constructors in the same files with explicit lineage values.

- [ ] **Step 5: Preserve lineage during augmentation**

Pass `example.lineage_group` unchanged to immutable reconstruction and never mutate it in mutable-record augmentation.

- [ ] **Step 6: Run focused tests**

Run: `uv run pytest tests/test_schema.py tests/test_preprocess_augment.py -q`
Expected: all selected tests pass.

- [ ] **Step 7: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/schema.py glyph-training/src/wizardry_glyphs/augment.py glyph-training/tests/test_schema.py glyph-training/tests/test_preprocess_augment.py
git commit -m "feat: require glyph lineage identity"
```

### Task 2: Sealed-test and stratified group-fold assignment

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/split.py`
- Create: `glyph-training/tests/test_grouped_cross_validation.py`

**Interfaces:**
- Produces: `grouped_cross_validation_split(rows, *, folds: int = 5, test_ratio: float = 0.15, seed: int = 0) -> dict` returning `{"test": list, "folds": list[list]}`.
- Produces: `validate_partition_isolation(partitions) -> None`.
- Consumes: rows containing mandatory `label` and `lineage_group`.

- [ ] **Step 1: Add failing identity and feasibility tests**

Cover missing/empty lineage, mixed-label lineage, five-fold input with only five lineages for one label, invalid `folds < 2`, and invalid test ratios. Assert deficient errors name the label, required six lineages, and actual count.

- [ ] **Step 2: Add failing deterministic partition tests**

Build two labels with six unevenly sized lineage groups each. Assert exactly five folds, every fold contains both labels, one or more complete groups are sealed, no lineage crosses partitions, the same seed reproduces assignments, and another seed may differ while preserving invariants.

- [ ] **Step 3: Run splitter tests and confirm failure**

Run: `uv run pytest tests/test_grouped_cross_validation.py -q`
Expected: import failure for `grouped_cross_validation_split`.

- [ ] **Step 4: Implement strict grouping and feasibility validation**

Group only by `row["lineage_group"]`; reject missing/empty values and mixed labels. Validate fold count and ratio, calculate per-label required lineages as `folds + 1`, and raise before returning assignments when any label is deficient.

- [ ] **Step 5: Implement deterministic assignment**

For each label, shuffle lineages with a local seeded RNG, reserve the minimum complete groups approximating `test_ratio` while leaving at least `folds`, then greedily assign remaining groups to the fold with the lowest current per-label sample count. Return rows, never copied examples.

- [ ] **Step 6: Implement isolation validation**

Collect lineage sets from the sealed test and every fold and raise if any intersection is nonempty. Also verify every fold contains each corpus label.

- [ ] **Step 7: Run splitter tests**

Run: `uv run pytest tests/test_grouped_cross_validation.py -q`
Expected: all tests pass.

- [ ] **Step 8: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/split.py glyph-training/tests/test_grouped_cross_validation.py
git commit -m "feat: add sealed stratified group folds"
```

### Task 3: Independent synthetic template lineages

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/dev_corpus.py`
- Modify: `glyph-training/catalog-geometry-v1.json`
- Modify: `glyph-training/tests/test_validate_dataset.py`
- Modify: `glyph-training/tests/test_train_end_to_end.py`

**Interfaces:**
- Produces: generated records with `lineage_group="lineage:<label>:<template>"`.
- Produces: at least six independently specified base geometries per positive label and six balanced reject lineages.
- Consumes: explicit geometry definitions in `catalog-geometry-v1.json`.

- [ ] **Step 1: Add failing corpus lineage tests**

Assert every generated record contains `lineage_group`, each label has at least six unique lineages, all derivatives of a seed retain the base template lineage, and manifest lineage counts match the generated JSONL.

- [ ] **Step 2: Run corpus tests and confirm failure**

Run: `uv run pytest tests/test_validate_dataset.py tests/test_train_end_to_end.py -q`
Expected: failure because generated records lack explicit lineage identities and the corpus has only three groups per label.

- [ ] **Step 3: Add independent geometries**

Expand each label's catalog geometry entry to six independently authored templates. Preserve the glyph's semantic shape while varying topology, stroke ordering, proportions, and stroke count rather than applying coordinate jitter to an existing template.

- [ ] **Step 4: Generate lineage-aware records**

Update `_record` to require `lineage_group`; make a template and all its derivatives share that value. Give each reject base shape a distinct reject lineage. Record per-label lineage lists and counts in the corpus manifest.

- [ ] **Step 5: Regenerate and validate the development corpus**

Run the existing corpus-generation command used by `test_train_end_to_end.py`, then run:
`uv run pytest tests/test_validate_dataset.py tests/test_train_end_to_end.py -q`
Expected: all selected tests pass.

- [ ] **Step 6: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/dev_corpus.py glyph-training/catalog-geometry-v1.json glyph-training/tests/test_validate_dataset.py glyph-training/tests/test_train_end_to_end.py
git commit -m "feat: generate independent glyph lineages"
```

### Task 4: Cross-validation runner and aggregate metrics

**Files:**
- Create: `glyph-training/src/wizardry_glyphs/cross_validate.py`
- Create: `glyph-training/tests/test_cross_validate.py`
- Modify: `glyph-training/src/wizardry_glyphs/evaluate.py`

**Interfaces:**
- Produces: `aggregate_fold_metrics(metrics: list[dict]) -> dict` with `macro_f1_mean`, `macro_f1_stdev`, `macro_f1_min`, `folds`, and aggregated per-class fields.
- Produces: `rank_candidates(candidates: list[dict]) -> dict` ordered by mean descending, standard deviation ascending, parameters ascending, seconds ascending.
- Produces: `run_cross_validation(folds, candidates, train_fold, evaluate_fold, augment_fold) -> list[dict]`; it has no test-set parameter.

- [ ] **Step 1: Add failing aggregation and ranking tests**

Use known fold metrics to assert arithmetic mean, sample standard deviation via `statistics.stdev`, minimum, per-class means, and deterministic ranking tie-breaks.

- [ ] **Step 2: Add failing isolation-by-interface test**

Pass five sentinel folds and spies for training/evaluation. Assert each fold is validation exactly once, training contains the other four folds, augmentation sees only training rows, and no callback can receive a sealed-test collection because the runner signature has no test argument.

- [ ] **Step 3: Run tests and confirm failure**

Run: `uv run pytest tests/test_cross_validate.py -q`
Expected: import failure for the new module.

- [ ] **Step 4: Implement metric aggregation and ranking**

Use the standard library `statistics.mean` and `statistics.stdev`; retain raw fold metrics in the report. Aggregate per-class F1 by label, rejecting inconsistent label sets.

- [ ] **Step 5: Implement the fold runner**

For each candidate and fold index, concatenate the other folds as training rows, call `augment_fold` on training only, train with `train_fold`, and evaluate on the untouched validation fold. Measure candidate runtime around all five folds and attach parameter count from the trained fold result contract.

- [ ] **Step 6: Run focused tests**

Run: `uv run pytest tests/test_cross_validate.py -q`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/cross_validate.py glyph-training/src/wizardry_glyphs/evaluate.py glyph-training/tests/test_cross_validate.py
git commit -m "feat: evaluate glyph models across grouped folds"
```

### Task 5: Training orchestration and sealed final evaluation

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/train.py`
- Modify: `glyph-training/src/wizardry_glyphs/export.py`
- Modify: `glyph-training/train-dev-basic-v1.json`
- Modify: `glyph-training/tests/test_model_tooling.py`
- Modify: `glyph-training/tests/test_train_end_to_end.py`

**Interfaces:**
- Consumes: `grouped_cross_validation_split`, `run_cross_validation`, and `rank_candidates`.
- Produces: manifest fields `cross_validation`, `partition`, `selected_candidate`, and one `metrics` object for the sealed test.

- [ ] **Step 1: Add failing pre-training feasibility test**

Monkeypatch model constructors with spies and provide one label with fewer than six lineages. Assert the entrypoint returns validation failure and no constructor is called.

- [ ] **Step 2: Add failing sealed-test sequencing test**

Instrument candidate evaluation and final evaluation. Assert candidate selection completes before the sealed-test rows are passed to `_logits`, and assert sealed-test evaluation is called exactly once.

- [ ] **Step 3: Add failing manifest contract test**

Assert the exported manifest includes fold assignments/hashes, all five validation metrics, aggregate mean/stdev/minimum, selected configuration, calibration lineage count, sealed-test lineage count, and a synthetic-generalization warning when applicable.

- [ ] **Step 4: Run focused tests and confirm failure**

Run: `uv run pytest tests/test_model_tooling.py tests/test_train_end_to_end.py -q`
Expected: failures because training still uses one 70/15/15 split.

- [ ] **Step 5: Wire grouped cross-validation selection**

Replace `grouped_split` orchestration with strict grouped cross-validation. Construct candidate configurations from the configured candidate list, execute five folds for each candidate, rank aggregate results, and avoid retaining fold models after metrics are collected.

- [ ] **Step 6: Implement grouped calibration and final training**

After candidate selection, choose complete calibration lineages from non-test folds while preserving at least one training lineage per label, train the selected model on remaining non-test rows, fit calibration values, then evaluate sealed test rows once.

- [ ] **Step 7: Export protocol evidence and saturation warning**

Record assignment hashes rather than full private example IDs, aggregate CV metrics, calibration/test counts, selected candidate, and `benchmark_warning="perfect synthetic score indicates benchmark saturation, not generalization"` whenever synthetic CV or test macro F1 equals 1.0.

- [ ] **Step 8: Run focused tests**

Run: `uv run pytest tests/test_model_tooling.py tests/test_train_end_to_end.py -q`
Expected: all selected tests pass.

- [ ] **Step 9: Commit**

```bash
git add glyph-training/src/wizardry_glyphs/train.py glyph-training/src/wizardry_glyphs/export.py glyph-training/train-dev-basic-v1.json glyph-training/tests/test_model_tooling.py glyph-training/tests/test_train_end_to_end.py
git commit -m "feat: select glyph models with grouped cross-validation"
```

### Task 6: Reports, full verification, and CUDA smoke run

**Files:**
- Modify: `glyph-training/src/wizardry_glyphs/experiment_report.py`
- Modify: `glyph-training/tests/test_experiment_report.py`
- Generated: `glyph-training/artifacts/dev-basic-v1/`
- Generated: `glyph-training/artifacts/f1-experiments/`

**Interfaces:**
- Consumes: cross-validation reports and sealed-test manifest metrics.
- Produces: fold-score SVG, per-class fold-distribution SVG, JSON, and CSV reports clearly labeled synthetic-development.

- [ ] **Step 1: Add failing report tests**

Assert report JSON/CSV contains every fold score plus mean, sample standard deviation, and minimum. Assert SVG titles say `Synthetic grouped validation macro F1` and contain the benchmark warning when any score is perfect.

- [ ] **Step 2: Run report tests and confirm failure**

Run: `uv run pytest tests/test_experiment_report.py -q`
Expected: failures because current reports are single-split comparisons.

- [ ] **Step 3: Implement cross-validation reports**

Render one marker per fold, aggregate reference lines, and per-class fold distributions without adding a plotting dependency. Keep sealed-test metrics visually separate and label all synthetic outputs.

- [ ] **Step 4: Run complete test suite**

Run: `uv run pytest -q`
Expected: all tests pass with no failures.

- [ ] **Step 5: Run the real CUDA workflow**

Run: `CUDA_VISIBLE_DEVICES=0 uv run python -m wizardry_glyphs.train --config train-dev-basic-v1.json`
Expected: five folds execute per candidate on the RTX 3080, final bundle exports successfully, and the development-profile command returns the documented non-release status without a traceback.

- [ ] **Step 6: Verify artifacts behaviorally**

Load `model.pt` on CPU with `weights_only=True`, run `onnx.checker.check_model`, execute ONNX Runtime on the golden fixture, compare outputs to manifest tolerances, verify file hashes, inspect exactly five fold metrics, and confirm `release_ready` is false with the synthetic benchmark warning present.

- [ ] **Step 7: Commit report changes**

```bash
git add glyph-training/src/wizardry_glyphs/experiment_report.py glyph-training/tests/test_experiment_report.py
git commit -m "feat: report grouped glyph validation results"
```
