# Wizardry Domain — Living Spec

> Status: active
> Last updated: 2026-08-16
> Owners: jlo

## Intent

Wizardry is a Paper (Minecraft) plugin framework for **authoring enchantments** through two distinct, converging front ends:

- **Scribe** authors enchantments with a constrained, safe textual DSL.
- **Glyphcraft** authors enchantments by drawing recognized glyphs on a private handheld MapGUI canvas.

Both front ends must normalize into one shared, validated enchantment representation and execute through one runtime. Authored enchantments must be deterministic and understandable before attachment, and progress by unlocking expressive capability — never by drawing quality or textual obscurity.

The entry-point concept document is `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`. This living spec is the durable domain catalog: it captures intent, boundaries, invariants, and horizon state. Focused design specs implement slices of Current/Next and link here.

## Current state (verified 2026-08-14)

The repository already ships a **trustworthy foundation** for a flat 5-op spell system, plus a full glyph-training ML pipeline. Verified green:

| Subsystem | Delivered behavior | Test evidence |
|---|---|---|
| Rust reference compiler (`scribe-compiler`) | DSL lexer/parser/validate/canonical/wire; 4096-scalar + 16 KiB UTF-8 bounds; 16 stmt / 4 effect caps; 32-diagnostic truncation; atomic rejection | 15 lib tests + 1 conformance test; `cargo fmt`/`clippy -D warnings` clean |
| Conformance corpus | 12 fixtures (7 invalid / 5 valid), schema-v1; `generate_conformance --check` passes | Rust↔Java byte-for-byte identity |
| Java compiler (`java-compiler`) | Lexer/parser/model, semantic validation, canonical bytes, SHA-256 identity; **passes Rust corpus exactly** (no Bukkit types) | 25 tests (glyph 4, ml 6, scribe 15) |
| Paper module (`paper`) | Scribe book store, chat-fallback edit dialog, `/scribe` commands, `SpellRuntime` (damage/heal/push/cooldown/ray) | 2 tests |
| MapGUI integration (`mapgui-integration`, Java 25) | Handheld screen, stroke tracker, draft-store adapter, ONNX classification service, plugin loader (requires MapGUI) | 12 tests |
| Python glyph-training (`wizardry-glyphs`) | JSON schema, dev corpus (≥6 independent lineages/label), grouped 5-fold CV + sealed test, ONNX export, experiment report | 73 tests |

The implemented spell DSL is:

```text
spell IDENTIFIER { target ray NUMBER; damage target TYPE NUMBER; heal self NUMBER; push target NUMBER; cooldown NUMBER s; }
```

Bounds: ray `1..32`, damage/heal `.5..20`, push `.1..3`, cooldown `0..60`. This is the **v0 flat op set**, not the concept's graph model.

## Boundaries

### In scope
- Shared enchantment graph (`EnchantGraph`): `Trigger → Conditions → Effects → Modifiers`.
- Cost engine with item budgets: slots, power, complexity, stability.
- Bounded Scribe language progression (conditions, targets, modifiers, bounded branching).
- Glyphcraft semantic composition and recognition, converging to the same graph.
- Persistence of a stable canonical graph + source, with explicit versioning/migration.
- Capability/progression gating shared by both authoring paths.
- Deterministic runtime execution of the canonical graph.
- CI coverage for all verified suites (Rust, Java, MapGUI, Paper, Python).

### Out of scope / non-goals
- General-purpose scripting, loops, recursion, mutable globals, filesystem/network/command access.
- Arbitrary handwriting or natural-language recognition.
- Drawing neatness as a source of enchantment power.
- Collaborative/wall authoring in v1 (map-independent wall canvases are future).
- Separate execution or balance implementations per front end.
- Random destruction/catastrophic failure on instability (deterministic drawbacks only).
- Recompiling canonical from source as an automatic balance migration.

## Invariants

Settled rules that must never be violated:

- **Dual-compiler identity.** Rust is the semantic reference; Java is the production compiler. Canonical bytes and SHA-256 identity must match byte-for-byte across both for every accepted source. Java packages contain no Bukkit types.
- **Atomic rejection.** A rejected compile exposes diagnostics only — never partial operations, canonical bytes, or an identity. Concurrently: a failed validation never partially attaches or executes an enchantment.
- **Deterministic canonicalization.** Equivalent formatting has identical canonical identity; changing semantics or order changes it.
- **Bounded source.** Program ≤ 4,096 Unicode scalars and ≤ 16 KiB UTF-8, ≤ 16 statements, ≤ 4 effects; diagnostics ≤ 32 sorted by UTF-8 start byte, code, message, with byte spans and scalar line/column. (Scribe language expansion must keep or extend these caps conservatively.)
- **Cost equivalence.** Equivalent Scribe and Glyphcraft enchantments have equal costs; the cost engine evaluates the normalized graph, never the authored surface.
- **Confidence never scales power.** Recognition confidence selects interpretability only; it never changes effect magnitude or cost.
- **Feature separation from graphics.** The Java compiler stack contains no Bukkit types; the ML/Glyphcraft capture model lives in the Java 21-compatible layer with a package boundary these specs re-state.
- **Lineage integrity in ML.** Every training example carries a stable `lineage_group`; cross-validation never crosses lineage boundaries; synthetic scores are never presented as real-player generalization evidence.

These are settled law; they are asserted in focused specs where behavior must uphold them.

## Implementation guidance

- **Reference over duplicate policy.** Any behavior defined in the concept and implemented in Java/Python must first be pinned in the Rust reference and conformance corpus, then matched byte-for-byte by Java.
- **Module seams.** Keep Java 21-compatible shared glyph/model code in `java-compiler`; MapGUI-only code in `mapgui-integration` (Java 25, `compileOnly` MapGUI API, required dependency/load ordering); Paper-only wiring in `paper`. No Bukkit types leak into `java-compiler`.
- **Canonical graph as the only runtime input.** Runtime executes `EnchantGraph`, never source text or pixels. Source/draft remain editable authoring data attached for recompile-on-author-action.
- **Cost/balance data is data, not code.** Multipliers, rounding rules, thresholds live in shared balance data both front ends consume.
- **Testing expectations.** Rust: `cargo test`, `cargo fmt --all --check`, `clippy -D warnings`, conformance `--check`. Python: `uv run --python 3.12 --extra test pytest -q`. Java/MapGUI/Paper: `./gradlew :module:test --rerun-tasks`. Pinned-suite counts include 15 Rust, 25 Java, 12 MapGUI, 2 Paper, 73 Python — a regression that silently drops tests should be challenged.
- **Anti-patterns (do not):** duplicate compiler policy in Paper; flat op runtime as the long-term execution model; cost computed from authored text; checkboxes marked done without matching test evidence; implementing Future-horizon items without promotion.

## Current

Active surface for the coming implementation wave.

- [ ] EnchantGraph shared model + cost engine (biggest missing capability)
- [ ] Scribe language expansion: conditions, target selectors, modifiers, bounded branching
- [ ] Glyphcraft semantic composition converging on the graph (roles, candidate graph, ambiguity confirmation)
- [ ] Canonical persistence/versioning + migration
- [ ] Scribe↔Glyphcraft equivalence, proven end-to-end
- [ ] CI coverage for Python and MapGUI in `ci.yml`
- [x] Stable Rust/Java dual compiler + conformance corpus (verified 2026-08-14)
- [x] Flat 5-op spell runtime + Scribe chat dialog + MapGUI draw screen (verified 2026-08-14)
- [x] Grouped lineage CV + sealed test + ONNX export ML pipeline (verified 2026-08-14)
- [x] Training/inference rasters are 64×64 2×2-max downsamples of the 128×128 round-brush bitmap (verified 2026-08-16; do not retrain on dotted sample rasters)
- [x] Glyph catalog expanded to 24 classes (12 composition-role glyphs + 6 lineages each) and RMSprop/0.003/100 selected from the CUDA sweep (verified 2026-08-16; 24-class ONNX not yet retrained)

### Current notes
- Concept: `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`.
- A cohesive applied spec series was authored on 2026-08-14 (this living spec + six focused specs listed in Decisions log) to drive the SDD plan workflow, replacing ad-hoc gap-filling.

## Next

Committed near-term after Current stabilizes. Not speculative.

- [ ] Progression/unlock gating shared by Scribe and Glyphcraft capability validation
- [ ] Progression-ordered skill/effect unlocks matching the concept's ordering
- [ ] Real-player capture lineage ingestion replacing the synthetic-only corpus

## Future

Parked ideas, later phases, explicit deferrals. Do not implement from here without promotion.

- [ ] Shared MapGUI wall canvases for teaching/collaborative rituals
- [ ] Trading signed source pages / glyph drafts between players
- [ ] Additional triggers and effect schools
- [ ] Cross-specialization composition beyond shared effect knowledge
- [ ] Visual cosmetic grading that does not affect mechanics

## Second convention

This living spec is the single north star for the domain. Focused design specs implement a Current item each and must keep invariants and non-goals in sync here when they change.

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-14 | Keep the Rust reference + conformance corpus as the byte-exact contract for every Java change | Already proven; guards against drift |
| 2026-08-14 | Build the shared `EnchantGraph` + cost engine spec as the first applied design | It is the largest missing capability and the convergence point for Scribe and Glyphcraft |
| 2026-08-14 | Raise flats/op runtime only via a compatibility seam; future runtime executes `EnchantGraph` | Vision requires graph execution; existing flat runtime is a v0 subset |
| 2026-08-14 | Persist canonical graph + validation metadata, not recompile-on-load | Compiler changes must not silently alter item behavior |
| 2026-08-16 | Glyph training/inference rasters are bit images from `GlyphRasterizer`, not 32 resampled dots | Existing F1 runs (baseline 0.92 / epochs-100 1.0) were trained on dotted skeletons; catalog shapes are distinctive as brush bitmaps (`damage`/`heal`/`target`/`self`/`target-ray`/`push`). Vector-only F1 0.11 is not evidence against a raster-first model. Bundles trained on dotted rasters are incompatible with the new preprocessor until retrained. |
| 2026-08-16 | Training optimizer is configurable (`adam`, `adamw`, `sgd`, `rmsprop`); default stays Adam | CUDA sweep on bit-image rasters: Adam/AdamW/RMSprop at lr=0.003 all reach cal F1 1.0 in 40 epochs; SGD does not at 0.003/0.03 and explodes at 0.3. Winner by cal F1 then runtime is RMSprop (test macro F1 0.9665). |
| 2026-08-16 | Persist sweep winner in `train-dev-basic-v1.json`: RMSprop, lr=0.003, 100 epochs, dim 32 | Combined F1-duration and optimizer matrices; Adam/AdamW tied on F1, RMSprop won the documented tie-break |
| 2026-08-16 | Classifier vocabulary is the Glyphcraft composition set (24 labels), not the v0 12-op subset | Living spec composition roles (triggers, conditions, shield, attacker, area, repeat, charges) must be classifiable; each label now has 6 independent geometry templates |
| 2026-08-16 | Model raster/vectors are a padded ink crop, not absolute canvas coordinates | Classification must follow the sigil shape; drawing the same glyph elsewhere on the 128×128 map cannot change the class |

## Open questions

- [ ] Does Scribe v1 lock extend the existing 16-statement / 4-effect caps, or establish per-feature budgets in the cost engine? (The EnchantGraph spec picks per-feature budgets; revisit against player-facing limits.)
- [ ] What exactly is an "unlock" (knowledge tree, player XP, item tier) in v1, and how does it map to capability gating? (Deferred to the progression spec.)