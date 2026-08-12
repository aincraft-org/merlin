# Scribe Dual Compiler and Dialog IDE Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Each task uses TDD and receives a task-scoped review before the next task.

**Goal:** Keep Rust as the authoritative Scribe language implementation, add a conformant Java 21 compiler for Paper runtime use, and expose it through a large Paper 1.21.8 dialog IDE attached to marked Scribe books.

**Architecture:** Rust owns the language specification and generates a versioned golden corpus. Java independently implements the same lexer, parser, validator, canonicalizer, and typed output, with no Bukkit dependencies. Paper invokes only Java, and executes a spell only after Java passes the complete Rust corpus.

**Tech Stack:** Rust 2021 edition, Cargo, Java 21, Gradle Kotlin DSL, Paper API `1.21.8-R0.1-SNAPSHOT`, JUnit Jupiter 5.11.4, Jackson 2.18.x for test fixture decoding only.

## Global Constraints

- Rust is the semantic reference and runs during development and CI only.
- Java is the production compiler used by Paper; there is no JNI, native loader, sidecar, or per-cast process.
- Compiler packages contain no Bukkit types.
- Source limits are 4,096 Unicode scalar values and 16,384 UTF-8 bytes, both inclusive.
- Programs contain at most 16 statements and four effects.
- Diagnostics are capped at 32 and sorted by UTF-8 start byte, code, and message.
- Diagnostics use stable codes, end-exclusive UTF-8 byte spans, and one-based Unicode-scalar line and column.
- Compilation is atomic: rejected results contain diagnostics only; successful results contain complete operations, canonical bytes, and identity.
- Canonical bytes and lowercase SHA-256 identity must match across Rust and Java.
- Initial effects are damage, heal, and push. Initial targeting is ray targeting. Initial limits are cooldowns.
- No loops, recursion, variables, arbitrary commands, filesystem/network access, user events, or general-purpose execution.
- Paper dialog input is 1,024 pixels wide, 512 pixels high, at most 128 lines and 4,096 Unicode scalar values.

## Repository Layout

```text
Cargo.toml
Cargo.lock
src/                                  # authoritative Rust compiler
conformance/
  schema-v1.json
  fixtures/*.json                     # generated and committed Rust oracle
java-compiler/
  build.gradle.kts
  src/main/java/dev/jlo/wizardry/scribe/
  src/test/java/dev/jlo/wizardry/scribe/
paper/
  build.gradle.kts
  src/main/java/dev/jlo/wizardry/paper/
  src/main/resources/paper-plugin.yml
settings.gradle.kts
build.gradle.kts
```

## Shared DSL

```text
program        := "spell" IDENTIFIER "{" statement* "}" EOF ;
statement      := targetStmt | damageStmt | healStmt | pushStmt | cooldownStmt ;
targetStmt     := "target" "ray" NUMBER ";" ;
damageStmt     := "damage" "target" DAMAGE_TYPE NUMBER ";" ;
healStmt       := "heal" "self" NUMBER ";" ;
pushStmt       := "push" "target" NUMBER ";" ;
cooldownStmt   := "cooldown" NUMBER "s" ";" ;
DAMAGE_TYPE    := "physical" | "fire" | "frost" | "arcane" ;
```

Bounds are inclusive: ray `1..32`, damage and healing `0.5..20`, push `0.1..3`, cooldown `0..60` seconds.

---

### Task 1: Stabilize the Rust Reference Compiler

**Files:**
- Modify: `Cargo.toml`
- Modify: `src/lexer.rs`
- Modify: `src/parser.rs`
- Modify: `src/validate.rs`
- Modify: `src/canonical.rs`
- Modify: `src/diagnostic.rs`
- Modify: `src/lib.rs`

**Produces:** `scribe_compiler::compile(&str) -> Result<CompiledSpell, CompileFailure>` as the authoritative behavior.

- [ ] Add failing tests for Unicode scalar consumption, 4,096-scalar acceptance, 4,097-scalar rejection, 16-KiB UTF-8 acceptance/rejection, 16/17 statements, four/five effects, all numeric boundaries, malformed decimals, and 32-diagnostic truncation.
- [ ] Run each new focused test and confirm it fails for the intended missing behavior.
- [ ] Fix lexer iteration with `source[pos..].chars().next()` and maintain end-exclusive UTF-8 spans.
- [ ] Make every parser and validator diagnostic source-aware before sorting.
- [ ] Enforce limits without panic, overflow, partial output, or spans inside a Unicode scalar.
- [ ] Replace implementation-dependent debug canonicalization with a frozen UTF-8 grammar using lowercase operation names, `Double`-equivalent finite decimal normalization, LF separators, and no trailing newline.
- [ ] Run `cargo fmt --all --check && cargo test -- --nocapture && cargo clippy --all-targets --all-features -- -D warnings`.
- [ ] Commit: `feat: stabilize Rust Scribe compiler contract`.

---

### Task 2: Generate the Rust Golden Conformance Corpus

**Files:**
- Create: `conformance/schema-v1.json`
- Create: `conformance/cases/*.source`
- Create: `conformance/fixtures/*.json`
- Create: `src/bin/generate_conformance.rs`
- Create: `tests/conformance.rs`

**Fixture contract:** each fixture contains `schemaVersion`, unique `id`, exact UTF-8 `source`, and `result`.
Accepted results contain `status`, `name`, full `canonicalHex`, lowercase 64-character
`identitySha256`, and the ordered typed `operations`. Rejected results contain
`status: "rejected"` and ordered diagnostics with `code`, `message`, `startByte`,
`endByte`, `line`, and `column`; they contain no semantic output.

- [ ] Write a failing corpus round-trip test that requires every `.source` file to have exactly one deterministic fixture.
- [ ] Implement the generator using only `scribe_compiler::compile`; it must never duplicate compiler rules.
- [ ] Cover every grammar production, damage type, exact boundary, one-beyond boundary, equivalent formatting, CRLF/LF, `é`, `中`, combining marks, astral emoji, EOF diagnostics, multiple diagnostics, 16/17 statements, four/five effects, and scalar/byte source limits.
- [ ] Make `cargo run --bin generate_conformance -- --check` fail when committed fixtures differ.
- [ ] Run Rust format, tests, clippy, and generator check.
- [ ] Commit: `test: add Rust Scribe conformance corpus`.

---

### Task 3: Initialize the Java Compiler Module and Model

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `java-compiler/build.gradle.kts`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/CompilerConstants.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/Span.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/Diagnostic.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/ast/*.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/CompiledSpell.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/CompileResult.java`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/scribe/ModelTest.java`

**Produces:** immutable Java records and sealed interfaces matching Rust types exactly.

- [ ] Configure Java 21, JUnit 5.11.4, and Jackson for tests only.
- [ ] Write failing model invariants: defensive operation copies, rejected results cannot contain semantic output, accepted results cannot contain diagnostics, and spans require ordered non-negative UTF-8 offsets plus positive line/column.
- [ ] Implement immutable records/sealed interfaces for target ray, damage, heal, push, cooldown, damage type, diagnostics, compiled spell, and result.
- [ ] Run `./gradlew :java-compiler:test` and commit `build: initialize Java Scribe compiler model`.

---

### Task 4: Implement the Java Lexer and Parser

**Files:**
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/lexer/*.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/parser/*.java`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/scribe/LexerTest.java`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/scribe/ParserTest.java`

**Produces:** `Parser.parse(String source)` returning typed syntax or ordered diagnostics.

- [ ] Write failing tests from Rust fixtures for every token, production, malformed number, unknown scalar, missing semicolon, duplicate target/cooldown, Unicode span, and recovery to the next semicolon/right brace.
- [ ] Track source positions as UTF-8 byte offsets while advancing Java code points, never UTF-16 indices.
- [ ] Implement the recursive-descent grammar exactly; no extra syntax or implicit defaults.
- [ ] Sort and cap diagnostics only after assigning positions from the original source.
- [ ] Run lexer/parser tests and commit `feat: parse Scribe spells in Java`.

---

### Task 5: Implement Java Validation and Canonicalization

**Files:**
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/validate/*.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/canonical/*.java`
- Create: `java-compiler/src/main/java/dev/jlo/wizardry/scribe/ScribeCompiler.java`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/scribe/CompilerTest.java`
- Create: `java-compiler/src/test/java/dev/jlo/wizardry/scribe/ConformanceTest.java`

**Produces:** `ScribeCompiler.compile(String): CompileResult`.

- [ ] Write failing tests for all Rust validator fixtures, atomic rejection, source limits, statement/effect limits, canonical bytes, and SHA-256.
- [ ] Use locale-independent deterministic numeric canonicalization matching Rust golden bytes; reject non-finite values before canonicalization.
- [ ] Load every `conformance/fixtures/*.json` and compare exact accepted output or exact ordered diagnostics.
- [ ] Fail the build on missing, duplicate, skipped, or unknown-schema fixtures.
- [ ] Run `cargo run --bin generate_conformance -- --check && ./gradlew :java-compiler:test`.
- [ ] Commit: `feat: add conformant Java Scribe compiler`.

---

### Task 6: Initialize the Paper Plugin and Scribe Book Store

**Files:**
- Create: `paper/build.gradle.kts`
- Create: `paper/src/main/resources/paper-plugin.yml`
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/WizardryPlugin.java`
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/book/ScribeBookStore.java`
- Create: `paper/src/test/java/dev/jlo/wizardry/paper/book/ScribeBookStoreTest.java`

- [ ] Depend on `:java-compiler` and Paper API `1.21.8-R0.1-SNAPSHOT`; do not depend on Rust artifacts.
- [ ] Test marked `WRITABLE_BOOK` creation with PDC marker, random book UUID, starter source, and exact-identity save protection.
- [ ] Store source in PDC, never vanilla book pages; reject values outside compiler source limits.
- [ ] Add `/scribe book` creation wiring and permission `wizardry.scribe.book`.
- [ ] Run Paper module tests and jar build; commit `feat: add persistent Scribe books`.

---

### Task 7: Implement the Paper 1.21.8 Dialog IDE

**Files:**
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/dialog/ScribeDialog.java`
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/listener/ScribeBookListener.java`
- Create: `paper/src/test/java/dev/jlo/wizardry/paper/dialog/ScribeDialogStateTest.java`

- [ ] Build a dynamic dialog with a 1,024-pixel-wide input, 512-pixel multiline height, 128 lines, persisted initial source, and Save, Save & Cast, and Cancel actions.
- [ ] Bind each one-use callback to player UUID and exact book UUID with a maximum 15-minute lifetime.
- [ ] Save persists without compiling. Save & Cast persists then compiles. Invalid compilation reopens identical source with diagnostics and performs no cast/cooldown mutation. Cancel and Escape persist nothing.
- [ ] Intercept only main-hand right-clicks on marked Scribe books; ordinary books retain vanilla behavior.
- [ ] Run dialog state tests and compile against Paper 1.21.8; commit `feat: add Scribe book dialog IDE`.

---

### Task 8: Implement Paper Spell Runtime and Commands

**Files:**
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/runtime/*.java`
- Create: `paper/src/main/java/dev/jlo/wizardry/paper/command/ScribeCommand.java`
- Modify: `paper/src/main/java/dev/jlo/wizardry/paper/WizardryPlugin.java`
- Create: `paper/src/test/java/dev/jlo/wizardry/paper/runtime/*.java`

- [ ] Preflight target and cooldown before applying any operation.
- [ ] Execute operations in source order: attributed damage, fire ticks, frost slowness, arcane damage, bounded healing, and push.
- [ ] Key cooldown by player UUID and canonical identity.
- [ ] `/scribe cast` compiles and casts the held marked book; diagnostics and cooldown failures are player-visible and atomic.
- [ ] Run runtime tests and Paper jar build; commit `feat: cast Java-compiled Scribe spells`.

---

### Task 9: Documentation and End-to-End Verification

**Files:**
- Create: `docs/scribe-language.md`
- Create: `.github/workflows/ci.yml`

- [ ] Document exact grammar, bounds, diagnostics, canonical identity, Rust-reference/Java-runtime authority, dialog behavior, permissions, and examples.
- [ ] CI runs Rust format/test/clippy, corpus regeneration check, Java compiler tests, and Paper build on Java 21.
- [ ] Run locally:

```text
cargo fmt --all --check
cargo test -- --nocapture
cargo clippy --all-targets --all-features -- -D warnings
cargo run --bin generate_conformance -- --check
./gradlew clean test
./gradlew :paper:jar
```

- [ ] Smoke on Paper 1.21.8: create a Scribe book, right-click into the large dialog, save/reopen identical source, cast all three examples, verify invalid source reopens with diagnostics and no effects, verify exact-book callback protection, and verify ordinary books remain vanilla.
- [ ] Commit `docs: document Scribe compiler and IDE`.
