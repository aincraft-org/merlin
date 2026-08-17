# Scribe Phrasebook Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the v0 `spell { }` opcode language with the closed phrasebook in `docs/superpowers/specs/2026-08-17-scribe-phrasebook-language-design.md`, compile it to an `Action` tape, execute it on Paper, and take source from a Paper 26.2 multiline dialog.

**Architecture:** Java in `api`/`common` owns the language (no Bukkit). The lexer emits indent-aware lines. The parser builds `Phrase` nodes. The compiler validates, canonicalizes (`scribe-compiler/0.2`), and returns `CompileResult.Ok(CompiledSpell)` / `Error`. Paper maps each `Action` through a closed table and opens a real `Dialog` whose `getText("source")` is the program.

**Tech Stack:** Java 21 (`api`, `common`), Java 25 (`paper`), JUnit 5, Mockito 5.15, Paper API `26.2.build.84-stable`, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-17-scribe-phrasebook-language-design.md`

---

## File map

| File | Responsibility |
|---|---|
| `api/.../dsl/Action.java` | Compiled tape: verbs, `Noun`, `Patient`, `Place` |
| `api/.../dsl/Phrase.java` | AST with spans (replaces `Statement`) |
| `api/.../dsl/Page.java` | Ordered phrases (replaces `Program`) |
| `api/.../dsl/CompiledSpell.java` | version, identity, canonical bytes, `List<Action>` — **no name** |
| `api/.../dsl/CompilerConstants.java` | `scribe-compiler/0.2`, line/effect caps |
| `api/.../dsl/Operation.java` | **Delete** |
| `api/.../dsl/Statement.java` | **Delete** |
| `api/.../dsl/Program.java` | **Delete** |
| `common/.../lexer/Lexer.java` | Line lexer: indent, words, numbers, no `;`/`{}` |
| `common/.../parser/Parser.java` | Phrasebook verb phrases + one-level `riding` |
| `common/.../ScribeCompiler.java` | limits, validate, canonicalize, SHA-256 |
| `common/src/test/resources/conformance/` | Replace v0 fixtures; schema-v2 |
| `paper/.../runtime/SpellRuntime.java` | Closed Paper table over `Action` |
| `paper/.../dialog/ScribeDialog.java` | Build/show Paper Dialog; `getText("source")` |
| `paper/.../book/ScribeBookStore.java` | Phrasebook starter source |
| `paper/.../listener/ScribeBookListener.java` | Open dialog, do not instruct chat-as-editor |
| `paper/.../WizardryPlugin.java` / `ScribeCommand.java` | Resolve `target` from `LookAhead` (default 32) |
| `docs/scribe-language.md` | Player-facing phrasebook |

Do **not** implement EnchantGraph, restore Rust, or keep a `spell { }` compatibility path.

## Diagnostic codes (use these exact strings)

| Code | When |
|---|---|
| `E0002` | unexpected character (`{`, `}`, `;`, `@`, …) |
| `E0003` | malformed number |
| `E0004` | source exceeds 4096 Unicode scalars |
| `E0005` | source exceeds 16384 UTF-8 bytes |
| `E0200` | tab in indent |
| `E0201` | indent is not a multiple of 4 spaces |
| `E0202` | uppercase letter |
| `E0101` | expected number |
| `E0105` | unsupported / unknown word |
| `E0210` | expected verb |
| `E0211` | expected summon noun (`sheep`/`rocket`/`fangs`) |
| `E0212` | expected patient (`self`/`target`) |
| `E0213` | expected place |
| `E0214` | expected glue word (`ahead`/`at`/`for`/`seconds`/`skyward`/`riding`) |
| `E0215` | extra tokens on the line |
| `E0216` | indent depth greater than 1 |
| `E0217` | `riding` is not indented under a `summon` |
| `E0218` | indented line is not `riding` |
| `E0219` | `look` or `rest` is indented |
| `E0220` | `send` is indented |
| `E0221` | `strike` missing patient and `at` |
| `E0222` | reserved word (`when`, `if`, `spell`, …) |
| `E1003` | more than 16 non-blank lines |
| `E1004` | more than 4 effects |
| `E1011` | no effects |
| `E1012` | more than one `look` |
| `E1013` | more than one `rest` |
| `E1014` | `send` with no prior `summon` |
| `E1015` | `look` range not in `1..32` |
| `E1001` | burn/mend amount not in `0.5..20` |
| `E1002` | shove not in `0.1..3` |
| `E1009` | rest not in `0..60` |
| `E1016` | vanish duration not in `0.5..20` |

Reserved words (always `E0222`, never parsed as verbs): `when`, `if`, `spell`, `damage`, `heal`, `push`, `cooldown`, `upon`, `the`, `a`, `call`, `forth`.

## Canonical grammar (byte-exact)

Version line, then one opcode line per action, LF separators, **no trailing newline**.

```text
scribe-compiler/0.2
look_ahead|<16 hex bits>
summon|<noun>|<place>|<riding or ->
burn|<patient>|<16 hex bits>
mend|<patient>|<16 hex bits>
shove|<patient>|<16 hex bits>
strike|<place>
strike|ahead|<16 hex bits>
send_skyward
vanish|<patient>|<16 hex bits>
rest|<16 hex bits>
```

`place` is `caster`, `self`, `target`, or `ahead` (ahead is the two-field form above). Implicit summon place is `caster`. Default burn/mend/shove amount `1` is written as `3ff0000000000000`.

Golden identities (SHA-256 of the UTF-8 canonical bytes):

| Page | Identity |
|---|---|
| shepherd (`summon sheep` / `riding rocket` / `send skyward` / `rest 8 seconds`) | `a9b2c90b95ee7bc020fbe2c5ecfe1bdcab8fa8da653a783a8af040f85fbb32dd` |
| kindling (`look ahead 16` / `burn target 4` / `rest 3 seconds`) | `f4997fe3119ebbed9813da7cf263cd44e81983156ca02c148f32cd8e4ff7f969` |
| `burn target` (default 1) | `43792a1038718ee17e933de2a64c20eff7075f0e16981d83260f4a0b3568502f` |

---

### Task 1: Phrasebook compiler (API + lexer + parser + validate + canonical)

This is one green commit. Do the TDD steps in order. After the last step the project compiles and `:common:test` passes. Paper may still reference `Action` — update `SpellRuntime` / `WizardryPlugin` / `ScribeCommand` at the end of this task so `:paper:compileJava` succeeds (runtime behavior is Task 2).

**Files:**
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Action.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Phrase.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Page.java`
- Modify: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/CompiledSpell.java`
- Modify: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/CompilerConstants.java`
- Delete: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Operation.java`
- Delete: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Statement.java`
- Delete: `api/src/main/java/dev/mintychochip/wizardry/api/dsl/Program.java`
- Modify: `common/src/main/java/dev/mintychochip/wizardry/common/dsl/lexer/Lexer.java`
- Modify: `common/src/main/java/dev/mintychochip/wizardry/common/dsl/parser/Parser.java`
- Modify: `common/src/main/java/dev/mintychochip/wizardry/common/dsl/ScribeCompiler.java`
- Modify: `common/src/test/java/dev/mintychochip/wizardry/common/dsl/ModelTest.java`
- Modify: `common/src/test/java/dev/mintychochip/wizardry/common/dsl/LexerTest.java`
- Modify: `common/src/test/java/dev/mintychochip/wizardry/common/dsl/ParserTest.java`
- Modify: `common/src/test/java/dev/mintychochip/wizardry/common/dsl/CompilerTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntime.java` (compile-only switch; real behavior in Task 2)
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/command/ScribeCommand.java`

- [ ] **Step 1: Write the failing model test**

Replace `compiledSpellDefensivelyCopiesOperationsAndCanonicalBytes` and the `CompileResult.Ok` constructor that still passes a name. In `ModelTest.java`:

```java
@Test
void compiledSpellDefensivelyCopiesActionsAndCanonicalBytes() {
    var actions = new ArrayList<Action>();
    actions.add(new Action.Mend(Action.Patient.SELF, 1.0));
    byte[] canonical = {1, 2, 3};
    var spell = new CompiledSpell("scribe-compiler/0.2", "00", canonical, actions);

    actions.clear();
    canonical[0] = 9;

    assertEquals(1, spell.actions().size());
    assertEquals(1, spell.canonical()[0]);
}

@Test
void compileResultOkContainsTheSpell() {
    var spell = new CompiledSpell("v", "00", new byte[0], List.of());
    CompileResult result = new CompileResult.Ok(spell);
    assertInstanceOf(CompileResult.Ok.class, result);
    assertEquals(spell, ((CompileResult.Ok) result).spell());
}
```

Remove the `Operation` import. Keep the span and `Error` tests.

- [ ] **Step 2: Run the model test and confirm it fails to compile**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.ModelTest' --rerun-tasks`

Expected: FAIL — `CompiledSpell` has no 4-arg constructor / `Action` does not exist.

- [ ] **Step 3: Add Action, Phrase, Page, and reshape CompiledSpell**

`Action.java`:

```java
package dev.mintychochip.wizardry.api.dsl;

public sealed interface Action permits Action.LookAhead, Action.Summon, Action.Burn, Action.Mend,
        Action.Shove, Action.Strike, Action.SendSkyward, Action.Vanish, Action.Rest {
    enum Noun { SHEEP, ROCKET, FANGS }
    enum Patient { SELF, TARGET }

    sealed interface Place permits Place.Caster, Place.Self, Place.Target, Place.Ahead {
        record Caster() implements Place {}
        record Self() implements Place {}
        record Target() implements Place {}
        record Ahead(double range) implements Place {}
    }

    record LookAhead(double range) implements Action {}
    record Summon(Noun noun, Place place, Noun riding) implements Action {}
    record Burn(Patient patient, double amount) implements Action {}
    record Mend(Patient patient, double amount) implements Action {}
    record Shove(Patient patient, double amount) implements Action {}
    record Strike(Place place) implements Action {}
    record SendSkyward() implements Action {}
    record Vanish(Patient patient, double seconds) implements Action {}
    record Rest(double seconds) implements Action {}
}
```

`Summon.riding` is `null` when there is no `riding` child. `Phrase` mirrors each action plus a `Span span()` (same payloads). `Page` is `record Page(List<Phrase> phrases, Span span)` with `List.copyOf`.

`CompiledSpell`:

```java
public record CompiledSpell(
        String compilerVersion,
        String identitySha256,
        byte[] canonical,
        List<Action> actions
) {
    public CompiledSpell {
        compilerVersion = Objects.requireNonNull(compilerVersion, "compilerVersion");
        identitySha256 = Objects.requireNonNull(identitySha256, "identitySha256");
        canonical = Objects.requireNonNull(canonical, "canonical").clone();
        actions = List.copyOf(actions);
    }
    @Override public byte[] canonical() { return canonical.clone(); }
    public String canonicalHex() { return HexFormat.of().formatHex(canonical); }
}
```

`CompilerConstants.COMPILER_VERSION = "scribe-compiler/0.2"`. Keep the four cap constants.

Delete `Operation.java`, `Statement.java`, `Program.java`.

- [ ] **Step 4: Write failing lexer tests**

Replace `LexerTest.java` entirely:

```java
package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.api.dsl.Span;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import org.junit.jupiter.api.Test;

final class LexerTest {
    @Test
    void splitsLinesAndRecordsFourSpaceIndent() {
        var result = Lexer.lex("summon sheep\n    riding rocket\n");
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(2, result.lines().size());
        assertEquals(0, result.lines().get(0).indentSpaces());
        assertEquals(4, result.lines().get(1).indentSpaces());
        assertEquals("summon", result.lines().get(0).tokens().get(0).text());
        assertEquals("riding", result.lines().get(1).tokens().get(0).text());
    }

    @Test
    void ignoresBlankLines() {
        var result = Lexer.lex("burn target\n\nrest 3 seconds");
        assertEquals(2, result.lines().size());
    }

    @Test
    void tabIndentIsDiagnostic() {
        var result = Lexer.lex("summon sheep\n\triding rocket");
        assertEquals("E0200", result.diagnostics().getFirst().code());
    }

    @Test
    void twoSpaceIndentIsDiagnostic() {
        var result = Lexer.lex("summon sheep\n  riding rocket");
        assertEquals("E0201", result.diagnostics().getFirst().code());
    }

    @Test
    void uppercaseWordIsDiagnostic() {
        var result = Lexer.lex("Burn target");
        assertEquals("E0202", result.diagnostics().getFirst().code());
    }

    @Test
    void bracesAndSemicolonsAreUnexpectedCharacters() {
        var result = Lexer.lex("spell ember { burn target; }");
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("E0002") || d.code().equals("E0222")));
    }

    @Test
    void multibyteDiagnosticsUseUtf8SpansAndScalarColumns() {
        var result = Lexer.lex("é中😀@");
        assertEquals(4, result.diagnostics().size());
        assertEquals(new Span(0, 2, 1, 1), result.diagnostics().get(0).span());
        assertEquals(new Span(2, 5, 1, 2), result.diagnostics().get(1).span());
        assertEquals(new Span(5, 9, 1, 3), result.diagnostics().get(2).span());
        assertEquals(new Span(9, 10, 1, 4), result.diagnostics().get(3).span());
    }

    @Test
    void malformedDecimalIsLexicalFailure() {
        var result = Lexer.lex("burn target 1.2.3");
        assertEquals("E0003", result.diagnostics().getFirst().code());
    }
}
```

- [ ] **Step 5: Run lexer tests — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.LexerTest' --rerun-tasks`

Expected: FAIL — `Lexer.Result` has no `lines()`.

- [ ] **Step 6: Implement the line lexer**

Replace `Lexer` so that:

- `Result` is `record Result(List<Line> lines, List<Diagnostic> diagnostics)`.
- `Line` is `record Line(int indentSpaces, List<Token> tokens, Span span)`.
- `Token` stays `WORD` / `NUMBER` (no `SYMBOL`, no `EOF` token).
- Leading spaces count as indent. A tab in the leading indent → `E0200` and the line is not emitted.
- Leading spaces not divisible by 4 → `E0201` and the line is not emitted.
- Blank lines (only spaces) are skipped and do not emit diagnostics.
- `{`, `}`, `;` and other non-word/non-number/non-space → `E0002` (same UTF-8 span rules as today).
- A `WORD` containing any `A-Z` → `E0202`.
- Numbers: same finite-decimal rules; `1.2.3` → `E0003`.
- Newlines increment line and reset column. Indent column of the first token is `indentSpaces + 1`.
- Keep UTF-8 byte spans and Unicode-scalar columns.

Remove `lex(...).tokens()` — callers use `lines()`.

- [ ] **Step 7: Re-run lexer tests — expect PASS**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.LexerTest'`

Expected: PASS

- [ ] **Step 8: Write failing parser tests**

Replace `ParserTest.java`:

```java
package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.Phrase;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import org.junit.jupiter.api.Test;

final class ParserTest {
    @Test
    void parsesShepherdFlare() {
        var source = "summon sheep\n    riding rocket\nsend skyward\nrest 8 seconds";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.diagnostics().isEmpty());
        var phrases = parsed.page().orElseThrow().phrases();
        assertEquals(3, phrases.size());
        var summon = assertInstanceOf(Phrase.Summon.class, phrases.get(0));
        assertEquals(Action.Noun.SHEEP, summon.noun());
        assertEquals(Action.Noun.ROCKET, summon.riding());
        assertInstanceOf(Action.Place.Caster.class, summon.place());
        assertInstanceOf(Phrase.SendSkyward.class, phrases.get(1));
        assertEquals(8.0, assertInstanceOf(Phrase.Rest.class, phrases.get(2)).seconds());
    }

    @Test
    void strikeWithoutPlaceOrPatientIsError() {
        var parsed = Parser.parse(Lexer.lex("strike"), "strike");
        assertEquals("E0221", parsed.diagnostics().getFirst().code());
    }

    @Test
    void orphanRidingIsError() {
        var parsed = Parser.parse(Lexer.lex("    riding rocket"), "    riding rocket");
        assertEquals("E0217", parsed.diagnostics().getFirst().code());
    }

    @Test
    void depthTwoIndentIsError() {
        var source = "summon sheep\n    riding rocket\n        riding fangs";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.diagnostics().stream().anyMatch(d -> d.code().equals("E0216") || d.code().equals("E0201")));
    }

    @Test
    void retiredSpellWrapperIsRejected() {
        var source = "spell ember { burn target 4 }";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.page().isEmpty());
        assertFalse(parsed.diagnostics().isEmpty());
    }

    @Test
    void emptyTokensRejectWithoutThrowing() {
        var parsed = Parser.parse(Lexer.lex(""), "");
        assertTrue(parsed.page().isEmpty());
        assertTrue(parsed.diagnostics().stream().allMatch(d -> d.span().line() > 0));
    }
}
```

`Parser.Result` is `record Result(Optional<Page> page, List<Diagnostic> diagnostics)`. Empty input is `Error` at compile time (`E1011`); the parser may return an empty page with no diagnostics — the compiler owns `E1011`. For this empty-input test, either emit a diagnostic or return an empty page; do **not** throw. Prefer empty page + no diagnostics here; compiler test covers `E1011`.

Adjust `emptyTokensRejectWithoutThrowing` to:

```java
assertTrue(parsed.diagnostics().stream().allMatch(d -> d.span().line() > 0));
```

which passes on an empty diagnostic list.

- [ ] **Step 9: Run parser tests — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.ParserTest'`

Expected: FAIL — `Parser.parse` still takes tokens / has no `page()`.

- [ ] **Step 10: Implement the phrasebook parser**

`Parser.parse(Lexer.Result lexed, String source)`:

- If `lexed.diagnostics()` is non-empty, return them (do not parse). The compiler also short-circuits on lexer diagnostics; either path is fine as long as tests see the lexer codes.
- Walk `lexed.lines()` in order.
- Indent 8+ or indent/4 > 1 → `E0216`.
- Indent 4: first word must be `riding`; attach `Noun` to the previous `Phrase.Summon`; else `E0218`. If there is no previous summon or it already has riding → `E0217`.
- Indent 0: dispatch on first word:

| First word | Parse |
|---|---|
| `look` | `ahead` + NUMBER → `Phrase.LookAhead` |
| `summon` | NOUN + optional `at` PLACE → `Phrase.Summon` (`riding` null, place default `Caster`) |
| `burn`/`mend`/`shove` | PATIENT + optional NUMBER (default 1.0) |
| `strike` | `at` PLACE **or** PATIENT (patient `self`/`target` become `Place.Self`/`Place.Target`). Bare `strike` → `E0221` |
| `send` | `skyward` → `Phrase.SendSkyward` |
| `vanish` | PATIENT `for` NUMBER `seconds` |
| `rest` | NUMBER `seconds` |
| reserved | `E0222` |
| other WORD | `E0105` |

PLACE: `caster` → `Caster`, `self` → `Self`, `target` → `Target`, `ahead` NUMBER → `Ahead`.

If `look` or `rest` appears at indent 4 → `E0219`. If `send` at indent 4 → `E0220`.

Extra tokens after a complete phrase → `E0215`.

Missing number/glue/noun/patient → the matching `E01xx` / `E021x` code on that token's span.

On any diagnostic, `page()` is empty (atomic parse: no partial page).

- [ ] **Step 11: Re-run parser tests — expect PASS**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.ParserTest'`

Expected: PASS

- [ ] **Step 12: Write failing compiler tests**

Replace `CompilerTest.java`:

```java
package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import org.junit.jupiter.api.Test;

final class CompilerTest {
    @Test
    void compilesShepherdIdentity() {
        var source = "summon sheep\n    riding rocket\nsend skyward\nrest 8 seconds";
        var result = ScribeCompiler.INSTANCE.compile(source);
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("scribe-compiler/0.2", spell.compilerVersion());
        assertEquals(
                "scribe-compiler/0.2\nsummon|sheep|caster|rocket\nsend_skyward\nrest|4020000000000000",
                new String(spell.canonical(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("a9b2c90b95ee7bc020fbe2c5ecfe1bdcab8fa8da653a783a8af040f85fbb32dd", spell.identitySha256());
        var summon = assertInstanceOf(Action.Summon.class, spell.actions().getFirst());
        assertEquals(Action.Noun.SHEEP, summon.noun());
        assertEquals(Action.Noun.ROCKET, summon.riding());
    }

    @Test
    void blankLinesDoNotChangeIdentity() {
        var compact = ScribeCompiler.INSTANCE.compile("burn target");
        var spaced = ScribeCompiler.INSTANCE.compile("\n\nburn target\n\n");
        var a = assertInstanceOf(CompileResult.Ok.class, compact).spell();
        var b = assertInstanceOf(CompileResult.Ok.class, spaced).spell();
        assertEquals(a.identitySha256(), b.identitySha256());
        assertEquals("43792a1038718ee17e933de2a64c20eff7075f0e16981d83260f4a0b3568502f", a.identitySha256());
    }

    @Test
    void kindlingLookMatchesGoldenIdentity() {
        var result = ScribeCompiler.INSTANCE.compile("look ahead 16\nburn target 4\nrest 3 seconds");
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("f4997fe3119ebbed9813da7cf263cd44e81983156ca02c148f32cd8e4ff7f969", spell.identitySha256());
    }

    @Test
    void sendWithoutSummonIsError() {
        var result = ScribeCompiler.INSTANCE.compile("send skyward");
        assertEquals("E1014", assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code());
    }

    @Test
    void twoLooksAreError() {
        var result = ScribeCompiler.INSTANCE.compile("look ahead 8\nlook ahead 16\nburn target");
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E1012")));
    }

    @Test
    void pageWithOnlyRestIsError() {
        var result = ScribeCompiler.INSTANCE.compile("rest 3 seconds");
        assertEquals("E1011", assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code());
    }

    @Test
    void fiveEffectsAreError() {
        var source = "burn target\nmend self\nshove target\nstrike target\nvanish self for 1 seconds";
        var result = ScribeCompiler.INSTANCE.compile(source);
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E1004")));
    }

    @Test
    void acceptsInclusiveBoundaries() {
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(
                "look ahead 1\nburn target 0.5\nmend self 20\nshove target 0.1"));
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(
                "look ahead 32\nshove target 3\nrest 60 seconds"));
    }

    @Test
    void rejectsOutOfRangeMagnitudes() {
        assertEquals("E1001", code("burn target 0.4"));
        assertEquals("E1002", code("shove target 0.05"));
        assertEquals("E1015", code("look ahead 0\nburn target"));
        assertEquals("E1016", code("vanish self for 0 seconds"));
    }

    @Test
    void retiredSpellSourceIsError() {
        var result = ScribeCompiler.INSTANCE.compile("spell ember { target ray 16; damage target fire 4; }");
        assertInstanceOf(CompileResult.Error.class, result);
    }

    @Test
    void sourceLimitsUseScalarsAndUtf8Bytes() {
        String valid = " ".repeat(4096 - "burn target".codePointCount(0, "burn target".length())) + "burn target";
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(valid));
        var tooMany = ScribeCompiler.INSTANCE.compile("é".repeat(4097));
        assertEquals("E0004", assertInstanceOf(CompileResult.Error.class, tooMany).diagnostics().getFirst().code());
    }

    @Test
    void rejectsMultibyteUtf8OverflowWithoutThrowing() {
        var result = ScribeCompiler.INSTANCE.compile("é".repeat(8193));
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E0005")));
    }

    private static String code(String source) {
        return assertInstanceOf(CompileResult.Error.class, ScribeCompiler.INSTANCE.compile(source))
                .diagnostics().getFirst().code();
    }
}
```

- [ ] **Step 13: Run compiler tests — expect FAIL**

Run: `./gradlew :common:test --tests 'dev.mintychochip.wizardry.common.dsl.CompilerTest'`

Expected: FAIL — compiler still builds `Operation` / old canonical.

- [ ] **Step 14: Implement ScribeCompiler for the phrasebook**

Pipeline:

1. Source-limit diagnostics (`E0004`/`E0005`) — keep existing scalar/UTF-8 helpers.
2. `Lexer.lex`. If diagnostics → `CompileResult.error(cap(...))`.
3. `Parser.parse`. If no page → `error`.
4. Validate the `Page`:
   - non-blank phrase count > 16 → `E1003` (count phrases, not physical lines; physical-line cap is the editor). Also reject if the lexer produced more than 16 lines → `E1003`.
   - effect count: `Summon`, `Burn`, `Mend`, `Shove`, `Strike`, `SendSkyward`, `Vanish`. 0 → `E1011`. > 4 → `E1004`.
   - more than one `LookAhead` → `E1012`. more than one `Rest` → `E1013`.
   - `SendSkyward` with no earlier `Summon` in phrase order → `E1014`.
   - bounds per the spec table (`E1015`, `E1001`, `E1002`, `E1009`, `E1016`).
5. Map phrases → `Action` (drop spans). `Summon.riding` comes from the phrase.
6. Canonicalize with the grammar above. Helper:

```java
private static String bits(double v) {
    return String.format(java.util.Locale.ROOT, "%016x", Double.doubleToRawLongBits(v));
}
private static String place(Action.Place p) {
    return switch (p) {
        case Action.Place.Caster ignored -> "caster";
        case Action.Place.Self ignored -> "self";
        case Action.Place.Target ignored -> "target";
        case Action.Place.Ahead ignored -> "ahead";
    };
}
```

`strike` + `Ahead` uses `strike|ahead|<bits>`. Other strikes use `strike|<place>`.
`summon` uses `summon|<noun>|<place>|<riding or ->`. If place is `Ahead`, use `summon|<noun>|ahead|<bits>|<riding or ->` so the number is not lost.

7. `CompileResult.ok(new CompiledSpell(COMPILER_VERSION, sha256(canonical), canonical, actions))`.

Keep sort/cap of diagnostics exactly as today.

- [ ] **Step 15: Update Paper so the tree compiles**

`SpellRuntime.cast` signature stays `(Player, LivingEntity, CompiledSpell, long)` plus the range overload. Replace the `Operation` switch with an `Action` switch. For this task it is enough that it **compiles** and that target-preflight is conservative:

- Needs a looked-at entity if any action has `Patient.TARGET` or `Place.Target`.
- Range: first `LookAhead`, else `32`.
- If `needsTarget && (target == null || !target.isValid())` → `false`.
- Execute: implement the real Paper table now (avoid a second rewrite):

```java
case Action.LookAhead ignored -> { }
case Action.Burn x -> applyBurn(x, caster, target);
case Action.Mend x -> applyMend(x, caster, target);
case Action.Shove x -> applyShove(x, caster, target);
case Action.Strike x -> world.strikeLightning(resolvePlace(x.place(), caster, target, look));
case Action.Summon x -> spawnSummon(x, caster, target, look);
case Action.SendSkyward ignored -> { if (lastVehicle != null) lastVehicle.setVelocity(new Vector(0, 1.5, 0)); }
case Action.Vanish x -> applyVanish(x, caster, target);
case Action.Rest x -> cooldownUntil = Math.max(cooldownUntil, nowMillis + Math.round(x.seconds() * 1000));
```

`spawnSummon`: resolve place; spawn `riding` first if non-null (vehicle), then `noun` (passenger), `vehicle.addPassenger(passenger)`; tag both with a PDC key `wizardry:scribe_summon` = 1. Remember `lastVehicle`. Entity types: `SHEEP`, `FIREWORK_ROCKET`, `EVOKER_FANGS` — a closed `switch`, never `EntityType.valueOf`.

`applyBurn`: `entity.damage(amount, caster)` and `setFireTicks(max(current, (int) Math.round(amount * 20)))`.

`applyMend`: `setHealth(min(getMaxHealth(), getHealth() + amount))` on the patient entity.

`applyShove`: same vector math as today's push, strength from the action.

`applyVanish`: `setInvisible(true)`. Duration restoration is Task 2 (needs a scheduler). In this task, set invisible and leave it — Task 2 adds the delayed clear.

`resolvePlace`: `Caster`/`Self` → caster location; `Target` → target location; `Ahead` → `caster.getEyeLocation().add(direction.multiply(range))`.

`WizardryPlugin` / `ScribeCommand`: range from first `Action.LookAhead`, else 32. Delete `Operation` imports.

- [ ] **Step 16: Run common tests and paper compile**

Run:

```text
./gradlew :common:test :paper:compileJava --rerun-tasks
```

Expected: `:common:test` PASS. `:paper:compileJava` PASS (MapPalette deprecation warnings are OK).

If `ConformanceTest` still reads v0 fixtures, it will FAIL. Either skip it until Task 1b below or update it in the next steps of this same task.

- [ ] **Step 17: Replace the conformance corpus (same commit)**

Delete every file in `common/src/test/resources/conformance/fixtures/`.

Replace `schema-v1.json` with `schema-v2.json` (`schemaVersion` const `2`). Accepted result required fields: `status`, `compilerVersion`, `canonicalHex`, `identitySha256`, `actions`. No `name`. Each action:

```json
{
  "opcode": "summon",
  "noun": "sheep",
  "place": "caster",
  "aheadBits": null,
  "patient": null,
  "riding": "rocket",
  "valueBits": null
}
```

`opcode` enum: `look_ahead`, `summon`, `burn`, `mend`, `shove`, `strike`, `send_skyward`, `vanish`, `rest`.

Write at least these fixtures (source + expected result). Compute `canonicalHex` as UTF-8 hex of the canonical string.

1. `valid-shepherd.json` — shepherd page; identity `a9b2c90b95ee7bc020fbe2c5ecfe1bdcab8fa8da653a783a8af040f85fbb32dd`
2. `valid-kindling.json` — kindling page; identity `f4997fe3119ebbed9813da7cf263cd44e81983156ca02c148f32cd8e4ff7f969`
3. `valid-burn-default.json` — `burn target`; identity `43792a1038718ee17e933de2a64c20eff7075f0e16981d83260f4a0b3568502f`
4. `valid-formatting.json` — same as shepherd with extra blank lines; **same identity as shepherd**
5. `valid-places.json` — `summon fangs at target` / `strike at ahead 8` / `mend self`
6. `invalid-send-alone.json` — `send skyward` → `E1014`
7. `invalid-retired-spell.json` — old ember source
8. `invalid-orphan-riding.json`
9. `invalid-five-effects.json`
10. `invalid-uppercase.json` — `Burn target`
11. `invalid-tab-indent.json`
12. `invalid-no-effects.json` — `rest 3 seconds`

Rewrite `ConformanceTest.compareFixture` to `schemaVersion == 2`, `CompileResult.Ok` / `Error`, and compare action fields (`opcode`, `noun`, `place`, `aheadBits`, `patient`, `riding`, `valueBits`) instead of `Operation`.

Helper mapping must switch on `Action`, not `Operation`.

- [ ] **Step 18: Run common tests — expect PASS**

Run: `./gradlew :common:test --rerun-tasks`

Expected: PASS, including `ConformanceTest`.

- [ ] **Step 19: Commit**

```bash
git add api/src/main/java/dev/mintychochip/wizardry/api/dsl \
        common/src/main/java/dev/mintychochip/wizardry/common/dsl \
        common/src/test/java/dev/mintychochip/wizardry/common/dsl \
        common/src/test/resources/conformance \
        paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntime.java \
        paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java \
        paper/src/main/java/dev/mintychochip/wizardry/paper/command/ScribeCommand.java
git commit -m "feat: compile the Scribe phrasebook language"
```

---

### Task 2: Spell runtime — Paper table, vanish restore, atomic target miss

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntime.java`
- Create: `paper/src/test/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntimeTest.java`

- [ ] **Step 1: Write failing runtime tests**

`SpellRuntime` needs a scheduler hook so vanish can be restored in tests:

```java
public SpellRuntime() { this((delay, task) -> {}); }
public SpellRuntime(BiConsumer<Long, Runnable> later) { ... }
```

`later.accept(delayTicks, task)` — Paper will pass `plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks)`.

Test file (Mockito, `paper` already has `mockito-core`):

```java
@Test
void missingTargetIsAtomic() {
    var sheep = mock(LivingEntity.class);
    var world = mock(World.class);
    var caster = mock(Player.class);
    when(caster.isValid()).thenReturn(true);
    when(caster.getWorld()).thenReturn(world);
    var spell = compile("summon sheep at target\nburn target");
    var runtime = new SpellRuntime();
    assertFalse(runtime.cast(caster, null, spell, 0L));
    verify(world, never()).spawn(any(), any(), any());
}

@Test
void ridingSpawnsVehicleThenPassenger() {
    // mock World.spawn to return distinct Entity mocks for FIREWORK_ROCKET then SHEEP
    // verify addPassenger(sheep) on the rocket
    // verify setVelocity(new Vector(0, 1.5, 0)) on the rocket after send skyward
}

@Test
void strikeUsesTargetLocation() {
    // compile "strike target"
    // verify world.strikeLightning(targetLocation)
}

@Test
void vanishClearsAfterDuration() {
    var tasks = new ArrayList<Runnable>();
    var runtime = new SpellRuntime((delay, task) -> { assertEquals(60L, delay); tasks.add(task); });
    // vanish self for 3 seconds → 3 * 20 ticks
    // verify setInvisible(true) immediately, false after tasks.run()
}

@Test
void restBlocksSecondCast() {
    var spell = compile("burn target\nrest 3 seconds");
    var runtime = new SpellRuntime();
    assertTrue(runtime.cast(caster, target, spell, 0L));
    assertTrue(runtime.onCooldown(caster, spell, 1_000L));
    assertFalse(runtime.cast(caster, target, spell, 1_000L));
    assertTrue(runtime.cast(caster, target, spell, 3_001L));
}
```

`compile` helper: `((CompileResult.Ok) ScribeCompiler.INSTANCE.compile(src)).spell()`.

- [ ] **Step 2: Run tests — expect FAIL**

Run: `./gradlew :paper:test --tests 'dev.mintychochip.wizardry.paper.runtime.SpellRuntimeTest'`

Expected: FAIL — vanish is never cleared; constructor has no scheduler; and/or spawn order is wrong.

- [ ] **Step 3: Implement vanish restore and PDC tag**

- Inject `BiConsumer<Long, Runnable> later`.
- After `setInvisible(true)`, `later.accept(Math.round(seconds * 20), () -> { if (entity.isValid()) entity.setInvisible(false); })`.
- Default constructor uses a no-op `later` only in unit isolation; `WizardryPlugin` constructs `new SpellRuntime((delay, task) -> getServer().getScheduler().runTaskLater(this, task, delay))`.
- Confirm spawn order: vehicle (`riding`) first, passenger second, `addPassenger`, then `SendSkyward` uses that vehicle.
- Confirm target-miss returns `false` **before** any spawn/damage/lightning/cooldown write.

Preflight must scan **all** actions first. If any requires `target` and it is missing, return `false` immediately.

- [ ] **Step 4: Run runtime tests — expect PASS**

Run: `./gradlew :paper:test --tests 'dev.mintychochip.wizardry.paper.runtime.SpellRuntimeTest' :common:test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntime.java \
        paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java \
        paper/src/test/java/dev/mintychochip/wizardry/paper/runtime/SpellRuntimeTest.java
git commit -m "feat: execute phrasebook actions through Paper"
```

---

### Task 3: Paper 26.2 multiline dialog editor

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialog.java`
- Modify: `paper/src/test/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialogStateTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/listener/ScribeBookListener.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/listener/ScribeChatListener.java` (keep as fallback; do not delete)

- [ ] **Step 1: Write failing dialog contract tests**

Add to `ScribeDialogStateTest`:

```java
@Test
void sourceKeyAndLimitsMatchDialogContract() {
    assertEquals("source", ScribeDialog.SOURCE_KEY);
    assertEquals(1024, ScribeDialog.WIDTH_PIXELS);
    assertEquals(512, ScribeDialog.HEIGHT_PIXELS);
    assertEquals(128, ScribeDialog.MAX_LINES);
    assertEquals(4096, ScribeDialog.MAX_SCALARS);
}

@Test
void readsPreservedNewlinesFromResponse() {
    var view = mock(io.papermc.paper.dialog.DialogResponseView.class);
    when(view.getText("source")).thenReturn("summon sheep\n    riding rocket");
    assertEquals("summon sheep\n    riding rocket", ScribeDialog.readSource(view));
}

@Test
void saveAndCastWithErrorDoesNotCallCaster() {
    // existing submit path: compile phrasebook error (e.g. "send skyward")
    // caster must not run; outcome.cast() is false; compilation is CompileResult.Error
}
```

Add `SOURCE_KEY` and `readSource` — they do not exist yet.

- [ ] **Step 2: Run tests — expect FAIL**

Run: `./gradlew :paper:test --tests 'dev.mintychochip.wizardry.paper.dialog.ScribeDialogStateTest'`

Expected: FAIL — `SOURCE_KEY` / `readSource` missing.

- [ ] **Step 3: Implement Dialog show + getText**

On `ScribeDialog`:

```java
public static final String SOURCE_KEY = "source";
public static String readSource(DialogResponseView view) {
    String text = view.getText(SOURCE_KEY);
    return text == null ? "" : text;
}
```

Add `public void show(Player player, ItemStack book, long nowMillis)`:

1. `open(...)` to create the session (same UUID/expiry rules).
2. Build `Dialog.create(factory -> factory.empty().base(...).type(...))`:
   - Title: `Component.text("Scribe")`.
   - Body: if the last compile was `Error`, one `DialogBody.plainMessage` per diagnostic (`code + ": " + message`), otherwise empty or a one-line hint `verb first, indent riding`.
   - Input: `DialogInput.text(SOURCE_KEY, Component.text("page")).width(1024).maxLength(4096).initial(session.pendingSource()).multiline(TextDialogInput.MultilineOptions.create(128, 512))`.
   - Buttons via `DialogType.multiAction(List.of(save, saveCast, cancel))`.
   - Each button `DialogAction.customClick(callback, ClickCallback.Options.builder().uses(1).lifetime(MAX_CALLBACK_LIFETIME).build())`.
   - Callback: resolve the player from the `Audience`; load main-hand book; `readSource(view)`; `draft` + `submit` with `SAVE` / `SAVE_AND_CAST` / `CANCEL`.
   - On `SAVE_AND_CAST` + `Error`: call `show` again with diagnostics in the body. Do not cast.
3. `player.showDialog(dialog)`.

`ScribeBookListener`: after `open`, call `dialog.show(player, item, now)` and **remove** the chat instruction messages.

Keep `/scribe begin` + `ScribeChatListener` as fallback (session already opened). Do not change their protocol.

`canCloseWithEscape(true)`. Escape still hits existing `Action.ESCAPE` only if you wire an exit action; if the API just closes, that is `ESCAPE` (persist nothing) — do not save.

If `DialogInput.text` builder names differ slightly from this snippet, follow the 26.2 javap: `DialogInput.text(key, label)` → builder with `width`, `maxLength`, `initial`, `multiline`.

- [ ] **Step 4: Run dialog + paper tests — expect PASS**

Run: `./gradlew :paper:test :common:test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialog.java \
        paper/src/main/java/dev/mintychochip/wizardry/paper/listener/ScribeBookListener.java \
        paper/src/test/java/dev/mintychochip/wizardry/paper/dialog/ScribeDialogStateTest.java
git commit -m "feat: edit Scribe pages in a Paper multiline dialog"
```

---

### Task 4: Starter book + player-facing language doc

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/book/ScribeBookStore.java`
- Create: `paper/src/test/java/dev/mintychochip/wizardry/paper/book/ScribeBookStoreTest.java` (only if a store test already exists — there is none; add a focused test for the starter string)
- Modify: `docs/scribe-language.md`

- [ ] **Step 1: Write a failing starter-source test**

```java
@Test
void starterSourceIsALegalPhrasebookPage() {
    assertInstanceOf(CompileResult.Ok.class,
            ScribeCompiler.INSTANCE.compile(ScribeBookStore.STARTER_SOURCE));
    assertTrue(ScribeBookStore.STARTER_SOURCE.contains("summon sheep"));
}
```

`STARTER_SOURCE` is still the old `spell novice { ... }` — this fails.

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :paper:test --tests 'dev.mintychochip.wizardry.paper.book.ScribeBookStoreTest'`

Expected: FAIL — compile of starter is `Error`.

- [ ] **Step 3: Change the starter and rewrite `docs/scribe-language.md`**

```java
public static final String STARTER_SOURCE = "look ahead 8\nburn target\nrest 3 seconds";
```

Rewrite `docs/scribe-language.md` to the phrasebook: line grammar, closed word list, implicit at caster, `riding`, dialog authoring (26.2), bounds, canonical version `0.2`, and the four worked pages from the spec. Remove the `spell { }` grammar and the “Dialog API not exposed” paragraph.

- [ ] **Step 4: Run tests — expect PASS**

Run: `./gradlew :common:test :paper:test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/book/ScribeBookStore.java \
        paper/src/test/java/dev/mintychochip/wizardry/paper/book/ScribeBookStoreTest.java \
        docs/scribe-language.md
git commit -m "docs: teach the phrasebook in the starter book and language guide"
```

---

## Self-review

**Spec coverage**

| Spec requirement | Task |
|---|---|
| Closed phrasebook verbs/nouns/glue | 1 (parser + reserved words) |
| Significant lines, 4-space indent, no `;` | 1 (lexer) |
| Implicit at caster, `at` place, `riding` | 1 |
| `send` last summon, velocity (0,1.5,0) | 1 compile + 2 runtime |
| Caps, atomic `Error`, canonical 0.2 identities | 1 |
| Retire `spell { }` / delete `Operation` | 1 |
| No spell name in identity | 1 |
| Paper table, no `EntityType.valueOf` | 1/2 |
| Target-miss atomic | 2 |
| Vanish duration restore | 2 |
| 26.2 multiline dialog, `getText("source")` | 3 |
| Chat fallback remains, not the editor | 3 |
| Starter book + `docs/scribe-language.md` | 4 |
| EnchantGraph / Rust / `when` | explicitly not scheduled |

**Type names used throughout:** `Action`, `Action.Noun`, `Action.Patient`, `Action.Place.*`, `Phrase.*`, `Page`, `CompiledSpell(compilerVersion, identitySha256, canonical, actions)`, `CompileResult.Ok` / `Error`, `Lexer.Line`, `Parser.parse(Lexer.Result, String)`.

**Summon + Ahead canonical:** `summon|<noun>|ahead|<bits>|<riding or ->` so the ahead number is not dropped. Strike ahead: `strike|ahead|<bits>`.
