# Task 1 Report: Explicit lineage schema and propagation

## RED evidence

Command:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: expected RED. Before implementation, the new schema tests failed because `lineage_group` was not required or exposed; the augmentation propagation assertion also failed. The focused run reported 10 failed, 10 passed.

## GREEN evidence

Command:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: `20 passed in 0.06s`.

## Follow-up fixture migration evidence

Commands:

`uv run --python 3.12 python -m pytest tests/test_schema.py tests/test_preprocess_augment.py -q`

Result: `20 passed in 0.07s`.

`uv run --python 3.12 python -m pytest -q`

Result: `33 passed, 8 warnings in 8.43s`.

Migrated legacy test fixture builders in `glyph-training/tests/test_train_end_to_end.py` and `glyph-training/tests/test_validate_dataset.py` to emit stable lineage values derived from source, label, and existing grouping identity. This preserves fixture grouping semantics while satisfying the mandatory schema.

Self-review: searched all glyph-training `GlyphExample(...)` call sites and JSON fixture builders; no remaining direct constructor or generated test record omits lineage. Only test fixture builders were changed in the follow-up; production corpus generation and splitter behavior remain untouched.

## Full-suite evidence (initial run)

Command:

`uv run --python 3.12 python -m pytest -q`

Result: `5 failed, 28 passed, 4 warnings in 4.43s` before fixture migration; failures were legacy records missing `lineage_group`.

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
