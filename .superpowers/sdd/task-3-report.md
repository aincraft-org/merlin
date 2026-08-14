# Task 3 report: independent synthetic template lineages

## Result

Implemented the explicit independent-template catalog contract in `glyph-training/src/wizardry_glyphs/dev_corpus.py` without modifying or fabricating the deficient production catalog. Every label, including `reject`, now requires at least six explicit non-empty templates before generation. Validation aggregates all deficient labels in one error and generation does not create output before prerequisites pass.

Valid fixture catalogs generate one stable `lineage_group` per explicit template; derivatives inherit their base lineage. The development manifest now records per-label `lineages` and `lineage_counts`, and corpus validation checks both against JSONL.

## Files

- `glyph-training/src/wizardry_glyphs/dev_corpus.py`
- `glyph-training/tests/test_dev_corpus.py`
- `.superpowers/sdd/task-3-report.md`

The existing `glyph-training/catalog-geometry-v1.json` remains unchanged and intentionally fails the new prerequisite gate because it contains legacy single `strokes` entries rather than six explicit templates per label.

## RED/GREEN evidence

- RED: `pytest -q tests/test_dev_corpus.py` initially failed both new tests: legacy generator reported `missing geometry for target-ray` instead of the aggregated prerequisite error, and valid fixture generation rejected the new `templates` schema.
- GREEN: `.venv/bin/python -m pytest -q tests/test_dev_corpus.py` → `2 passed`.

## Verification

- Focused tests: 2 passed.
- Full Python 3.12 suite: `.venv/bin/python -m pytest -q` → `41 passed, 8 warnings`.
- Deficient-catalog smoke check: generation raised one error containing all 12 labels (`target-ray`, `damage`, `heal`, `push`, `cooldown`, `self`, `target`, `physical`, `fire`, `frost`, `arcane`, `reject`) and no corpus was generated before validation.
- System `pytest -q` was not usable because it invoked Python 3.14 without torch; the project `.venv` Python 3.12 run is the authoritative suite.

## Commit

Commit subject: `feat: enforce independent glyph lineage corpus`

SHA: `1e64cfcab8ab7fc5d40aae6032762b45cf966f80`.

## Concerns

The real development catalog remains intentionally deficient per controller resolution. A future catalog-authoring task must supply six genuinely independent templates per label; this change deliberately does not synthesize or transform legacy geometry into independent templates.
