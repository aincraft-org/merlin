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
