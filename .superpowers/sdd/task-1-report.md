# Task 1 Report: Explicit lineage schema and propagation

## RED evidence

Command:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: expected RED. Before implementation, the new schema tests failed because `lineage_group` was not required or exposed; the augmentation propagation assertion also failed. The focused run reported 10 failed, 10 passed.

## GREEN evidence

Command:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: `20 passed in 0.06s`.

## Follow-up fixture and immutable augmentation evidence

Commands:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: `21 passed in 0.14s`.

`uv run --python 3.12 python -m pytest -q`

Result: `34 passed, 8 warnings in 11.55s`.

Changes:

- Added `lineage_group` as a required non-empty string property in `glyph-training/dataset/schema-v1.json`; the existing root `additionalProperties: false` remains in force.
- Added a real immutable `GlyphExample` augmentation test.
- Reworked augmentation to build new immutable `GlyphStrokeData`/`GlyphPointData` values instead of mutating frozen nested dataclasses; mutable records retain their existing mutation path and lineage.
- Migrated legacy test fixture builders to stable lineage values.

Self-review: production schema, augmentation, and all focused/full tests were checked; no corpus generation or splitter behavior was changed. Warnings are existing ONNX/torch export deprecation warnings only.

## Initial full-suite evidence

## Changes

- Added mandatory `GlyphExample.lineage_group: str` and validated it with the existing non-empty text validator.
- Added `lineage_group` to required JSON fields and passed it through `_record`.
- Preserved lineage during immutable `GlyphExample` augmentation reconstruction and mutable-record augmentation.
- Added focused schema tests for missing, valid, and empty lineage values.
- Updated augmentation fixture and asserted exact lineage preservation.

Commit SHA: `2035c2812bc13dc651aff75cd6b45e3ce40c82f4`

- Searched all `GlyphExample(...)` call sites in `glyph-training`; schema parsing and augmentation reconstruction both pass lineage explicitly.
- Confirmed only the four brief-specified files changed.
- No corpus generation or splitter behavior was modified.

## Commit

Commit subject: `feat: require glyph lineage identity`

Commit SHA: pending

## Concerns

The full suite currently contains legacy fixtures without `lineage_group`. Updating those fixtures is outside Task 1's four-file scope and would overlap later corpus/validation work.
