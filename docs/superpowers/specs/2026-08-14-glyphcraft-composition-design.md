# Glyphcraft Composition — Design Spec

## Goal

Extend the Glyphcraft authoring surface from the verified draw-and-classify single-glyph flow to **semantic composition**: players assemble recognized glyphs into a candidate graph (trigger, conditions, effects, target arrows, modifiers) that converges to the shared `EnchantGraph`, is validated and priced by the shared cost engine, and is confirmed before attachment.

Links: concept `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`; recognition `docs/superpowers/specs/2026-08-12-glyph-recognition-design.md`; living spec `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md`; model `docs/superpowers/specs/2026-08-14-enchantgraph-cost-engine-design.md`.

## Scope

- Define glyph roles and their mapping to graph nodes.
- Define composition semantics: how arranged glyphs construct a candidate graph.
- Define the recognition → candidate-graph → confirmation flow.
- Preserve the "drawing neatness never scales power" and "confidence never scales power" invariants.
- Do **not** add arbitrary arithmetic, general branching, or handwritten syntax. Glyphcraft stays spatially compositional and mechanically distinct from Scribe.

## Canvas and draft model (unchanged, verified)

- `GlyphDraft`: vector strokes on a full 128×128 canvas; each stroke has ordered points, brush width, timing. Bitmaps are derived (full-canvas + normalized crop) and never authoritative.
- Stroke segmentation by pause/proximity; vector authority for undo/clear/snapshot; close preserves draft.
- See `docs/superpowers/specs/2026-08-12-glyph-recognition-design.md` for the deterministic recognizer contract currently verified.

## Glyph roles (→ graph mapping)

Each glyph has one of these typed roles and maps to an `EnchantGraph` construct:

| Role | Graph construct | Example glyph intent |
|------|-----------------|----------------------|
| Trigger | the single `trigger` | "on hit", "on damage taken", "on use", "every N s" |
| Condition | `conditions[]` | "health < 30%", "target is undead", "outdoors" |
| Effect | `effects[]` | flame (damage), barrier (shield), mend (heal), knock (push) |
| Target/Arrow | target selector | self, attacker, hit target, nearest enemy, area around selector |
| Containment | area scope on an effect | "enemies within radius", area modifier |
| Repetition | bounded count modifier | ×N repeats |
| Clock | cooldown/periodic timing | cooldown N s |

Trigger glyph starts a graph; condition glyphs gate paths; effect glyphs produce operations; arrows connect or set target; containment expresses area/scope; repetition marks add bounded counts; clock glyphs specify cooldown/periodic timing.

The verified 2026-08-14 catalog contains `target-ray`, `damage`, `heal`, `push`, `cooldown`, `self`, `target`. These map to the v1 subset (effect/heal/push, cooldown limit, target arrows). Trigger/condition/containment/repetition roles are new glyphs to be added to the catalog with the same documented `intent` + `ambiguity_risks` entry format.

## Composition semantics

Initial topology: one trigger, optional conditions, effects, modifiers, and bounded linear connections.

- Placement order (stroke-confirm order or arrow direction) determines graph wiring; an arrow out of a trigger into an effect's target box sets the target selector; containment placed on an effect adds area scope.
- The current release permits at most one trigger and bounded (v1) linear connections. Ambiguity/overlap between target-selector and effect stroke sets is disambiguated by confirmed interpretation.
- Composition is deterministic: same drafted arrangement → same candidate graph (given identical recognition results).
- Drawing neatness, stroke count beyond the minimum template, and recognition confidence never alter effect magnitude or cost. They affect only whether a symbol is recognized and which candidate is ranked.

## Recognition → candidate graph → confirmation

```text
stroke composition
  → segment by pause and proximity
  → recognize each glyph (deterministic template/features)
  → build candidate graph from recognized roles + arrows + containment
  → shared validation and costing (EnchantGraph pipeline)
  → display interpreted trigger/conditions/effects/targets/modifiers + calculated costs
  → player confirmation (or correction of ambiguous candidates)
```

- Recognition constructs **candidate graphs**; low-confidence or tied results are presented as ranked valid candidates and require player correction — never a silent low-confidence pick.
- Confirmation is required before attachment. Attaching writes the canonical graph through the persistence spec.
- A failed shared validation returns node-associated errors (from the cost/validation spec); an invalid candidate cannot attach.
- Closing a private screen preserves the draft so an unconfirmed composition is not lost.

## Errors and recoverability

- Recognition ambiguity → preserve strokes, ask player to select interpretation.
- Failed shared validation → structured node-associated errors, no attach.
- Persistence failure → item unchanged, draft retained for retry.
- Runtime target disappearance → skip only the invalid target (runtime contract, not Glyphcraft-specific).

## Verification

Automated tests must prove:

- Each glyph role maps to the expected graph node; role composition produces a valid candidate graph.
- Trigger/condition/effect/arrow/containment/repetition/clock arrangement compiles to the expected `EnchantGraph` (cross-check with the graph model spec).
- Failed validation yields node-associated errors and never attaches.
- Ambiguous drawings require confirmation and never silently select a low-confidence result.
- Drawing neatness and confidence do not alter normalized effect power or cost (regression: equal cost for same candidate regardless of drawing quality).
- Undo removes one authoritative vector stroke and updates recognition; closing/reopening preserves the draft.
- Deterministic composition: identical draft → identical candidate graph.
- Recognition stays deterministic for the same draft and catalog version (existing 1 `ConformanceTest`-style recognizer contract respected).

## Interfaces produced (for later specs)

- Glyph role registry + graph-mapping builder (Rust reference; Java production matching).
- Candidate-list + confirmation record type.
- Composition validator (delegates to EnchantGraph validation).

Consumed by the persistence, CI, and equivalence specs.