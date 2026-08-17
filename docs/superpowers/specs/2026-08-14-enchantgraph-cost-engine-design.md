# EnchantGraph Shared Model + Cost Engine — Design Spec

## Goal

Introduce the shared typed operation graph that both authoring paths (Scribe textual and Glyphcraft drawn) normalize into, plus the cost engine that prices a graph against item budgets. This is the largest missing capability in the Wizardry domain and the single convergence point for Scribe and Glyphcraft.

Links: concept `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`; spine `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md`.

## Scope

- Define the canonical `EnchantGraph` representation (schema-versioned) and its validation pipeline.
- Define the cost engine: four item budgets (slots, power, complexity, stability), balance data, and cost rules.
- Provide the compatibility seam from the existing flat 5-op spell runtime to the graph, so existing verified behavior is not orphaned.
- Do **not** define the Scribe language expansion or Glyphcraft composition — those specs consume this model.

## Canonical representation

A typed directed graph:

```text
EnchantGraph
  version                      : schema version (u64)
  trigger                      : exactly one Trigger node
  conditions[]                 : 0..n Condition nodes
  effects[]                    : 1..n Effect nodes
  targetSelectors[]            : typed selectors referenced by effects
  modifiers[]                  : 0..n Modifier nodes (scope: effect or graph)
  limits
    cooldown                  : seconds (optional)
    charges                   : count (optional)
  cost
    slots / power / complexity / stability   <- per-budget calculated totals
  metadata
    sourceRepr                 : optional Scribe source or Glyph draft (editable authoring data)
    balanceVersion             : which balance data version priced this graph
```

### Node categories

- **Trigger** (exactly one): `ON_HIT`, `ON_DAMAGE_TAKEN`, `ON_BLOCK`, `ON_USE`, `ON_KILL`, `INTERVAL`. Each exposes typed context:
  - `ON_HIT` → wearer, target, damage
  - `ON_DAMAGE_TAKEN` → wearer, attacker?, damage
  - `ON_BLOCK` → wearer, attacker?, blockedDamage, item
  - `ON_USE` → wearer, item, target?
  - `ON_KILL` → wearer, victim, damageType
  - `INTERVAL` → wearer, item
- **Condition** (0..n): numeric comparison, entity classification, status presence, damage-type comparison, environment predicate, resource predicate. Multiple conditions `AND` by default. A nullable context value (`attacker?`) cannot feed a definite-entity operation unless guarded by a presence condition.
- **Effect** (1..n): damage, status, shield, heal, push/pull, projectile, charge, event modification (only where the trigger permits). Each effect has an operation, a target selector (or explicit self), arguments, and modifiers.
- **Target selector** (typed): self, attacker, hit target, nearest enemy, nearby allies, area around another selector. Valid only when its required context exists.
- **Modifier**: magnitude, radius, duration, chaining, repetition, damage conversion, cooldown, charges, projectile count. Scope may be per-effect or graph-wide.

### Canonicalization rules

- Canonical ordering is deterministic: triggers first, then conditions, effects in authored order (Scribe) or stroke-confirmed order (Glyphcraft), modifiers attached to their effect. Cost evaluation uses the normalized order.
- Equivalent Scribe and Glyphcraft inputs must normalize to the **same** `EnchantGraph` and thus the same cost. This is the convergence invariant (see the equivalence spec).
- The graph is the only runtime input. Source/draft stay attached as editable authoring data.

## Validation pipeline

Every candidate graph passes, in order:

```text
Candidate graph
  → structural validation
  → type validation
  → capability validation
  → cycle and bound validation
  → cost calculation
  → item-budget validation
  → canonicalization
  → persistence
  → runtime registration
```

Reject:

- Missing or multiple initial triggers.
- Inputs of the wrong type.
- References absent from the trigger context.
- Operations the player has not unlocked (capability gate).
- Unbounded cycles or repeated execution.
- Budget overflow.
- Effects unsupported by the destination item.

A failed validation never partially attaches or executes. Rejection exposes structured errors **associated with graph nodes** (node id + error code + message), not just a flat list.

## Cost engine

### Four budgets

An item or page tier supplies four budgets:

1. **Slots** — number of graph operations.
2. **Power** — calculated output magnitude.
3. **Complexity** — branching, transformations, targeting, graph structure.
4. **Stability** — cost of aggressive timings and risky combinations; produces deterministic drawbacks (increased charge consumption, longer lockout, durability cost, reduced magnitude). Never random destruction.

### Cost rules

- Restrictions reduce power cost: a cooldown, limited charges, narrow target category, or demanding condition make an effect cheaper.
- Cost engine evaluates the **normalized graph**, so equivalent Scribe/Glyphcraft enchantments have equal costs.
- All multipliers, rounding rules, and thresholds live in **shared balance data**, not in either front end or in code.

### Worked example (from concept)

Item budgets: slots 6, power 12, complexity 5, stability 10.

Enchantment: `ON_HIT → FIRE_DAMAGE(4) → AREA(radius=3)`, cooldown 8 s.

```text
On-hit trigger      1 slot
Fire effect         1 slot, 4 power
Area modifier       1 slot, ×1.8 power, 2 complexity
Cooldown            1 slot, ×0.8 power
Graph overhead      1 stability

Final power: 4 × 1.8 × 0.8 = 5.76, rounded to 6
Final cost:  4/6 slots, 6/12 power, 2/5 complexity, 9/10 stability remaining
```

All multipliers/rounding/thresholds are balance data.

## Runtime execution

The runtime executes the canonical graph:

```text
Trigger fires
  → capture immutable event context
  → evaluate conditions (AND)
  → resolve target selectors against context
  → calculate bounded effect values
  → apply effects in canonical order
  → record cooldown/charge state
```

- Runtime state is keyed by enchantment **instance**, not by source representation.
- A runtime operation that loses a target between selection and application **skips** that target; it does not retarget unless the graph explicitly requests fallback targeting.
- Runtime arithmetic clamps to declared bounds; non-finite compiler expressions are rejected **before** persistence.

## Compatibility seam (existing flat runtime)

The existing `SpellRuntime`/5-op flat path must not be orphaned. Provide a deterministic mapper from the flat `CompiledSpell` op list to a minimal `EnchantGraph` (one `ON_HIT`-style trigger implied by target presence, no conditions, flat effects, cooldown limit) so:

- Every existing Paper test contract is preserved (no regression).
- The mapper is conformance-tested against the Rust corpus so identity/canonicalization are stable.
- New graph-native behavior supersedes the flat path; the flat path remains a documented v0 subset reachable only via the seam, never the primary execution mode for newly authored enchantments.

## Balance data schema (versioned)

```text
BalanceManifest
  version
  budgets           // defaults per item/page tier
  effectCosts       // per effect operation: { baseCost, powerContribution, complexity, stability }
  modifierMultipliers
  restrictionDiscounts
  roundingRule      // e.g. round-half-up power, floor slots
  maxPower / maxComplexity / maxStability  // absolute ceilings (overflow reject)
```

Balance changes bump `balanceVersion`; graphs persist which balance version priced them.

## Error handling

- Structural/type/capability/cycle/budget errors → structured node-associated diagnostics.
- Budget overflow → rejection before persistence; never emission of an over-budget graph.
- Persistence failure → item unchanged, graph retained for retry.
- Balance-version mismatch on load → surface for explicit migration decision (author action), never silent re-price.

## Verification

Automated tests must prove:

- A minimal `EnchantGraph` round-trips: build → validate → canonicalize → cost → persist → reload → identical graph + cost.
- Equivalent Scribe and Glyphcraft inputs produce identical graphs and identical calculated costs (cross-front-end test helper).
- Each of the seven pipeline stages rejects its specific class of invalid graph (missing/multiple trigger, wrong type, missing context, locked capability, unbounded cycle, budget overflow, unsupported effect).
- The flat→graph mapper reproduces the Rust corpus identity for every fixture (no regression vs verified 2026-08-14).
- Cost arithmetic matches the worked example exactly (4/6 slots, 6/12 power, 2/5 complexity, 9/10 stability).
- `balanceVersion` is recorded and a bumped version is rejected on load without author consent.
- Runtime condition gating, target disappearance (skip-not-retarget), cooldown/charge state, and bounded clamping behave as specified.
- The four-budget absolute ceilings overflow rather than emit an over-budget graph.

## Interfaces produced (for later specs)

- `EnchantGraph` construction + validation entry point (Rust reference; Java production matching Rust).
- `CostEngine::price(&EnchantGraph, &BalanceManifest) -> CostReport` (Rust) / Java equivalent.
- Flat→graph mapper `from_flat(&CompiledSpell, &BalanceManifest) -> EnchantGraph`.
- Structured node diagnostics type.
- `BalanceManifest` load + version check.

Consumed by the Scribe-expansion, Glyphcraft-composition, persistence, and equivalence specs.