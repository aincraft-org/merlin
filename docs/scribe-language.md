# Scribe Language

Scribe is a deliberately bounded spell DSL. Java 21 is the in-process compiler used by Paper. Java compiler packages contain no Bukkit dependency.

## Grammar

```text
program        := "spell" IDENTIFIER "{" statement* "}" EOF
statement      := targetStmt | damageStmt | healStmt | pushStmt | cooldownStmt
targetStmt     := "target" "ray" NUMBER ";"
damageStmt     := "damage" "target" DAMAGE_TYPE NUMBER ";"
healStmt       := "heal" "self" NUMBER ";"
pushStmt       := "push" "target" NUMBER ";"
cooldownStmt   := "cooldown" NUMBER "s" ";"
DAMAGE_TYPE    := "physical" | "fire" | "frost" | "arcane"
```

Whitespace is insignificant. Identifiers use ASCII letters, digits, and `_`, and cannot begin with a digit.

## Bounds

- Source: at most 4,096 Unicode scalar values and 16,384 UTF-8 bytes, inclusive.
- Program: at most 16 statements and four effects.
- Ray range: `1..32`.
- Damage/healing: `0.5..20`.
- Push strength: `0.1..3`.
- Cooldown: `0..60` seconds.
- Diagnostics: at most 32, sorted by UTF-8 start byte, code, and message.

Rejected compilation is atomic: it contains diagnostics only. It never exposes partial operations, canonical bytes, or an identity.

## Canonical identity

Accepted programs preserve statement order. Canonical UTF-8 contains lowercase operation names and finite IEEE-754 values as exactly 16 lowercase hexadecimal bits, separated by LF with no trailing newline. The identity is lowercase SHA-256 of those bytes. Equivalent formatting has the same identity; changing semantics or order changes it.

## Paper authoring

`/scribe book` creates a marked writable Scribe book. Source is stored in plugin-owned persistent data, not vanilla pages. Right-clicking the marked main-hand book starts the Scribe chat editor; `/scribe begin` starts it explicitly. Send the complete source as the next chat message, then use `/scribe save`, `/scribe cast`, or `/scribe cancel`.

- Save persists source without casting.
- Save & Cast persists the submitted draft, compiles it, and casts only on success.
- Invalid source is reopened with diagnostics and causes no effect or cooldown mutation.
- Cancel and Escape do not modify saved source.
- Callbacks are one-use, player-bound, exact-book-bound, and expire after 15 minutes.
- Ordinary books retain vanilla behavior.

The editor uses the documented 4,096-scalar, 16,384-byte, and 128-line limits. A future Paper target that exposes the Dialog API may provide a native dialog implementation behind the same editor boundary; the current target intentionally uses the chat fallback because its API does not expose those classes.

`/scribe cast` is also the direct command path for compiling a held marked book. Permission: `wizardry.scribe.book`.
