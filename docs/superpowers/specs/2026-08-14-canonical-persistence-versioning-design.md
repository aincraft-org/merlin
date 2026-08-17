# Canonical Persistence + Versioning — Design Spec

## Goal

Persist authored enchantments durably and version them so item behavior never changes silently. Three artifacts are persisted together; loading uses the canonical graph when its version is supported, and migration is an explicit author action, never an automatic balance migration.

Links: living spec `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md`; model `docs/superpowers/specs/2026-08-14-enchantgraph-cost-engine-design.md`.

## Scope

- Define the persisted artifact set and their relationship.
- Define versioning and the migration rules for the canonical graph and validation metadata.
- Keep the existing Paper persistence behavior (`ScribeBookStore` sources) working and extend it without regressing the verified 2026-08-14 contract.

## Persisted artifacts

Three related artifacts persist together, under a stable key (per book instance / enchantment):

1. **Original authored representation** — Scribe source text or Glyph vector draft. Editable authoring data; not used for runtime execution.
2. **Canonical graph** + its schema version — the runtime input.
3. **Validation metadata** — content definitions version and balance version that priced/validated this graph.

### Storage shape

```text
PersistedEnchantment
  key                    // opaque stable id (enchantment/book instance) + player binding where applicable
  sourceRepr
    scribeSource          | null
    glyphDraft            | null
  graph
    EnchantGraph          // exactly the canonical graph
    graphSchemaVersion    // u64
  validation
    contentVersion        // content definitions version
    balanceVersion        // balance data version that priced the graph
  attachedItem            // null until attached
  cooldown/chargeState    // runtime instance state keyed by enchantment instance
```

Both front ends write through the same persistence seam, so a Scribe-sourced and Glyphcraft-sourced enchantment with the same graph persist identically except for `sourceRepr`.

## Versioning rules

- Loading uses the canonical graph when its version remains **supported**.
- Migration converts older canonical versions **explicitly** (a migration table + asserted transformation), never approximately.
- **Recompilation from source is an author action**, not an automatic balance migration. Compiler changes could otherwise silently change item behavior; therefore loading never recompiles source to reconcile a graph if the stored graph is authoritative and version-supported.
- A `graphSchemaVersion` outside the supported range does **not** execute and is surfaced for migration.
- `balanceVersion` on load differing from current balance data → surface for explicit author decision (re-price by revalidation), never silent re-price.

## Migration rules (concrete)

- Backward migration: vN → vN+1 must produce a graph that re-validates and re-prices deterministically; assert canonical identity is preserved when semantics are unchanged.
- A migration is irreversible-by-default: do not auto-downgrade; preserve the original authored representation so a future explicit reauthor remains possible.
- Migration is logged; a failed migration leaves the persisted artifact unchanged and the enchantment non-executing (surfaced).

## Integration with existing Paper behavior (verified)

- `ScribeBookStore` already persists Scribe source in plugin-owned data (not vanilla book pages). That source remains artifact (1).
- `/scribe book` + right-click chat editor, `/scribe save`, `/scribe cast` stay intact. `Save & Cast` persists the submitted draft, compiles, and casts only on success; invalid source causes no effect/cooldown mutation and reopens with diagnostics.
- Save persists source without casting; Cancel/Escape do not modify saved source; callbacks one-use, player-bound, exact-book-bound, 15-minute expiry. These contracts are unchanged and must not regress (verified Paper behavior: 2 tests).

## Error handling

- Persistence failure → item unchanged, draft retained for retry.
- Unsupported graph version → not executed, surfaced for migration (per versioning rules).
- Balance-version mismatch on load → surfaced for explicit author decision.
- Migration failure → artifact unchanged, enchantment non-executing, surfaced.

## Verification

Automated tests must prove:

- A persisted enchantment round-trips: save → load → identical canonical graph, schema version, validation metadata, and runtime state.
- Editing the authoritative source and reauthoring produces an updated graph only through an explicit action; loading the stored graph never silently recompiles.
- vN → vN+1 migration re-validates/re-prices deterministically and preserves canonical identity when semantics are unchanged; unsupported future versions do not execute.
- Balance/content version mismatch on load requires explicit author action before re-price; a matching version loads without prompt.
- Migration failure leaves the artifact unchanged and the enchantment non-executing.
- Paper editor/book-store contracts (source persistence, save/cast/cancel/escape, callback one-use + 15-min expiry, unambiguous-book binding) have no regressions.

## Interfaces produced (for later specs)

- `PersistEnchantment` / `LoadEnchantment` seam (store adapter + version check, Rust reference; Java production).
- Graph migration registry `v(N) -> v(N+1)`.
- Source-representation (Scribe/Glyph) storage adapters.

Consumed by the CI and equivalence specs.