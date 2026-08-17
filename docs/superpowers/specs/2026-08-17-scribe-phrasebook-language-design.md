# Scribe Phrasebook Language — Design Spec

> Status: approved for implementation planning
> Date: 2026-08-17
> Supersedes (surface grammar only): `docs/superpowers/specs/2026-08-14-scribe-language-expansion-design.md`
> EnchantGraph, cost engine, and Glyphcraft equivalence specs remain in force. This document replaces the *textual surface* those specs assumed.

## Goal

Replace the v0 `spell NAME { … }` opcode language with a **closed phrasebook**: significant lines of ordinary words that allude to code (verb first, indent, implicit defaults) without being code (no braces, dotted paths, comparison operators, or arbitrary English).

A page is a **spell** the player casts from a Scribe book. The same words will later bind as item enchantments (`when hit` / `when struck` / `when used`). That binding is reserved, not in this drop.

The compiler remains Java in-process, Bukkit-free. Paper executes only a complete `CompileResult.Ok`. Paper 26.2 is the authoring surface: a real multiline dialog supplies the source string, including newlines and leading spaces.

## Locked decisions

| Decision | Choice |
|---|---|
| What a page is | A hand-cast spell. Enchantment triggers come later, same words. |
| Voice | Closed phrasebook. Not templates, not `wearer.health < 30%`, not free English. |
| Line model | Newlines are significant. Verb first. Implicit location is the caster. `at` overrides place. Indent attaches a child to the line above. |
| Wrapper | No `spell NAME { }`. A page is only action lines. |
| First lexicon | The v1 phrasebook in this spec. New power is a new word, not a newly exposed Paper method. |
| Compiled form | A typed **action tape**, not EnchantGraph (yet) and not more `Operation` variants. |
| Old grammar | Retired. Existing fixtures and books using `spell { }` will not compile. |
| Editor | Paper 26.2 `Dialog` + multiline `TextDialogInput`. Chat `/scribe begin` is fallback only. |
| Paper version | Stay on `paper-api:26.2`. Do not downgrade to 1.21.8; 26.2 already exposes Dialog. |

## Intent

Scribe should feel like writing a short rite you can learn from one page in the starter book:

```text
look ahead 16
summon sheep
    riding rocket
send skyward
rest 8 seconds
```

A player who knows the word list can read any legal page. A player who does not know the word list cannot guess Paper APIs into existence.

## Boundaries

### In scope

- Line-significant lexer/parser/validator/canonicalizer for the phrasebook.
- Typed action tape on `CompileResult.Ok`.
- Paper 26.2 multiline book dialog that feeds the compiler the raw field text.
- Runtime that executes the action tape through a **curated** Paper map.
- Conformance fixtures for the new language. The v0 `spell { }` corpus is replaced, not extended in place.

### Out of scope / non-goals

- Arbitrary natural language, synonyms, or an intent parser.
- Articles (`a`, `the`) and punctuation-as-syntax (`:`, `{`, `<`, `.` paths).
- `when` / `if` / charges / shield / area / allies / branching (reserved words or later dictionary entries).
- EnchantGraph construction in this drop (a later mapper from the action tape).
- Restoring the removed Rust reference compiler. Java owns this language. Dual-compiler identity is not a requirement until a reference compiler exists again.
- Exposing the full Paper entity, block, potion, or explosion catalogs.
- Command execution, world edit, inventory mutation, weather/time lock, wither/ender-dragon summon.

## Phrasebook (v1 closed lexicon)

Unknown words are compile errors. The list is the language.

### Verbs

| Word | Role | Shape |
|---|---|---|
| `look` | page setting | `look ahead NUMBER` |
| `summon` | effect | `summon NOUN` optional `at PLACE` |
| `burn` | effect | `burn PATIENT` optional `NUMBER` |
| `mend` | effect | `mend PATIENT` optional `NUMBER` |
| `shove` | effect | `shove PATIENT` optional `NUMBER` |
| `strike` | effect | `strike` `at PLACE` **or** `strike PATIENT` |
| `send` | effect | `send skyward` |
| `vanish` | effect | `vanish PATIENT` `for NUMBER seconds` |
| `rest` | limit | `rest NUMBER seconds` |

### Nouns (summonable)

| Word | Paper |
|---|---|
| `sheep` | `SHEEP` |
| `rocket` | `FIREWORK_ROCKET` |
| `fangs` | `EVOKER_FANGS` |

### Patients and places

| Word | Meaning |
|---|---|
| `self` | the caster |
| `target` | living entity under the crosshair, range from `look ahead` or default 32 |
| `caster` | the caster's location (also the implicit `at`) |
| `ahead` | a point `NUMBER` blocks along the look vector; only legal as `ahead NUMBER` |

### Glue

| Word | Meaning |
|---|---|
| `at` | following location for the current verb |
| `ahead` | location or `look` argument |
| `riding` | indented modifier: the parent entity rides this noun |
| `skyward` | required argument of `send` |
| `for` | required before a `vanish` duration |
| `seconds` | required unit after `rest` and `vanish` durations |

Not in the lexicon: `a`, `the`, `upon`, `call`, `forth`, `when`, `if`, `spell`, `damage`, `heal`, `push`, `cooldown`, `fire`, `frost`, `arcane`, `physical`.

`riding` is the only composition word. `upon` is not accepted.

## Line grammar

```text
page         := (blank | line)* EOF
line         := INDENT verbPhrase
verbPhrase   := lookLine | summonLine | burnLine | mendLine | shoveLine
              | strikeLine | sendLine | vanishLine | restLine
lookLine     := "look" "ahead" NUMBER
summonLine   := "summon" NOUN ("at" place)?
burnLine     := "burn" patient NUMBER?
mendLine     := "mend" patient NUMBER?
shoveLine    := "shove" patient NUMBER?
strikeLine   := "strike" "at" place | "strike" patient
sendLine     := "send" "skyward"
vanishLine   := "vanish" patient "for" NUMBER "seconds"
restLine     := "rest" NUMBER "seconds"
patient      := "self" | "target"
place        := "caster" | "self" | "target" | "ahead" NUMBER
NOUN         := "sheep" | "rocket" | "fangs"
```

Whitespace rules:

- Newlines end a statement. There is no `;`.
- Blank lines are ignored and do not change indent.
- Indent is exactly **4 spaces** per level. Tabs are a diagnostic. Mixed widths are a diagnostic.
- A line's indent must be 0, or exactly one level deeper than some ancestor, or equal to a sibling. Dedent to a width that no open line used is a diagnostic (Python-style).
- Maximum indent depth is **1** in this drop. `riding` is the only legal child. A `when` body is reserved and rejected if written.
- Words are ASCII letters. Numbers are finite decimal IEEE-754. Case is lowercase only; uppercase is a diagnostic.
- Comments are out of scope.

`look` and `rest` must be indent 0. `riding` must be indent 1 under a `summon`. `send skyward` must be indent 0.

## Semantics

### Implicit location

Every effect that needs a place and has no `at` uses the caster's location. `summon sheep` appears at the caster's feet, not along the look vector.

### `look ahead`

At most one `look ahead N` per page. It sets the ray range used to resolve `target` (entity) and `ahead N` already carries its own number. Legal range: `1..32`. Default when omitted: `32`.

### Patients vs places

- `burn target` — `target` is **who** is burned. Location is irrelevant.
- `summon sheep at target` — `target` is **where** (the target's location). If there is no looked-at living entity at cast time, the cast fails atomically and does nothing.
- `strike target` — lightning at the target's location.
- `strike at ahead 8` — lightning at a point 8 blocks along the look vector.
- `strike` with neither patient nor `at` is a compile error (striking the caster by accident is not implicit).

### `riding`

```text
summon sheep
    riding rocket
```

Means: spawn a rocket at the summon place, spawn a sheep at the same place, `rocket.addPassenger(sheep)`. The rocket is the vehicle. The sheep is the passenger.

`riding` is only legal under `summon`. The child noun is from the summonable list. `summon rocket` / `riding sheep` is legal and means the opposite stack.

A `summon` may have at most one `riding` child.

### `send skyward`

Applies to the **last `summon` on the page** (the vehicle if that summon has `riding`, otherwise the summoned entity). Launch velocity is `(0, 1.5, 0)`. No prior `summon` → compile error.

### Magnitudes

| Verb | Number | Default if omitted | Bounds |
|---|---|---|---|
| `look ahead` | range | 32 if the line is absent | `1..32` |
| `burn` | damage | `1` | `0.5..20` |
| `mend` | healing | `1` | `0.5..20` |
| `shove` | strength | `1` | `0.1..3` |
| `vanish for` | seconds | required | `0.5..20` |
| `rest` | seconds | required; at most one `rest` | `0..60` |

Non-finite numbers are compile errors.

### Cast-time target miss

Missing `target` when a line needs one: no effect, no cooldown, no partial summons. Same atomic failure as today's preflight.

## Caps

Unchanged ceilings, reinterpreted for lines:

- Source ≤ 4,096 Unicode scalars and ≤ 16,384 UTF-8 bytes.
- ≤ 128 physical lines (dialog `MultilineOptions.maxLines`).
- ≤ 16 non-blank lines.
- ≥ 1 and ≤ 4 effects (`summon`, `burn`, `mend`, `shove`, `strike`, `send`, `vanish`). `look` and `rest` are not effects. `riding` is not an effect. A page that is only `look` / `rest` is `Error`.
- ≤ 32 diagnostics, sorted by UTF-8 start byte, code, message; end-exclusive UTF-8 spans; one-based scalar line/column.
- Rejected compilation is atomic: diagnostics only.

## Compiled form

`CompileResult` stays the sealed `Ok` / `Error` type. `Ok` holds a `CompiledSpell`.

`CompiledSpell.operations` is replaced by a list of **actions** — a new sealed interface, not more `Operation` variants:

```text
Action
  LookAhead(range)
  Summon(noun, place, riding?)
  Burn(patient, amount)
  Mend(patient, amount)
  Shove(patient, amount)
  Strike(place)
  SendSkyward
  Vanish(patient, seconds)
  Rest(seconds)
```

`place` is `Caster`, `Self`, `Target`, or `Ahead(range)`. `patient` is `Self` or `Target`. `riding` is an optional summonable noun.

Canonical bytes (version `scribe-compiler/0.2`) are lowercase opcode lines with IEEE-754 hex bits for every number, LF separators, no trailing newline. Identity is lowercase SHA-256 of those bytes. Equivalent spacing (extra blank lines, indent-legal formatting) shares an identity; changing word, order, or a number changes it.

There is no spell name in the source. Identity does not include a name. `CompiledSpell.name` is removed in this drop. The book item's display name is cosmetic and not hashed.

`Operation` (TargetRay / Damage / Heal / Push / Cooldown) is deleted with the v0 parser. New code uses `Action` only.

## Authoring surface

Right-clicking a marked Scribe book calls `player.showDialog` with a dynamic `Dialog.create`:

- `DialogInput.text("source", …)` with `multiline(128, 512)`, `width(1024)`, `maxLength(4096)`, `initial` = saved PDC source.
- `DialogType.multiAction` buttons: Save, Save & Cast, Cancel.
- Each button is a `DialogAction.customClick` callback, one-use, player-bound, exact-book-bound, 15-minute lifetime (`ClickCallback.Options`).
- The callback reads `DialogResponseView.getText("source")`. That string is the program. The compiler splits on `\n`; leading spaces are indent.

Save persists source without compiling. Save & Cast persists, compiles, and casts only on `Ok`. `Error` reopens the same dialog with diagnostics in the body and does not mutate cooldown or the world. Cancel / Escape persist nothing.

`/scribe cast` still compiles the held book's saved source. `/scribe begin` may keep the chat fallback for environments where Dialog cannot open; it is not the designed editor.

Vanilla book pages remain unused.

## Runtime

Paper maps each action through a closed table. No reflection, no `EntityType.valueOf` on player text.

| Action | Paper |
|---|---|
| `LookAhead` | stored range for this cast |
| `Summon` | `World.spawn` at resolved place; if `riding`, spawn vehicle then passenger, `vehicle.addPassenger(passenger)` |
| `Burn` | `damage` + fire ticks (`amount * 20`) |
| `Mend` | add health, clamp to max |
| `Shove` | bounded knockback away from caster |
| `Strike` | `World.strikeLightning` at resolved place. Real lightning. Place never defaults to caster. |
| `SendSkyward` | `setVelocity` +Y on the last summon vehicle |
| `Vanish` | `setInvisible(true)` for duration, then clear |
| `Rest` | in-memory cooldown keyed by player UUID + identity, as today |

Summoned entities are tagged with plugin PDC so they can be cleaned up later; v1 does not despawn them automatically except the rocket's vanilla explosion.

`Strike` and explosions are the grief edge. v1 allows `strike` only at an explicit place or patient. It does not add `burst`.

## Error handling

- Unknown word, bad indent, illegal child, missing required glue (`seconds`, `skyward`, `for`), extra tokens, uppercase → diagnostics, `CompileResult.Error`.
- Two `look`, two `rest`, `send` without a prior `summon`, `riding` under a non-summon, indent depth > 1 → diagnostics.
- Numeric bounds and non-finite values → diagnostics.
- Dialog over 128 lines / 4096 scalars / 16 KiB → rejected before compile (existing editor limits).
- Cast-time missing target or caster invalid → no world mutation, no cooldown.
- Persistence failure → item unchanged, dialog retained.

## Testing

Automated tests must prove:

- Each verb phrase in the grammar accepts a legal line and rejects one-token and one-bound mutations.
- Indent: 4-space `riding` accepted; tab, 2-space, depth 2, and orphan `riding` rejected.
- `summon sheep` / `riding rocket` compiles to vehicle=`rocket`, passenger=`sheep`.
- `send skyward` without a prior summon is `Error`.
- Implicit `at` is caster; `at target` / `at ahead N` / `at self` encode distinct places.
- `strike` without place or patient is `Error`.
- Canonical identity is stable under extra blank lines and unstable under word or number changes.
- Atomic rejection: `Error` has diagnostics only.
- Dialog builder uses multiline options 128 / 512 and reads `getText("source")` (unit-level; no live client required).
- Runtime tests (mocked Bukkit types in `paper`) for spawn+addPassenger, lightning place, vanish duration, cooldown, and target-miss atomicity.
- The retired `spell ember { … }` source is `Error` (unknown word `spell` or unexpected `{`).

## Architecture

```text
Dialog getText("source")
  → ScribeCompiler.compile
      lexer (line + indent + words + numbers)
      parser (verb phrases + one-level children)
      validate (caps, uniqueness, send/summon pairing)
      canonicalize + SHA-256
  → CompileResult.Ok(CompiledSpell(actions)) | Error(diagnostics)
  → SpellRuntime.cast (Paper table)
```

Units:

| Unit | Owns | Depends on |
|---|---|---|
| Phrasebook lexer/parser | words, indent, diagnostics | `api.dsl` types only |
| Action tape + canonical | `CompiledSpell` / `Action` | no Bukkit |
| Dialog adapter | show / callback / getText | Paper Dialog, book store |
| SpellRuntime | Paper table | `Action`, Bukkit |

Do not put Bukkit types in `api` or `common`. Do not parse source in Paper. Do not execute source text.

## Relationship to other specs

- **EnchantGraph** still becomes the long-term runtime input. This drop produces an action tape. A later `from_actions` mapper will project it to a single implicit `ON_USE` graph with no conditions.
- **2026-08-14 language expansion** described English-shaped code (`when wearer.health < 30%:`). That surface is not what we are building. Keep the expansion spec on disk as history; do not implement it.
- **Living spec** current item “Scribe language expansion” means this phrasebook, not the 2026-08-14 grammar.
- **`docs/scribe-language.md`** is rewritten when this ships; until then it documents the retired v0 language.

## Worked pages

Shepherd's flare:

```text
summon sheep
    riding rocket
send skyward
rest 8 seconds
```

Kindling look:

```text
look ahead 16
burn target 4
rest 3 seconds
```

Judgment:

```text
strike target
shove target
rest 12 seconds
```

Coward's breath (self only, book-cast):

```text
vanish self for 3 seconds
rest 12 seconds
```

## Open questions (none blocking)

- Automatic despawn / firework lifetime for summons — leave vanilla until a later word (`banish`, `for N seconds` on summon) exists.
- Whether `strike` should be visual-only on some servers — not in v1.
- Starter-book phrasebook cheat sheet text — write when implementing the book store copy.
