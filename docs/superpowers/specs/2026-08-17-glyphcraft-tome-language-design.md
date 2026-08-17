# Glyphcraft Tome Language — Design Spec

> Status: approved for implementation planning
> Date: 2026-08-17
> Supersedes (composition surface only): `docs/superpowers/specs/2026-08-14-glyphcraft-composition-design.md`
> EnchantGraph, cost engine, and Scribe↔Glyphcraft equivalence specs remain in force as the long-term convergence. This document replaces the *authoring surface* those specs assumed (many raw glyphs on one canvas, construction-matching, spatial parse).

## Goal

Give Glyphcraft a **compound language** that is mechanically distinct from the Scribe phrasebook:

- A **glyph is a map item**. One drawing, one classified label, optional pips, a printed mana cost.
- A **tome is a book of those maps**. Flip pages to look. The whole book is the spell.
- Types have different grammars. Missing pieces fill from implicits. Rank is overlay chrome, not ink.

Java in `api`/`common` owns the language (no Bukkit). Paper owns map items, the MapGUI pen, tome binding, and cast through the existing `Action` tape.

## Locked decisions

| Decision | Choice |
|---|---|
| What a glyph is | A map item: ink + frozen label + pips + mana |
| What a tome is | An ordered book of those maps (pages) |
| One map | One glyph. The classifier still sees one crop |
| Composition | The bag of pages, typed. Not shared pixels |
| Page order | How you flip. Does not change meaning in v1 |
| Rank / pips | Overlay clicker on the pen. Belongs to **that** glyph |
| Implicit patient | `target` |
| Implicit school | none / plain |
| Implicit pips | 1 (I) |
| Magnitude | pips `1..5` → amount `1..5` |
| Mana | Printed on the glyph; tome mana is the sum |
| Compile target | Action tape for combat terms (`Burn` / `Mend` / `Shove`). Not EnchantGraph |
| Scribe relationship | Overlapping envelopes, not a twin lexicon |
| Multi-glyph on one map | Out of scope |
| Construction matching | Out of scope (types, not recipes) |

## Intent

A battlemage writes **stones** (maps) and binds them into a **tome**. A recluse writes **words** into a Scribe book. Both can say “burn the thing I am looking at.” Only the tome is `fire` ⊕ `damage ●●●●●`. Only the Scribe page is `summon sheep` / `riding rocket`.

## Boundaries

### In scope

- Role table over the existing 24 `Label`s.
- `GlyphToken` (label + pips) and `ManaTable` (data).
- `GlyphCompiler`: pages → `CompileResult` (combat) or charm bind data.
- Persist token fields on the existing glyph map PDC.
- MapGUI pip clicker (`◀ ●●●●● ▶`) on a layer above the canvas.
- Tome item: add page, tear page, flip, cast combat tomes.
- Bind a `sharpness` map to a sword as vanilla Sharpness I–V.

### Out of scope / non-goals

- Several unclassified glyphs on one 128×128 crop.
- Triggers, conditions, `area`, `repeat`, `target-ray`, `attacker`, `cooldown`, `charges` as grammatical words (they classify; they do not compile).
- Fusing two maps into a third map.
- Player mana pool / regen. v1 **prints** mana; spending it is the next Paper slice.
- EnchantGraph construction.
- Digit glyphs, size-is-power, confidence-is-power.
- Making a Scribe book and a tome the same item type.

## Types and grammars

Every `Label` has exactly one role.

| Role | Labels | Hunger | Pips |
|---|---|---|---|
| Effect | `damage`, `heal`, `push`, `shield` | Head of a combat term. Exactly one per tome | Yes — magnitude |
| School | `physical`, `fire`, `frost`, `arcane` | Wants that effect. At most one | No — pips must stay 1 |
| Patient | `self`, `target` | Wants that effect. At most one | No |
| Charm | `sharpness` (v1 list; Java `Label` only — not in the 24-class ONNX catalog yet) | Wants a gear host, not a combat tome | Yes — enchantment rank |
| Aim | `target-ray` | Reserved | — |
| Trigger | `on-hit`, `on-hurt`, `on-use`, `periodic` | Reserved | — |
| Condition | `if-health`, `if-undead`, `if-outdoors` | Reserved | — |
| Scope | `area`, `repeat` | Reserved | — |
| Limit | `cooldown`, `charges` | Reserved | — |
| Bottom | `reject` | Never a word | — |
| Patient (locked) | `attacker` | Reserved until `on-hurt` | — |

`shield` is an Effect in the catalog but has no Action-tape lowering in this drop. Compiling a tome whose effect is `shield` is `Error` (`G0111`) — known, not executable yet.

### Combat grammar (v1)

A combat tome is 1..3 pages. After implicits:

- exactly one Effect from `{damage, heal, push}`
- zero or one School
- zero or one Patient from `{self, target}`
- no Charm
- no reserved label

Illegal: two heads, two schools, two patients, school or patient without an effect, charm mixed in, more than 3 pages, pips other than 1 on a no-pip role.

### Charm grammar (v1)

A charm is **one** map, not a tome. Use it on a sword. Label `sharpness`, pips `1..5` → vanilla Sharpness I–V. A charm map will not bind as a tome page.

### Implicits

Applied only to what the pages left open:

| Omitted | Fills as |
|---|---|
| Patient | `target` |
| School | none (plain). `damage` still lowers to `Burn` |
| Pips on an effect or charm | `1` |
| Magnitude | equal to pips |

`heal` / `shield` do **not** default to `self`. One patient implicit.

## Rank clicker (pen only)

While a map is open in MapGUI:

- Overlay strip above the ink, not in `GlyphDraft`, not classified.
- Display: `◀` + five pips + `▶`. Filled count is the rank. None filled = 1.
- Left: −1, clamp 1. Right: +1, clamp 5.
- Shift+left → 1. Shift+right → 5. If the map click cannot see Shift, sneak is the same modifier (`Player.isSneaking()`).
- Clicker is enabled only when the *pending* label has a pip grammar (effect or charm). After classify, if the label is a school/patient, force pips to 1 and ignore the clicker.
- Confirm / save freezes `(label, pips)` onto the map item. Inventory cannot retune pips.

## Mana (printed)

`ManaTable` v1 (integer, data, not scattered magic numbers):

| Token | Mana |
|---|---|
| Effect, pips *n* | `2 + n` (so I = 3, V = 7) |
| School | `2` |
| Patient | `1` |
| Charm `sharpness`, pips *n* | `2 + 2n` (I = 4, V = 12) |

Tome mana = sum of page manas. Shown on the tome lore. Not spent in this drop.

## Compiled form

`GlyphCompiler.compile(List<GlyphToken>)` returns the same sealed `CompileResult` as Scribe.

Combat `Ok` holds a `CompiledSpell`:

- `compilerVersion`: `glyph-compiler/0.1`
- actions, in this order: optional `LookAhead(32)` when patient is `target`; then `Burn` / `Mend` / `Shove`
- `Burn` / `Mend` amount = pips as `double`. `Shove` amount = `min(3, pips)` (existing shove cap is `0.1..3`)
- `school == fire` does not change the tape in this drop (`Burn` already applies fire ticks). Other schools are accepted as grammar and do not change the tape yet
- identity is SHA-256 of canonical bytes
- page order does not appear in canonical bytes

Canonical UTF-8, LF separators, no trailing newline:

```text
glyph-compiler/0.1
look_ahead|<16 hex bits of 32.0>     # only if patient is target
burn|target|<16 hex bits>            # example
```

Same opcode lines as Scribe for the overlapping actions so a later equivalence suite can compare tapes. The version line is `glyph-compiler/0.1`, so identities are **not** shared with Scribe pages yet (intentional; the equivalence spec stays future work).

Atomic rejection: `Error` has diagnostics only. No tape, no identity.

### Diagnostic codes

| Code | When |
|---|---|
| `G0100` | empty page list |
| `G0101` | `reject` or unknown label |
| `G0102` | reserved label (trigger, condition, aim, scope, limit, `attacker`) |
| `G0103` | more than 3 pages |
| `G0104` | more than one effect |
| `G0105` | more than one school |
| `G0106` | more than one patient |
| `G0107` | school or patient with no effect |
| `G0108` | charm mixed with any other page, or charm in a multi-page compile |
| `G0109` | pips not in `1..5` |
| `G0110` | pips ≠ 1 on a role that has no pip grammar |
| `G0111` | effect `shield` (no tape lowering) |

`Span.line` is the 1-based page index. `startByte`/`endByte` are 0 for token diagnostics.

Charm bind is **not** `compile()` — it is `GlyphCompiler.charm(GlyphToken)` → `Optional<CharmBind>` (`sharpness`, rank) or empty + diagnostics (`G0108` / `G0102` / …).

## Items (Paper)

### Glyph map

Existing marked paper / filled map (`glyph_item`, `glyph_item_id`, `glyph_draft_v1`) plus:

| Key | Type | Meaning |
|---|---|---|
| `glyph_label` | STRING | `Label.id()` after a successful classify+confirm |
| `glyph_pips` | INTEGER | `1..5` |
| `glyph_mana` | INTEGER | printed mana at freeze time |

A map with draft but no label is an **unfinished canvas**. It will not bind to a tome.

Display name / lore: `fire`, or `damage ●●●●●`, plus `mana N`.

### Tome

New item: `Material.BOOK`, marker `glyph_tome`, id `glyph_tome_id`.

Pages stored as a PDC list of records (label, pips, mana, draft bytes, source map id). Binding **copies** the frozen token + draft into the tome; the loose map remains in hand unless the player is sneaking (sneak-bind **consumes** the map).

v1: at most 3 pages. Insert runs the compiler on the *would-be* page list. `Error` → no insert, message is the first diagnostic. `G0107` (unfinished) is allowed while building — a lone `fire` page may sit in the tome; **cast** rejects unfinished tomes.

Tear current page: give the player a new glyph map with that draft+token.

Current page index in PDC. Right-click opens MapGUI on that page’s draft (read-only ink + flip chrome). Left-click menu: previous, next, tear, close. Cast is `/glyph cast` on the held tome.

A tome is never a Scribe book (`scribe_book` marker). Scribe listener ignores it.

### Sword bind

Use a frozen `sharpness` map on a sword. Apply vanilla `SHARPNESS` at level = pips. Consume the map. Non-sword / already higher sharpness → fail, map stays.

## Authoring loop

```text
/glyph book          → blank canvas
right-click / /glyph → MapGUI pen
draw                 → ink only
◀ ▶ / sneak          → pips on this glyph
classify + save      → freeze label + pips + mana onto the map
/glyph tome          → empty book
use map on tome      → add page if grammar allows
/glyph cast          → compile pages, SpellRuntime.cast
use sharpness on sword → vanilla Sharpness
```

Classifier still does not run on every point. Explicit classify, then save, freezes the token.

## Testing

Automated tests must prove:

- Role table covers all 24 labels; `reject` is BOTTOM.
- Token rejects pips outside `1..5`; no-pip roles reject pips ≠ 1.
- Mana table: `damage` pips 1 → 3, pips 5 → 7; `fire` → 2; `sharpness` pips 5 → 12.
- `fire` + `damage` pips 5 → `Burn(TARGET, 5)` + `LookAhead(32)`, mana 9.
- `heal` + `self` → `Mend(SELF, 1)`, no `LookAhead`.
- `push` pips 5 → `Shove(TARGET, 3)` (cap).
- Lone `fire` compiles as `G0107`. Two `damage` → `G0104`. `on-hit` → `G0102`.
- Page order `damage` then `fire` vs the reverse shares identity.
- Extra blank… (N/A). Changing pips changes identity.
- Atomic rejection: `Error` has no spell.
- `shield` → `G0111`.
- Charm `sharpness` pips 3 → bind rank 3; charm + `damage` → `G0108`.
- Pip clicker: left/right/sneak clamps; pips are not in the draft raster.
- Tome insert of a second effect fails; tear restores a map with the same token.
- Cast of a valid tome calls the same `SpellRuntime` path as Scribe.
- Sharpness map on a diamond sword sets vanilla level; hoe fails.

## Architecture

```text
MapGUI pen (ink + pip overlay)
  → classify Label
  → freeze GlyphToken on map PDC
  → bind maps into Tome pages
  → GlyphCompiler.compile(tokens)
      role check, implicits, caps
      lower to Action tape + canonical + SHA-256
  → CompileResult.Ok | Error
  → SpellRuntime.cast
```

| Unit | Owns | Depends on |
|---|---|---|
| `GlyphRoles` / `GlyphToken` / `ManaTable` | types, pips, printed mana | `Label` only |
| `GlyphCompiler` | grammar, implicits, tape, identity | `Action`, `CompileResult` |
| Glyph map PDC | freeze token onto existing canvas | store adapter |
| `GlyphScreen` pip overlay | clicker state | MapGUI `Overlay`, not draft |
| `GlyphTomeStore` | pages, insert/tear/flip | compiler, map store |
| Paper bind / cast | listeners, commands | `SpellRuntime` |

Do not put Bukkit types in `api` or `common`. Do not parse ink in Paper. Do not execute unclassified maps.

## Relationship to other specs

- **Scribe phrasebook** stays the text language. Shared runtime is the Action tape. Identities are not equal in this drop.
- **2026-08-14 Glyphcraft composition** assumed spatial parse into EnchantGraph. Keep it on disk as history. Do not implement it.
- **EnchantGraph / cost engine** remain the long-term priced IR. Printed mana is a v1 stand-in, not a second balance system to keep forever.
- **Living spec** Current item “Glyphcraft semantic composition” means this tome language.

## Worked terms

Kindling (overlap with Scribe `burn target`):

```text
pages: damage          →  burn target 1    mana 3
```

Kindling 4 (overlap with `burn target 4`):

```text
pages: damage ●●●●     →  burn target 4    mana 6
```

Flame:

```text
pages: fire, damage ●●●●●
→  burn target 5    mana 9
```

Self mend:

```text
pages: heal, self
→  mend self 1    mana 4
```

Blade:

```text
map: sharpness ●●●●●
→  Sharpness V on the sword    mana 12
```

## Open questions (none blocking)

- Whether sneak-bind consumes the map — locked as yes if sneaking, copy otherwise.
- Frost/arcane tape differences — parked with the school labels accepted but no-op on the tape.
- Spending printed mana — next Paper slice.
- Second chapter in a tome (sequence of compounds) — Future.
