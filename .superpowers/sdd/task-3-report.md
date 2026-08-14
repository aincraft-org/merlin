# Task 3 report: independent synthetic template lineages

## Final result

Implemented geometry validation, transform-tolerant structural duplicate screening, explicit independent provenance, propagation through generated records, and manifest tamper validation.

## Commits

- `04f32e2ff0e26571cbdecde709cd34305d348465` — `feat: enforce independent glyph lineage corpus`
- `e3269a2fee2edbd21c625f213b2c3c3f1112c558` — `fix: gate duplicate glyph template lineages`
- `f19deee752e76c45a30f618ab5a4fee4297f2aad` — `fix: validate independent glyph geometry`
- `affc96a6a04f21defabdde57d2f8095f3e18e89f` — `fix: make glyph fingerprints transform invariant`
- `101f2a5ce16aea3dc1cd3e5c14b38fad981eb376` — `fix: require independent glyph provenance`
- `fad10d36544622490d714d2ea78c1eff88e820f8` — `fix: propagate independent glyph provenance`
- `aa5739778a691ea5af2b2b4ea0fdf2958695bd3f` — `fix: verify generated corpus manifest structure`
- `e6e04fa4e45cdfe58f058cf34f9d976a8d6f2ee8` — `fix: verify provenance per glyph lineage`
- `c1d90a6409396d58b2e341ee439e1ae049bc14d7` — `fix: reject conflicting glyph lineage provenance`

## Evidence

- Focused development corpus tests: `9 passed`.
- Full Python 3.12 suite: `48 passed, 8 warnings`.
- `py_compile` passed for `dev_corpus.py` and `schema.py`.

## Scope

- Geometry validation aggregates malformed templates by label and rejects nonempty-list/finite two-number violations.
- Fingerprints preserve stroke count/order/topology while collapsing translation, scale, rotation/reflection, and small jitter via named quantization.
- Templates require six unique nonblank `independent_source` declarations per label.
- Generated JSONL carries opaque provenance IDs; manifest stores SHA-256 provenance summaries and groups/lineages. Validator detects tampering.
- `dataset/schema-v1.json` requires nonblank `independent_source`; generated-record/schema regressions cover missing and whitespace values.

## Exact provenance mapping

- Manifest provenance now maps each label's `lineage_group` to the SHA-256 of its opaque `independent_source`.
- Validator compares the exact mapping, detecting row-level provenance swaps even when the source hash set is unchanged.
- Verification: focused `9 passed`; full Python 3.12 `48 passed, 8 warnings`; py_compile passed.

## Final helper verification

- `_provenance_map` is used by both generation and validation and rejects conflicting sources for one label/lineage.
- Focused development corpus tests: `10 passed`.
- Full Python 3.12 suite: `49 passed, 8 warnings`.
- `py_compile` passed for `dev_corpus.py` and `schema.py`.

## Validation conflict handling

- Validator catches provenance-map conflicts and returns structured errors instead of propagating tracebacks.
- Added tampered JSONL conflict regression.
- Final verification: focused `11 passed`; full Python 3.12 `50 passed, 8 warnings`; py_compile passed.

## Conservative topology fingerprint

- Duplicate screening now ignores coordinates entirely, using ordered stroke point counts and closed/open status; any coordinate jitter or rigid transform of the same topology collapses.
- Added large-jitter unique-metadata regression.
- Final verification: focused `12 passed`; full Python 3.12 `51 passed, 8 warnings`; py_compile passed.
