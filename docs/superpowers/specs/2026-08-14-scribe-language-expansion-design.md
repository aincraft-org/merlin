# Scribe Language Expansion — Design Spec

## Goal

Extend the Scribe textual DSL from the verified flat 5-op subset to the concept's v1 grammar: triggers, conditions, target selectors, effects, bounded arithmetic modifiers, and bounded branching — all compiling through the shared `EnchantGraph` and cost engine. Rust remains the semantic reference; Java remains the production compiler with byte-exact identity.

Links: concept `docs/superpowers/specs/2026-08-12-scribe-glyph-enchanting-design.md`; living spec `docs/superpowers/specs/2026-08-14-wizardry-domain-living-spec.md`; model `docs/superpowers/specs/2026-08-14-enchantgraph-cost-engine-design.md`.

## Scope

- Add trigger declarations, conditions, target selectors, bounded arithmetic, cooldowns, charges, and bounded branching to the language.
- Preserve the dual-compiler contract: same canonical bytes + SHA-256 identity across Rust and Java.
- Preserve all verified limits and atomic-rejection invariants.
- Do **not** add: arbitrary loops, recursion, user-defined events, reflection, mutable globals, or access outside the enchantment API. Arithmetic stays bounded and cannot produce non-finite values.

## Target grammar (concept v1 shape)

```text
program      := triggerDecl stmt* cooldownDecl?
triggerDecl  := "when" WHEN_SOURCE ":"  block
stmt         := conditionStmt | effectStmt | branchStmt | limitStmt
WHEN_SOURCE  := "wearer takes damage" | "wearer hits target" | "wearer uses item"
             | "wearer blocks" | "wearer kills target" | "every" NUMBER "s"
conditionStmt:= "if" expr ":" block          # else-branch optional
expr         := contextPath relOp literal    # e.g. wearer.health < 30%
             | "target is" ENTITY_CLASS
             | "has status" STATUS
             | "outdoors" | "is night"
contextPath  := "wearer.health" | "target.health" | ...
relOp        := "<" | "<=" | ">" | ">=" | "=="
literal      := NUMBER | NUMBER "%" | ENTITY_CLASS | STATUS
effectStmt   := verb targetSpec argList ["for" DURATION]
             | "shield" targetRef "by" NUMBER "for" DURATION
             | "heal" targetRef "by" NUMBER
             | "push" targetRef "by" NUMBER
             | "deal" NUMBER "to" targetSpec ("within" NUMBER)?
             | "apply" STATUS "to" targetRef ("for" DURATION)?
targetSpec   := targetRef | "enemies within" NUMBER | "allies within" NUMBER
targetRef    := "wearer" | "self" | "target" | "attacker"
limitStmt    := "cooldown" NUMBER "s" | "charges" NUMBER
```

Whitespace insignificant. Identifiers ASCII letters/digits/`_`, not starting with a digit. `NUMBER` finite decimal IEEE-754. `DURATION` in seconds. Comments are out of scope for v1.

**Explanatory note:** this is illustrative and intentionally bounded. The exact keyword set may adjust during implementation, but the structural envelope (one trigger, optional `if/else`, typed conditions, typed target selectors, bounded arithmetic modifiers, cooldown/charges limits) is the v1 contract.

## Typed context and validity

Each trigger exposes typed context (from the EnchantGraph model):

- `when wearer takes damage` → wearer, attacker?, damage
- `when wearer hits target` → wearer, target, damage (the wearer's dealt damage)
- `when wearer uses item` → wearer, item, target?
- `when wearer blocks` → wearer, attacker?, blockedDamage, item
- `when wearer kills target` → wearer, victim, damageType
- `every N s` → wearer, item

A condition referencing a nullable value (`attacker?`) is only valid in a gate that guards presence. A target selector is only valid when its required context exists. Unavailable context → capability/type diagnostics.

## Bounds and limits (extended conservatively)

- Source ≤ 4,096 Unicode scalars and ≤ 16 KiB UTF-8 (unchanged), ≤ 128 physical lines (matches the editor).
- Statements per program: ≤ 16 (kept). Features beyond the flat subset penalize **complexity budget** in the cost engine, not a raw statement cap — but the 4,096/16-statement compiler-level ceiling still holds as a hard reject.
- Diagnostics ≤ 32, sorted by UTF-8 start byte, code, message; end-exclusive UTF-8 byte spans; one-based Unicode-scalar line/column.
- Branching depth ≤ 1 for v1 (single `if/else` layer) — bounded branching.
- Arithmetic is bounded; any expression that would produce a non-finite value is a compile error, not a runtime clamp. Runtime clamps only declared bounds.

## Diagnostics

- Keep the existing diagnostic model (code, message, end-exclusive UTF-8 byte span, one-based scalar line/column, ≤32, sorted).
- Add node association for graph-mapped errors: each diagnostic on a trigger/condition/effect may carry a node path so the cost engine and IDE can map it back.
- Invalid tokens, types, unavailable context, locked capabilities, and unsupported effects all yield deterministic diagnostics.

## Dual-compiler contract (unchanged invariants)

- Rust is semantic reference; Java is production. Canonical bytes and SHA-256 identity match byte-for-byte for every accepted source.
- Rejected compilation exposes diagnostics only — never partial operations, canonical bytes, or identity.
- Java packages contain no Bukkit types; Paper consumes only a complete successful result.

## Conformance corpus extension

- Extend `conformance/cases` + `fixtures` (schema-v1 can stay if additive; otherwise bump schema and migrate) with v1 fixtures covering:
  - each trigger source;
  - each condition kind, including nullable-context presence guards;
  - each target selector, including `enemies within` / `allies within`;
  - bounded arithmetic modifiers and magnitude/duration multipliers;
  - cooldown and charges;
  - single-layer `if/else` branching;
  - all numeric boundaries and non-finite rejection;
  - diagnostic order/truncation at 32;
  - atomic rejection for every invalid class.
- Java `ConformanceTest` (currently 1 test driving the whole corpus) must pass the full v1 corpus byte-for-byte.

## Paper authoring integration

- The chat-fallback editor (`ScribeDialog`) already enforces 4,096-scalar / 16 KiB-line limits and one-use, 15-minute, player+book-bound callbacks; the expanded grammar fits that contract unchanged.
- `/scribe cast` remains the direct command path; unknown v1 constructs produce structured diagnostics surfaced in the reopened editor.
- No dialog API assumptions; the Dialog-API target remains a future concern behind the same editor boundary.

## Verification

Automated tests must prove:

- Each v1 trigger/condition/selector/effect compiles to the expected `EnchantGraph` node (cross-check against the graph model spec).
- Accept/reject at every numeric boundary and non-finite expression → atomic rejection.
- Nullable-context references without a presence guard → diagnostics; guarded references → accepted.
- Single-layer `if/else` produces the expected graph conditions; depth-2 nesting rejected.
- Canonical identity is stable under equivalent formatting and changes under semantic/order changes.
- Java passes the extended conformance corpus byte-for-byte; existing flat fixtures still pass (no regression vs verified 2026-08-14).
- Diagnostic ordering/truncation at 32 is deterministic.
- Editor enforces line/scalar/byte limits and callback lifetime bounds without change regressions.

## Interfaces produced (for later specs)

- `EnchantGraph`-typed compile result (trigger, conditions, effects, targets, modifiers, limits) in both Rust and Java.
- Node-associated diagnostic path.
- Extended conformance fixture schema (if bumped).

Consumed by the persistence, CI, and equivalence specs.