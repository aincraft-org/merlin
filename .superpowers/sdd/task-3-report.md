# Task 3 report: independent synthetic template lineages

## Result

Implemented the explicit independent-template catalog contract in `glyph-training/src/wizardry_glyphs/dev_corpus.py` without modifying or fabricating the deficient production catalog. Every label, including `reject`, now requires at least six explicit templates before generation. The mechanical gate also requires nonblank, unique template IDs and at least six distinct normalized geometry fingerprints per label; it rejects coordinate-only duplicates after translation/scale normalization. Validation aggregates all deficiencies and generation does not create output before prerequisites pass.

Valid fixture catalogs generate one stable `lineage_group` per explicit template; derivatives inherit their base lineage. The development manifest records per-label `lineages` and `lineage_counts`, and corpus validation checks both against JSONL. Fixture templates differ in topology, stroke count/order, and proportions; no semantic/human independence is claimed by the mechanical gate.

## Files

- `glyph-training/src/wizardry_glyphs/dev_corpus.py`
- `glyph-training/tests/test_dev_corpus.py`
- `.superpowers/sdd/task-3-report.md`

The existing `glyph-training/catalog-geometry-v1.json` remains unchanged and intentionally fails because it contains legacy single `strokes` entries rather than six explicit templates per label.

## RED/GREEN evidence

- Initial RED: focused tests failed because the legacy generator did not understand `templates` or aggregate prerequisites.
- GREEN after implementation: `.venv/bin/python -m pytest -q tests/test_dev_corpus.py` → `4 passed`.
- Added regression coverage for blank/duplicate IDs and normalized duplicate geometry.

## Verification

- Full Python 3.12 suite: `.venv/bin/python -m pytest -q` → `43 passed, 8 warnings`.
- Deficient-catalog smoke check previously confirmed one aggregated error containing all 12 labels before corpus generation.
- System `pytest` is not authoritative here because it invokes Python 3.14 without torch; the project Python 3.12 venv suite passes.

## Commits

- `04f32e2ff0e26571cbdecde709cd34305d348465` — `feat: enforce independent glyph lineage corpus`
- `e3269a2fee2edbd21c625f213b2c3c3f1112c558` — `fix: gate duplicate glyph template lineages`

## Concerns

The real development catalog remains intentionally deficient per controller resolution. A future catalog-authoring task must supply six genuinely independent templates per label; this change deliberately does not synthesize or transform legacy geometry into independent templates. The fingerprint is a practical mechanical duplicate gate, not proof of semantic independence.

## Follow-up fix

- Current SHA before commit: `d696a5ef44c80fba6c90f2bec430d8d20fa240b1`.
- Added six transformed/reflected/small-jitter duplicate regression coverage and malformed geometry aggregation coverage in `glyph-training/tests/test_dev_corpus.py`.
- Added `FINGERPRINT_QUANTIZATION`, finite/nonempty stroke-point validation, per-label geometry deficiency aggregation, and rotation/reflection/translation/scale-tolerant structural fingerprints in `glyph-training/src/wizardry_glyphs/dev_corpus.py`.
- Evidence: `.venv/bin/python -m py_compile src/wizardry_glyphs/dev_corpus.py`; focused `6 passed`; full Python 3.12 suite `45 passed, 8 warnings`.

## Follow-up review fix

- Removed all centroid-relative coordinate vectors from the fingerprint; corrected the turn-angle dot-product expression.
- Fingerprint now uses quantized stroke counts/point counts, ordered segment and all-pairs distances, cross-stroke distances, centroid radii, and unsigned turn magnitudes.
- Replaced the test-only fixture's congruent sixth shape with six unmistakably non-congruent topologies.
- Verification: Python 3.12 `py_compile` passed; focused `6 passed`; full suite `45 passed, 8 warnings`.
