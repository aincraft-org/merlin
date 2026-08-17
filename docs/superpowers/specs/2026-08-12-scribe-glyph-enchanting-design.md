# Scribe and Glyph Enchanting Design

## Status

Approved concept design for two enchanting specializations:

- **Scribe** authors enchantments with a constrained textual language.
- **Glyphcraft** authors enchantments by drawing recognized glyphs on a private handheld MapGUI canvas.
- Both compile into one validated enchantment representation and execute through one runtime.

## Goals

1. Let Scribe players program precise item behavior with a small, safe language.
2. Let Glyphcraft players create enchantments through spatial drawing rather than text.
3. Share effects, balance rules, validation, persistence, and runtime execution between both systems.
4. Make authored enchantments deterministic and understandable before they are attached to an item.
5. Progress by unlocking expressive capability rather than only increasing numerical strength.

## Non-goals

The initial system does not include:

- General-purpose scripting, arbitrary loops, recursion, mutable global state, filesystem access, network access, or command execution.
- Recognition of arbitrary handwriting or natural-language text.
- Drawing quality as a source of enchantment power.
- Collaborative wall authoring. MapGUI walls may support teaching or shared rituals later.
- Separate execution or balance implementations for Scribe and Glyphcraft.
- Random destruction or catastrophic failure when an enchantment is unstable.

## Shared Enchantment Model

Every authored enchantment normalizes into a typed directed graph:

```text
Trigger → Conditions → Effects → Modifiers
```

A graph contains exactly one trigger in the initial system, zero or more conditions, one or more effects, explicit target selectors, and optional modifiers or limits.

### Node categories

**Triggers** start execution and expose typed context values:

- `ON_HIT` exposes `wearer`, `target`, and `damage`.
- `ON_DAMAGE_TAKEN` exposes `wearer`, `attacker?`, and `damage`.
- `ON_BLOCK` exposes `wearer`, `attacker?`, `blockedDamage`, and `item`.
- `ON_USE` exposes `wearer`, `item`, and `target?`.
- `ON_KILL` exposes `wearer`, `victim`, and `damageType`.
- `INTERVAL` exposes `wearer` and `item`.

A nullable context value, such as `attacker?`, cannot feed an operation requiring a definite entity unless guarded by a presence condition.

**Conditions** gate execution:

- Numeric comparison, such as health below 30%.
- Entity classification, such as target is undead.
- Status presence.
- Damage-type comparison.
- Environmental predicates such as outdoors or night.
- Resource predicates such as charge above a threshold.

Multiple conditions use `AND` by default. Branching and `OR` are later Scribe capabilities, not initial Glyphcraft operations.

**Effects** produce observable results:

- Deal typed damage.
- Apply a status.
- Create a shield.
- Heal.
- Push or pull.
- Spawn a projectile.
- Restore or consume charge.
- Modify the triggering event where the trigger explicitly permits it.

**Targets** are typed selectors such as self, attacker, hit target, nearest enemy, nearby allies, or an area around another selector. A selector is only valid when its required context exists.

**Modifiers** alter an effect: magnitude, radius, duration, chaining, repetition, damage conversion, cooldown, charges, or projectile count.

### Canonical representation

A canonical representation stores semantic operations rather than source text or pixels:

```text
EnchantGraph
  version
  trigger
  conditions[]
  effects[]
    operation
    target
    arguments
    modifiers[]
  limits
    cooldown
    charges
  cost
    slots
    power
    complexity
    stability
```

The original Scribe source or Glyph draft remains attached as editable authoring data, but runtime execution uses only the canonical graph.

## Shared Constraints

Each item or page tier supplies four budgets:

1. **Slots** limit the number of graph operations.
2. **Power** limits calculated output magnitude.
3. **Complexity** limits branching, transformations, targeting, and graph structure.
4. **Stability** limits aggressive timings and risky combinations.

Restrictions can reduce power cost. A cooldown, limited charges, narrow target category, or demanding condition makes an effect less available and therefore cheaper. The cost engine evaluates the normalized graph so equivalent Scribe and Glyphcraft enchantments have equal costs.

Stability produces deterministic drawbacks such as increased charge consumption, longer lockout, durability cost, or reduced magnitude. It does not randomly destroy the item or authored work.

### Numeric example

An item provides:

```text
Slots:       6
Power:      12
Complexity:  5
Stability:  10
```

The enchantment is `ON_HIT → FIRE_DAMAGE(4) → AREA(radius=3)`, limited by an eight-second cooldown.

```text
On-hit trigger                 1 slot
Fire effect                    1 slot, 4 power
Area modifier                  1 slot, ×1.8 power, 2 complexity
Cooldown                       1 slot, ×0.8 power
Graph overhead                 1 stability consumed

Final power: 4 × 1.8 × 0.8 = 5.76, rounded to 6
Final cost:  4/6 slots, 6/12 power, 2/5 complexity, 9/10 stability remaining
```

All multipliers, rounding rules, and thresholds live in shared balance data rather than either authoring front end.

## Validation and Runtime Resolution

Both authoring paths use the same pipeline after producing a candidate graph:

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

Validation rejects:

- Missing or multiple initial triggers.
- Inputs of the wrong type.
- References absent from trigger context.
- Operations the player has not unlocked.
- Unbounded cycles or repeated execution.
- Budget overflow.
- Effects unsupported by the destination item.

The runtime registers the canonical graph against its trigger. When the trigger fires, it captures an immutable event context, evaluates conditions, resolves targets, calculates bounded effect values, applies effects in canonical order, and records cooldown or charge state. Runtime state is keyed by enchantment instance, not by source representation.

A failed validation never partially attaches or executes an enchantment. Runtime operations that lose a target between selection and application skip that target; they do not retarget unless the graph explicitly requests fallback targeting.

## Scribe Authoring System

### Language

Scribe exposes the shared model through a constrained textual DSL:

```text
when wearer takes damage:
    if wearer.health < 30%:
        shield wearer by 6 for 4s
cooldown 12s
```

The grammar supports trigger declarations, typed conditions, effects, target expressions, bounded arithmetic, cooldowns, and charges. Later progression may add multiple effects and branches.

The language excludes arbitrary loops, recursion, user-defined events, reflection, mutable globals, and access outside the enchantment API. Arithmetic is bounded and cannot produce non-finite values.

### Scribe IDE

A Scribe book is a marked book item whose source is stored in plugin-owned persistent item data. Right-clicking the book suppresses the vanilla book screen and opens a dynamic Paper 1.21.8 dialog. The dialog is the authoring surface; book pages are not the source of truth.

The dialog contains one multiline text input initialized from the book's saved source. It uses the maximum useful client dimensions—up to 1024 pixels wide and 512 pixels high—with a 4,096-character and 128-line limit matching compiler bounds. Actions are:

- **Save:** write the current draft to the same marked book after verifying the player still holds that exact item.
- **Save & Cast:** save, compile, validate, and cast atomically. Compilation failure saves the draft, performs no spell effect or cooldown mutation, and reopens the editor with line-and-column diagnostics.
- **Cancel:** close without changing the saved source.

The server receives dialog input through a one-use custom callback. A callback is bound to the player and the book instance opened; it cannot write to a different item after inventory movement or replacement. `/scribe book` creates a marked starter book, while `/scribe cast` remains an accessibility and testing path for casting the held book without opening the editor.

The IDE does not attempt syntax highlighting, autocomplete, cursor control, or live compilation because the vanilla dialog text widget does not expose those editor capabilities. Its contract is a large multiline source field, deterministic save behavior, compile diagnostics, and direct casting.

### Dual Compiler Contract

The Scribe language has two compiler implementations:

- **Rust is the semantic reference.** It defines grammar, diagnostics, validation, canonicalization, and golden conformance fixtures. It runs during development and CI, not inside Paper.
- **Java is the production compiler.** It runs in-process when a player saves or casts from the Scribe dialog. It has no JNI, native loader, sidecar, or subprocess dependency.

Both compilers implement the same bounded pipeline:

```text
Source
  → lexer and parser
  → typed syntax tree
  → capability and budget validation
  → canonical representation and SHA-256 identity
  → typed compile result
```

Java compiler packages contain no Bukkit types. Paper consumes only a complete successful result and does not duplicate compiler policy.

The repository carries a versioned, UTF-8 JSON conformance corpus generated from the Rust reference. Every fixture records source plus either the complete typed operations, canonical bytes, and SHA-256 identity or the complete ordered diagnostics. Java tests must pass the entire corpus before its output may execute in Paper.

Source is limited to 4,096 Unicode scalar values and 16 KiB of UTF-8. Programs contain at most 16 statements and four effects. Diagnostics are capped at 32 and compare exactly by stable code, message, end-exclusive UTF-8 byte span, one-based Unicode-scalar line and column, and deterministic order.

Canonical bytes and identity must match byte-for-byte across Rust and Java. Rejected compilation exposes no operations, canonical bytes, or identity. Saving an invalid draft remains allowed, but invalid source cannot cast.

### Progression

Scribe progression unlocks structure in this order:

1. One trigger and one effect.
2. One condition.
3. Trigger context values.
4. Bounded arithmetic modifiers.
5. Multiple effects.
6. Branches and alternative conditions.
7. Reusable, explicitly bounded page composition.

Effect knowledge is shared with Glyphcraft. Discovering Flame makes both the Flame language operation and Flame glyph available, subject to specialization progression.

## Glyphcraft Authoring System

### MapGUI surface

Glyphcraft uses a private handheld screen rendered by [MapGUI](https://github.com/FloG99/MapGUI). MapGUI provides a 128×128 handheld map canvas, cursor movement through player aim, click input, raw-pixel drawing, and interfaces on unmodified clients. Its map pixels are virtual per viewer, so Glyphcraft persists its own authoring model instead of relying on Minecraft `MapView` storage.

The default screen is a full drawing canvas:

- Right-click draws.
- A pause ends the current stroke.
- Left-click toggles a tool overlay.
- The overlay provides undo, clear, interpretation, and glyph hints.
- `Q` closes the screen after preserving the draft.

The first release uses private handheld authoring only. Shared MapGUI wall canvases are outside the initial scope.

### Authoritative draft data

Glyphcraft stores vector strokes rather than treating rendered pixels as authoritative:

```text
GlyphDraft
  version
  canvasWidth: 128
  canvasHeight: 128
  strokes[]
    points[]
    brushWidth
    startedAt
```

The complete 128×128 map-sized bitmap is derived for preview, export, and layout-aware features. Recognition additionally derives a padded, normalized crop bitmap so placement and unused canvas margins do not dominate classification. Vector geometry remains available for stroke-order, endpoint, intersection, and enclosure features.

### Recognition

Glyphcraft uses constrained symbol recognition, not general OCR:

```text
Strokes
  → segment by pause and proximity
  → normalize translation, scale, and rotation where allowed
  → classify known glyph candidates
  → detect connections and containment
  → construct candidate graph
  → shared validation and costing
```

Recognition considers stroke count and direction, relative intersections, endpoints, enclosed regions, and simplified path shape. Each glyph may provide several accepted stroke templates.

Recognition confidence determines whether the system can identify a symbol; it never scales effect power. Ambiguous symbols present ranked valid candidates for player correction. Before attachment, the interface displays the interpreted trigger, effects, targets, modifiers, and calculated costs. The player must confirm that interpretation.

### Glyph semantics

Glyphs have typed roles:

- Trigger glyphs start graphs.
- Condition glyphs gate paths.
- Effect glyphs produce operations.
- Arrows connect operations or specify direction and target.
- Containment expresses area or scope.
- Repetition marks add bounded counts.
- Clock glyphs specify cooldown or periodic timing.

Glyphcraft favors quick spatial composition. The initial topology supports one trigger, optional conditions, effects, modifiers, and bounded linear connections. It does not expose arbitrary arithmetic or general branching. This preserves a mechanical distinction from Scribe rather than making Glyphcraft a handwritten syntax.

### Progression

Glyphcraft unlocks spatial operators in this order:

1. Trigger and effect glyphs.
2. Target arrows.
3. Containment and area.
4. Timing and repetition marks.
5. Multiple connected effects.
6. Resonant layouts that apply explicit modifiers.
7. Nested, statically bounded structures.

## Persistence and Versioning

Persist three related artifacts:

1. Original Scribe source or Glyph vector draft.
2. Canonical graph and its schema version.
3. Validation metadata including content definitions and balance version.

Loading an enchantment uses the canonical graph when its version remains supported. A migration converts older canonical versions explicitly. Recompilation from source is an author action, not an automatic balance migration, because compiler changes could otherwise silently change item behavior.

Drafts remain editable even when they reference unavailable or removed content. They must validate successfully before being attached again.

## Error Handling

- Parser errors preserve Scribe source and report source ranges.
- Recognition ambiguity preserves Glyph strokes and asks the player to select an interpretation.
- Failed shared validation returns structured errors associated with graph nodes.
- Persistence failure leaves the item unchanged and retains the draft for retry.
- Runtime target disappearance skips only the invalid target.
- Runtime arithmetic clamps to declared bounds and rejects non-finite compiler expressions before persistence.
- Unsupported graph versions do not execute and are surfaced for migration rather than interpreted approximately.

## Verification Strategy

### Shared model

- Equivalent Scribe and Glyphcraft inputs normalize to the same canonical graph and cost.
- Invalid types, missing context, unsupported capability, unbounded topology, and budget overflow are rejected.
- Canonical ordering is deterministic.
- Cooldowns, charges, target disappearance, and effect ordering execute as specified.

### Scribe

- Valid programs compile to expected graphs.
- Diagnostics identify invalid tokens, types, unavailable context, and locked capabilities.
- Invalid drafts can be saved but cannot be attached.
- Arithmetic remains bounded for minimum, maximum, zero, and overflow-adjacent inputs.

### Glyphcraft

- Accepted templates recognize across reasonable translation, scale, rotation, and stroke-order variations declared by each glyph.
- Ambiguous drawings require confirmation and never silently choose a low-confidence result.
- Drawing neatness does not alter normalized effect power.
- Undo removes one authoritative vector stroke and updates recognition.
- Closing and reopening a private MapGUI session preserves the draft.

### End-to-end equivalence example

The Scribe source:

```text
when wearer hits target:
    deal 4 fire to enemies within 3 of target
cooldown 8s
```

and the Glyphcraft composition `Blade → Flame contained by Area(radius=3), Clock(8s)` must produce identical canonical graphs, calculated costs, cooldown behavior, targeting, and damage results.

## Future Extensions

These remain separate design work:

- Shared wall canvases for teaching or collaborative rituals.
- Trading signed source pages or glyph drafts.
- Additional triggers and effect schools.
- Cross-specialization composition beyond shared effect knowledge.
- Visual cosmetic grading that does not affect mechanics.
