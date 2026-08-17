# Scribe↔Glyphcraft Equivalence — Design Spec

## Goal

Prove the domain's central invariant: a Scribe textual expression and a Glyphcraft drawn composition that denote the same enchantment normalize to the **same canonical `EnchantGraph`**, the **same calculated cost**, and thus identical runtime behavior. This is a cross-front-end guarantee enforced as a conformance-style test, not a claim.

Links: living spec `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md`; model `docs/superpowers/specs/2026-08-14-enchantgraph-cost-engine-design.md`; Scribe `docs/superpowers/specs/2026-08-14-scribe-language-expansion-design.md`; Glyphcraft `docs/superpowers/specs/2026-08-14-glyphcraft-composition-design.md`; persistence `docs/superpowers/specs/2026-08-14-canonical-persistence-versioning-design.md`; CI `docs/superpowers/specs/2026-08-14-ci-coverage-hardening-design.md`.

## Scope

- Define the equivalence relation and its testable surface.
- Provide the canonical worked example from the concept and make it a pinned test.
- Integrate the equivalence suite into CI (via the CI hardening spec).

## Equivalence relation

Two authored forms are **equivalent** iff they denote the same enchantment:

```text
EnchantGraph(graphA) == EnchantGraph(graphB)
  AND CostReport(graphA) == CostReport(graphB)
```

where equality is on the canonicalized graph and the cost report, not on authored text/pixels.

- The same canonical graph and cost ⇒ identical runtime behavior (same trigger/conditions/effects/targets/modifiers, same cooldown/charges, same budgets).
- This is the convergence invariant in the living spec and the reason both front ends share one model + one cost engine.

## Canonical worked example (pinned test)

The concept's example is the must-pass equivalence target:

Scribe:
```text
when wearer hits target:
    deal 4 fire to enemies within 3 of target
cooldown 8s
```

Glyphcraft composition: `Blade → Flame contained by Area(radius=3), Clock(8s)`.

Must produce **identical** canonical graphs, calculated costs, cooldown behavior, targeting, and damage results. This exact pair is a pinned equivalence fixture.

## Equivalence fixtures corpus

A corpus of authored pairs, one per distinct semantics, each asserting `EnchantGraph == EnchantGraph` and `CostReport == CostReport`:

- flat subset mapped into the graph (existing 5-op behavior through the flat seam);
- each trigger source;
- each condition kind, including presence-guarded nullable context;
- each target selector and area scope;
- bounded arithmetic modifiers;
- cooldown and charges;
- single-layer branches;
- budget-boundary graphs (max slots/power/complexity/stability).

Each pair uses Scribe source and a Glyphcraft composition that the Glyphcraft semantics fix (from the composition spec's role mappings). The corpus is generated deterministically (like the existing conformance generator) and checked in CI.

## Cost-engine equality

- Because cost is computed from the normalized graph, equal graphs ⇒ equal cost **by construction**. The equivalence suite still asserts the cost equality independently so a regression in the cost engine that prices identical graphs differently is caught.
- Cost equality asserts all four budgets (slots, power, complexity, stability) and the balance version.

## Runtime equality

- For equivalent pairs, runtime behavior must match: same trigger firing, same condition evaluation, same target resolution, same effects in canonical order, same cooldown/charge state transitions, same target-disappearance skip-not-retarget behavior.
- Asserted at the model/runtime seam (graph → runtime), not through a live Minecraft server (no server/client session in this environment). A live Paper smoke test stays a separate manual gate per the MapGUI + Paper designs.

## Verification

Automated tests must prove, at minimum:

- The canonical `FIRE_DAMAGE(4) → AREA(radius=3)` + `cooldown 8s` pair yields identical canonical graphs and identical cost reports (worked example).
- Every equivalence fixture in the corpus yields identical graphs and costs.
- A graph/cost difference for any pair — same behavior authored two ways — fails the suite (each fixture has an expected divergent negative case too, to prove the assertion detects inequality).
- Cost equality independently re-asserts all four budgets despite being implied by graph equality.
- Runtime transitions (cooldown, charges, target skip) match for the canonical pair.
- The equivalence suite runs in CI as part of the extended CI spec's baseline assertions.

## Interfaces produced (for later specs)

- Equivalence assertion helper `assert_equivalent(scribeInput, glyphDraft) -> ()` (Rust reference; Java production matching), plus a deterministic pair-corpus generator.

Consumed by CI (the hardening spec's pinned baseline includes this suite).