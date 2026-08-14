# Task 2 Report

## RED/GREEN evidence
- RED: focused pytest collection failed because `grouped_cross_validation_split` and `validate_partition_isolation` were absent (`ImportError`).
- GREEN: `uv run --python 3.12 python -m pytest tests/test_grouped_cross_validation.py` — 3 passed.
- Full suite: `uv run --python 3.12 python -m pytest` — 39 passed, 8 warnings.

## Files
- Modified `glyph-training/src/wizardry_glyphs/split.py`.
- Added `glyph-training/tests/test_grouped_cross_validation.py`.
- No training or generator code modified.

## Self-review
The implementation requires nonempty `lineage_group`, rejects mixed-label lineages, enforces six groups per label for five folds, performs seeded group shuffling and sample-count balancing, and validates partition label coverage and lineage overlap. Focused tests cover invalid inputs, deterministic differing seeds, stratification, complete groups, and isolation failures.

## Commit
29f15ae (`feat: add sealed stratified group folds`)

## Concerns
The existing legacy `grouped_split` remains unchanged for compatibility; new Task 2 behavior uses `lineage_group` exclusively.

## Follow-up review fixes
- Updated validator to allow repeated rows within a partition, reject cross-partition lineage reuse, and reject missing lineage.
- Feasibility now aggregates all deficient labels and reports dynamic folds+1 required counts.
- Follow-up focused: 3 passed. Full Python 3.12 suite: 39 passed, 8 warnings.
- Follow-up commit: 7f91d80 (`fix: tighten grouped fold isolation validation`).

## Final review fixes
- Replaced tautological test with complete row-membership and partition-isolation assertions; validator runs on real split output.
- Assignment and validator reject non-string or whitespace-only lineage groups.
- Focused: 3 passed. Full Python 3.12: 39 passed, 8 warnings.
- Commit: 2b693c0 (`fix: validate complete lineage assignment`).
