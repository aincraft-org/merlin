# Scribe Phrasebook

A Scribe page is a short rite of ordinary words. Verb first, one statement per line, no braces, no semicolons, no dotted paths. The word list is the language: unknown words do not compile.

Java 21 is the in-process compiler used by Paper. Compiler packages (`api`, `common`) contain no Bukkit types. Paper executes only a complete `CompileResult.Ok`.

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

- Newlines end a statement. There is no `;` and no `spell NAME { }`.
- Blank lines are ignored and do not change indent.
- Indent is exactly **4 spaces** per level. Tabs are rejected.
- Maximum indent depth is **1**. `riding` is the only legal child.
- `look` and `rest` stay at indent 0. `send skyward` stays at indent 0. `riding` must sit under a `summon`.
- Words are lowercase ASCII. Uppercase is a diagnostic.

## Closed word list

Unknown words are compile errors. New power is a new word, not a newly exposed Paper method.

### Verbs

| Word | Shape |
|---|---|
| `look` | `look ahead NUMBER` |
| `summon` | `summon NOUN` optional `at PLACE` |
| `burn` | `burn PATIENT` optional `NUMBER` |
| `mend` | `mend PATIENT` optional `NUMBER` |
| `shove` | `shove PATIENT` optional `NUMBER` |
| `strike` | `strike at PLACE` or `strike PATIENT` |
| `send` | `send skyward` |
| `vanish` | `vanish PATIENT for NUMBER seconds` |
| `rest` | `rest NUMBER seconds` |

### Nouns

| Word | Meaning |
|---|---|
| `sheep` | a sheep |
| `rocket` | a firework rocket |
| `fangs` | evoker fangs |

### Patients and places

| Word | Meaning |
|---|---|
| `self` | the caster |
| `target` | living entity under the crosshair (`look ahead` range, default 32) |
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

Not in the lexicon: `a`, `the`, `upon`, `call`, `forth`, `when`, `if`, `spell`, `damage`, `heal`, `push`, `cooldown`, `flame`, `frost`, `arcane`, `physical`.

## Implicit location

Every effect that needs a place and has no `at` uses the caster. `summon sheep` appears at the caster's feet, not along the look vector.

`look ahead N` may appear at most once. It sets the ray used to resolve `target`. Default when omitted: `32`.

- `burn target` — `target` is **who** is burned.
- `summon sheep at target` — `target` is **where**. No living entity under the crosshair at cast time: the cast fails atomically and does nothing.
- `strike target` — lightning at the target's location.
- `strike at ahead 8` — lightning 8 blocks along the look vector.
- `strike` with neither patient nor `at` is a compile error.

## Riding

```text
summon sheep
    riding rocket
```

Spawn a rocket at the summon place, spawn a sheep at the same place, and put the sheep on the rocket. The rocket is the vehicle. The child noun is from the summonable list. A `summon` may have at most one `riding` child.

`send skyward` applies to the last `summon` on the page (the vehicle if that summon has `riding`). No prior `summon` is a compile error.

## Bounds

- Source: at most 4,096 Unicode scalars and 16,384 UTF-8 bytes (16 KiB), inclusive.
- At most 16 non-blank lines and four effects (`summon`, `burn`, `mend`, `shove`, `strike`, `send`, `vanish`). `look` and `rest` are not effects. `riding` is not an effect.
- `look ahead`: `1..32`.
- `burn` / `mend`: `0.5..20` (default `1` if the number is omitted).
- `shove`: `0.1..3` (default `1`).
- `vanish`: `0.5..20` seconds (required).
- `rest`: `0..60` seconds (required; at most one).
- Diagnostics: at most 32.

A page that is only `look` / `rest` is rejected.

## Canonical identity

Accepted pages compile to version `scribe-compiler/0.2`. Canonical UTF-8 is lowercase opcode lines with IEEE-754 values as exactly 16 lowercase hexadecimal bits, separated by LF with no trailing newline. The identity is the lowercase SHA-256 of those bytes. Extra blank lines and indent-legal formatting share an identity; changing a word, order, or number changes it. There is no spell name in the source or the identity.

## Compile result

`CompileResult` is sealed `Ok` / `Error`. Paper casts only `Ok`. Rejected compilation is atomic: diagnostics only. It never exposes a partial action tape, canonical bytes, or an identity. Invalid source causes no world mutation and no cooldown.

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

Coward's breath:

```text
vanish self for 3 seconds
rest 12 seconds
```

The starter book opens with a shorter kindling:

```text
look ahead 8
burn target
rest 3 seconds
```

## Paper authoring

`/scribe book` creates a marked writable Scribe book. Source is stored in plugin-owned persistent data, not vanilla pages.

Right-click the marked main-hand book to open the Paper 26.2 multiline dialog. Edit the page there. Chat `/scribe begin` is fallback only, for environments where the dialog cannot open.

- Save persists source without compiling.
- Save & Cast persists the submitted draft, compiles it, and casts only on `Ok`.
- Invalid source reopens the same dialog with diagnostics and causes no effect or cooldown mutation.
- Cancel and Escape do not modify saved source.
- Callbacks are one-use, player-bound, exact-book-bound, and expire after 15 minutes.
- Ordinary books retain vanilla behavior.

The editor uses the documented 4,096-scalar, 16,384-byte, and 128-line limits.

`/scribe cast` compiles the held marked book's saved source. Permission: `merlin.scribe.book`.
