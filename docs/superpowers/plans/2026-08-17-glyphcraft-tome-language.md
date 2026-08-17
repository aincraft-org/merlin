# Glyphcraft Tome Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Glyphcraft tome language in `docs/superpowers/specs/2026-08-17-glyphcraft-tome-language-design.md`: typed glyph-map tokens, a compiler to the existing Action tape, a MapGUI pip clicker, tomes as bound map pages, combat cast, and Sharpness bind.

**Architecture:** `api`/`common` own roles, tokens, mana, and `GlyphCompiler` (no Bukkit). Paper freezes tokens onto the existing glyph map PDC, authors pips on a MapGUI overlay, stores tome pages, and casts through `SpellRuntime`. Page order is display-only; meaning comes from roles and implicits.

**Tech Stack:** Java 21 (`api`, `common`), Java 25 (`paper`), JUnit 5, Mockito 5.15, Paper API `26.2.build.84-stable`, MapGUI, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-17-glyphcraft-tome-language-design.md`

---

## File map

| File | Responsibility |
|---|---|
| `api/.../glyph/GlyphRole.java` | EFFECT, SCHOOL, PATIENT, CHARM, AIM, TRIGGER, CONDITION, SCOPE, LIMIT, BOTTOM |
| `api/.../glyph/GlyphRoles.java` | `of(Label)`, `hasPips`, `grammatical` |
| `api/.../glyph/GlyphToken.java` | `label` + `pips` (1..5), role checks |
| `api/.../glyph/ManaTable.java` | printed mana for a token (v1 data) |
| `api/.../glyph/CharmBind.java` | `sharpness` + rank |
| `api/.../glyph/GlyphCompiler.java` | pages → `CompileResult` / `charm` |
| `common/.../glyph/GlyphCompilerImpl.java` | grammar, implicits, tape, canonical, SHA-256 |
| `common/src/test/.../glyph/GlyphLanguageTest.java` | roles, mana, grammar, identities |
| `paper/.../mapgui/GlyphScreen.java` | pip clicker overlay; pips not in draft |
| `paper/.../mapgui/GlyphDraftStoreAdapter.java` | persist `glyph_label` / `glyph_pips` / `glyph_mana` |
| `paper/.../tome/GlyphTomeStore.java` | book of pages, insert/tear/flip |
| `paper/.../tome/GlyphTomeListener.java` | use map on tome; open tome |
| `paper/.../mapgui/GlyphCommand.java` | `/glyph tome`, `/glyph cast` |
| `paper/.../runtime/CharmBinder.java` | sharpness map → vanilla Sharpness |
| `docs/glyphcraft-language.md` | player-facing language |

Do **not** implement EnchantGraph, multi-glyph segmentation, spending mana, or a Scribe identity match.

## Diagnostic codes (use these exact strings)

| Code | When |
|---|---|
| `G0100` | empty page list |
| `G0101` | `reject` or unknown |
| `G0102` | reserved label |
| `G0103` | more than 3 pages |
| `G0104` | more than one effect |
| `G0105` | more than one school |
| `G0106` | more than one patient |
| `G0107` | school or patient with no effect |
| `G0108` | charm mixed with other pages |
| `G0109` | pips not in `1..5` |
| `G0110` | pips ≠ 1 on a no-pip role |
| `G0111` | effect `shield` |

## Canonical grammar (byte-exact)

Version line, then Scribe-shaped opcode lines, LF separators, **no trailing newline**.

```text
glyph-compiler/0.1
look_ahead|<16 hex bits>     # only if implicit or explicit patient is target
burn|<patient>|<16 hex bits>
mend|<patient>|<16 hex bits>
shove|<patient>|<16 hex bits>
```

`look_ahead` range is always `32.0` → `4040000000000000`. Default amount `1.0` → `3ff0000000000000`. Amount `5.0` → `4014000000000000`. Shove pips 5 clamps to `3.0` → `4008000000000000`.

| Term | Identity |
|---|---|
| `damage ●●●●●` (implicit target) | `9e71957a3a06e4e66f8cc9d0ac35485c967843d91b042ad99a1a70517464f8fb` |
| `damage` pips 1 | `95c2ca180d7ce4539a86fea12a49d323926dd04a085d5bf08ab7c27ee04f55a1` |
| `heal` + `self` | `b1677feaea78c242a11508f5aab0e1afb3d72de57242f685de42e648e12d5152` |
| `push` pips 5 | `b4078f15c51d789db2324912a80dd454d4676020d83bf6e2500f432a241354c4` |

`COMPILER_VERSION` for this compiler is `glyph-compiler/0.1` (do not change `scribe-compiler/0.2`).

---

### Task 1: Roles, tokens, mana table

This is one green commit. `:api:test` and `:common:test` stay green.

**Files:**
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphRole.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphRoles.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphToken.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/ManaTable.java`
- Create: `common/src/test/java/dev/mintychochip/wizardry/common/glyph/GlyphLanguageTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package dev.mintychochip.wizardry.common.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.api.glyph.GlyphRole;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.glyph.ManaTable;
import dev.mintychochip.wizardry.api.ml.Label;
import org.junit.jupiter.api.Test;

final class GlyphLanguageTest {
    @Test void everyLabelHasARole() {
        assertEquals(Label.values().length, 24);
        assertEquals(GlyphRole.EFFECT, GlyphRoles.of(Label.DAMAGE));
        assertEquals(GlyphRole.EFFECT, GlyphRoles.of(Label.HEAL));
        assertEquals(GlyphRole.EFFECT, GlyphRoles.of(Label.PUSH));
        assertEquals(GlyphRole.EFFECT, GlyphRoles.of(Label.SHIELD));
        assertEquals(GlyphRole.SCHOOL, GlyphRoles.of(Label.FIRE));
        assertEquals(GlyphRole.SCHOOL, GlyphRoles.of(Label.FROST));
        assertEquals(GlyphRole.SCHOOL, GlyphRoles.of(Label.ARCANE));
        assertEquals(GlyphRole.SCHOOL, GlyphRoles.of(Label.PHYSICAL));
        assertEquals(GlyphRole.PATIENT, GlyphRoles.of(Label.SELF));
        assertEquals(GlyphRole.PATIENT, GlyphRoles.of(Label.TARGET));
        assertEquals(GlyphRole.CHARM, GlyphRoles.of(Label.SHARPNESS));
        assertEquals(GlyphRole.AIM, GlyphRoles.of(Label.TARGET_RAY));
        assertEquals(GlyphRole.TRIGGER, GlyphRoles.of(Label.ON_HIT));
        assertEquals(GlyphRole.CONDITION, GlyphRoles.of(Label.IF_UNDEAD));
        assertEquals(GlyphRole.SCOPE, GlyphRoles.of(Label.AREA));
        assertEquals(GlyphRole.LIMIT, GlyphRoles.of(Label.COOLDOWN));
        assertEquals(GlyphRole.BOTTOM, GlyphRoles.of(Label.REJECT));
        assertEquals(GlyphRole.PATIENT, GlyphRoles.of(Label.ATTACKER));
        assertTrue(GlyphRoles.hasPips(GlyphRole.EFFECT));
        assertTrue(GlyphRoles.hasPips(GlyphRole.CHARM));
        assertFalse(GlyphRoles.hasPips(GlyphRole.SCHOOL));
        assertTrue(GlyphRoles.grammatical(GlyphRole.EFFECT));
        assertFalse(GlyphRoles.grammatical(GlyphRole.TRIGGER));
    }

    @Test void tokenRejectsBadPips() {
        assertThrows(IllegalArgumentException.class, () -> new GlyphToken(Label.DAMAGE, 0));
        assertThrows(IllegalArgumentException.class, () -> new GlyphToken(Label.DAMAGE, 6));
        assertThrows(IllegalArgumentException.class, () -> new GlyphToken(Label.FIRE, 2));
        assertEquals(1, new GlyphToken(Label.FIRE, 1).pips());
    }

    @Test void manaTableV1() {
        var mana = ManaTable.v1();
        assertEquals(3, mana.mana(new GlyphToken(Label.DAMAGE, 1)));
        assertEquals(7, mana.mana(new GlyphToken(Label.DAMAGE, 5)));
        assertEquals(2, mana.mana(new GlyphToken(Label.FIRE, 1)));
        assertEquals(1, mana.mana(new GlyphToken(Label.SELF, 1)));
        assertEquals(4, mana.mana(new GlyphToken(Label.SHARPNESS, 1)));
        assertEquals(12, mana.mana(new GlyphToken(Label.SHARPNESS, 5)));
    }
}
```

`Label.SHARPNESS` does not exist yet. Add it as an extra enum constant **only if** you keep the 24-class classifier intact. **Do not change the 24-class ONNX vocabulary.** Charm `sharpness` is a **language** label: add `Label.SHARPNESS("sharpness")` to the enum. The classifier will not emit it until a later catalog drop; players obtain a sharpness map via `/glyph stamp sharpness [pips]` in Task 4, or by classifying once the catalog gains the class. For Task 1, add the enum constant. If `Label.values().length` is currently 24 including `REJECT` and no sharpness, the test above expects 25 after the add **or** keep 24 and treat `sharpness` as a compiler-only id.

**Correction (YAGNI):** Do **not** add a 25th classifier label. Represent charm as `Label` only when the catalog has it. Check `Label.java` — there is no `SHARPNESS` today. Use a compiler-side charm id:

In Task 1 tests, use a dedicated `CharmId` or put sharpness in `GlyphToken` as optional. Spec says label `sharpness`. Add `Label.SHARPNESS("sharpness")` **without** retraining. `Label.fromId("sharpness")` works. Classifier `fromId` of model outputs will never produce it. `everyLabelHasARole` should iterate `Label.values()` and assert no throw, and assert `values().length >= 24`.

Replace the length assert with:

```java
for (var label : Label.values()) {
    assertTrue(GlyphRoles.of(label) != null);
}
assertEquals(GlyphRole.CHARM, GlyphRoles.of(Label.SHARPNESS));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests dev.mintychochip.wizardry.common.glyph.GlyphLanguageTest --rerun-tasks`

Expected: FAIL — `GlyphRoles` / `Label.SHARPNESS` do not exist.

- [ ] **Step 3: Implement types**

`GlyphRole.java` — enum with the ten roles.

`GlyphRoles.java`:

```java
public final class GlyphRoles {
    public static GlyphRole of(Label label) {
        return switch (label) {
            case DAMAGE, HEAL, PUSH, SHIELD -> GlyphRole.EFFECT;
            case PHYSICAL, FIRE, FROST, ARCANE -> GlyphRole.SCHOOL;
            case SELF, TARGET -> GlyphRole.PATIENT;
            case ATTACKER -> GlyphRole.PATIENT;
            case SHARPNESS -> GlyphRole.CHARM;
            case TARGET_RAY -> GlyphRole.AIM;
            case ON_HIT, ON_HURT, ON_USE, PERIODIC -> GlyphRole.TRIGGER;
            case IF_HEALTH, IF_UNDEAD, IF_OUTDOORS -> GlyphRole.CONDITION;
            case AREA, REPEAT -> GlyphRole.SCOPE;
            case COOLDOWN, CHARGES -> GlyphRole.LIMIT;
            case REJECT -> GlyphRole.BOTTOM;
        };
    }
    public static boolean hasPips(GlyphRole role) {
        return role == GlyphRole.EFFECT || role == GlyphRole.CHARM;
    }
    public static boolean grammatical(GlyphRole role) {
        return role == GlyphRole.EFFECT || role == GlyphRole.SCHOOL
                || role == GlyphRole.PATIENT || role == GlyphRole.CHARM;
    }
    private GlyphRoles() {}
}
```

`attacker` is PATIENT in the table but **not grammatical** for v1. `grammatical(PATIENT)` is true; the compiler treats `ATTACKER` as reserved (`G0102`) separately.

```java
public static boolean reserved(Label label) {
    if (label == Label.ATTACKER || label == Label.SHIELD) return label == Label.ATTACKER;
    var role = of(label);
    return role != GlyphRole.EFFECT && role != GlyphRole.SCHOOL
            && role != GlyphRole.PATIENT && role != GlyphRole.CHARM;
}
```

`shield` is grammatical as an effect; compile later returns `G0111`. `attacker` is reserved.

`GlyphToken`:

```java
public record GlyphToken(Label label, int pips) {
    public GlyphToken {
        if (label == null) throw new IllegalArgumentException("label");
        if (pips < 1 || pips > 5) throw new IllegalArgumentException("pips");
        if (!GlyphRoles.hasPips(GlyphRoles.of(label)) && pips != 1) {
            throw new IllegalArgumentException("pips");
        }
    }
    public GlyphRole role() { return GlyphRoles.of(label); }
}
```

`ManaTable`:

```java
public final class ManaTable {
    public static ManaTable v1() { return new ManaTable(); }
    public int mana(GlyphToken token) {
        return switch (token.role()) {
            case EFFECT -> 2 + token.pips();
            case SCHOOL -> 2;
            case PATIENT -> 1;
            case CHARM -> 2 + 2 * token.pips();
            default -> 0;
        };
    }
    private ManaTable() {}
}
```

Add to `Label.java`: `SHARPNESS("sharpness")`. Do not change training catalog JSON.

- [ ] **Step 4: Run tests**

Run: `./gradlew :api:test :common:test --rerun-tasks`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/dev/mintychochip/wizardry/api/ml/Label.java \
  api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphRole.java \
  api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphRoles.java \
  api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphToken.java \
  api/src/main/java/dev/mintychochip/wizardry/api/glyph/ManaTable.java \
  common/src/test/java/dev/mintychochip/wizardry/common/glyph/GlyphLanguageTest.java
git commit -m "feat: add Glyphcraft token roles and mana table"
```

---

### Task 2: GlyphCompiler — grammar, tape, identity

**Files:**
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/CharmBind.java`
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphCompiler.java`
- Create: `common/src/main/java/dev/mintychochip/wizardry/common/glyph/GlyphCompilerImpl.java`
- Modify: `common/src/test/java/dev/mintychochip/wizardry/common/glyph/GlyphLanguageTest.java`

- [ ] **Step 1: Write failing compiler tests** (append to `GlyphLanguageTest`)

```java
@Test void fireAndDamageFiveBurnsTarget() {
    var result = GlyphCompiler.INSTANCE.compile(List.of(
            new GlyphToken(Label.FIRE, 1),
            new GlyphToken(Label.DAMAGE, 5)));
    var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
    assertEquals("glyph-compiler/0.1", spell.compilerVersion());
    assertEquals("9e71957a3a06e4e66f8cc9d0ac35485c967843d91b042ad99a1a70517464f8fb",
            spell.identitySha256());
    assertEquals(2, spell.actions().size());
    assertInstanceOf(Action.LookAhead.class, spell.actions().get(0));
    var burn = assertInstanceOf(Action.Burn.class, spell.actions().get(1));
    assertEquals(Action.Patient.TARGET, burn.patient());
    assertEquals(5.0, burn.amount());
}

@Test void pageOrderDoesNotChangeIdentity() {
    var a = GlyphCompiler.INSTANCE.compile(List.of(
            new GlyphToken(Label.DAMAGE, 5), new GlyphToken(Label.FIRE, 1)));
    var b = GlyphCompiler.INSTANCE.compile(List.of(
            new GlyphToken(Label.FIRE, 1), new GlyphToken(Label.DAMAGE, 5)));
    assertEquals(
            ((CompileResult.Ok) a).spell().identitySha256(),
            ((CompileResult.Ok) b).spell().identitySha256());
}

@Test void healSelfHasNoLookAhead() {
    var result = GlyphCompiler.INSTANCE.compile(List.of(
            new GlyphToken(Label.HEAL, 1), new GlyphToken(Label.SELF, 1)));
    var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
    assertEquals("b1677feaea78c242a11508f5aab0e1afb3d72de57242f685de42e648e12d5152",
            spell.identitySha256());
    assertEquals(1, spell.actions().size());
    var mend = assertInstanceOf(Action.Mend.class, spell.actions().getFirst());
    assertEquals(Action.Patient.SELF, mend.patient());
}

@Test void pushFiveClampsShoveToThree() {
    var result = GlyphCompiler.INSTANCE.compile(List.of(new GlyphToken(Label.PUSH, 5)));
    var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
    assertEquals("b4078f15c51d789db2324912a80dd454d4676020d83bf6e2500f432a241354c4",
            spell.identitySha256());
    var shove = assertInstanceOf(Action.Shove.class, spell.actions().get(1));
    assertEquals(3.0, shove.amount());
}

@Test void loneFireIsUnfinished() {
    var result = GlyphCompiler.INSTANCE.compile(List.of(new GlyphToken(Label.FIRE, 1)));
    var error = assertInstanceOf(CompileResult.Error.class, result);
    assertEquals("G0107", error.diagnostics().getFirst().code());
}

@Test void twoEffectsAndReservedAndShieldAndCharmMix() {
    assertEquals("G0104", code(Label.DAMAGE, Label.HEAL));
    assertEquals("G0102", code(Label.ON_HIT));
    assertEquals("G0111", code(Label.SHIELD));
    assertEquals("G0108", GlyphCompiler.INSTANCE.compile(List.of(
            new GlyphToken(Label.SHARPNESS, 3),
            new GlyphToken(Label.DAMAGE, 1))) instanceof CompileResult.Error e
            ? e.diagnostics().getFirst().code() : "?");
    assertTrue(GlyphCompiler.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).isPresent());
    assertEquals(3, GlyphCompiler.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).orElseThrow().rank());
    assertTrue(GlyphCompiler.INSTANCE.charm(new GlyphToken(Label.DAMAGE, 1)).isEmpty());
}

@Test void emptyPagesIsG0100() {
    var error = assertInstanceOf(CompileResult.Error.class, GlyphCompiler.INSTANCE.compile(List.of()));
    assertEquals("G0100", error.diagnostics().getFirst().code());
}

private static String code(Label... labels) {
    var tokens = new java.util.ArrayList<GlyphToken>();
    for (var label : labels) tokens.add(new GlyphToken(label, 1));
    var result = GlyphCompiler.INSTANCE.compile(tokens);
    return assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code();
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `./gradlew :common:test --tests dev.mintychochip.wizardry.common.glyph.GlyphLanguageTest --rerun-tasks`

Expected: FAIL — `GlyphCompiler` missing.

- [ ] **Step 3: Implement compiler**

`CharmBind` record: `Label label, int rank`.

`GlyphCompiler` interface in api:

```java
public interface GlyphCompiler {
    GlyphCompiler INSTANCE = dev.mintychochip.wizardry.common.glyph.GlyphCompilerImpl.INSTANCE;
    CompileResult compile(List<GlyphToken> pages);
    Optional<CharmBind> charm(GlyphToken token);
}
```

Avoid api → common cycle: **do not** put `INSTANCE` on the api interface if `api` cannot see `common`. Match `ScribeCompiler`: api has `Compiler`, common has `ScribeCompiler.INSTANCE`. So:

```java
// api
public interface GlyphCompiler {
    CompileResult compile(List<GlyphToken> pages);
    Optional<CharmBind> charm(GlyphToken token);
}
```

```java
// common
public final class GlyphCompilerImpl implements GlyphCompiler {
    public static final GlyphCompilerImpl INSTANCE = new GlyphCompilerImpl();
}
```

Tests call `GlyphCompilerImpl.INSTANCE`.

Implementation sketch:

1. If pages is empty → `G0100`.
2. Copy pages. If size > 3 → `G0103`.
3. For each token: `REJECT` → `G0101`; `GlyphRoles.reserved(label)` or `ATTACKER` → `G0102`; if any `SHARPNESS` and pages.size() != 1 → `G0108`; if only sharpness → `G0108` from `compile` (charm is not a combat compile). `compile` of a lone charm is `G0108`.
4. Count effects / schools / patients among non-charm pages. >1 → `G0104`/`G0105`/`G0106`.
5. If no effect and (school or patient present) → `G0107`. If no effect and no school/patient (should not happen) → `G0100`.
6. If effect is `SHIELD` → `G0111`.
7. Implicit patient = `TARGET` unless a patient page exists. `SELF` → `Action.Patient.SELF`.
8. Build actions: if patient is TARGET, prepend `LookAhead(32)`. Effect `DAMAGE` → `Burn(patient, pips)`. `HEAL` → `Mend`. `PUSH` → `Shove(patient, Math.min(3.0, pips))`.
9. Canonicalize like Scribe (`HexFormat` of `doubleToRawLongBits`, lowercase, LF, no trailing newline). Version `glyph-compiler/0.1`.
10. `charm`: present only for `SHARPNESS`; rank = pips.

Cap diagnostics at 32. Sort by code then message. `Span` for page i is `new Span(0, 0, i + 1, 1)`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :common:test --tests dev.mintychochip.wizardry.common.glyph.GlyphLanguageTest --rerun-tasks`

Expected: PASS. If an identity mismatches, print `spell.canonical()` as UTF-8 and fix the canonicalizer — do not change the golden strings unless the spec's opcode lines were implemented differently; fix the impl to match the spec.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/dev/mintychochip/wizardry/api/glyph/CharmBind.java \
  api/src/main/java/dev/mintychochip/wizardry/api/glyph/GlyphCompiler.java \
  common/src/main/java/dev/mintychochip/wizardry/common/glyph/GlyphCompilerImpl.java \
  common/src/test/java/dev/mintychochip/wizardry/common/glyph/GlyphLanguageTest.java
git commit -m "feat: compile Glyphcraft page tokens to an action tape"
```

---

### Task 3: Pip clicker on GlyphScreen

Pips are screen state. They must not enter `GlyphDraft`.

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphScreen.java`
- Modify: `paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/GlyphScreenModelTest.java`

- [ ] **Step 1: Write failing clicker tests**

```java
@Test void pipClickerStepsAndSneakJumps() {
    var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {}, ignored -> {}, () -> false);
    assertEquals(1, screen.pips());
    screen.stepPips(1);
    assertEquals(2, screen.pips());
    screen.stepPips(1);
    screen.stepPips(1);
    screen.stepPips(1);
    screen.stepPips(1);
    assertEquals(5, screen.pips());
    screen.stepPips(-1);
    assertEquals(4, screen.pips());
    screen.jumpPips(1);
    assertEquals(5, screen.pips());
    screen.jumpPips(-1);
    assertEquals(1, screen.pips());
}

@Test void pipClickerDoesNotTouchDraft() {
    var screen = new GlyphScreen(new GlyphStrokeTracker(), () -> {}, () -> {}, ignored -> {}, () -> false);
    screen.beginStroke(10, 10);
    screen.endStroke();
    int strokes = screen.draft().strokes().size();
    screen.stepPips(1);
    assertEquals(strokes, screen.draft().strokes().size());
}
```

Update every existing `new GlyphScreen(...)` in this test class to pass `ignored -> {}` and `() -> false` if you add those constructor parameters. Keep the 3-arg constructor delegating to the 5-arg one.

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :paper:test --tests dev.mintychochip.wizardry.paper.mapgui.GlyphScreenModelTest --rerun-tasks`

Expected: FAIL — `pips()` missing.

- [ ] **Step 3: Implement clicker state + overlay**

Add fields: `int pips = 1;` and `BooleanSupplier sneaking`.

```java
public int pips() { return pips; }
public void stepPips(int delta) {
    pips = Math.max(1, Math.min(5, pips + delta));
    invalidate();
}
public void jumpPips(int direction) {
    pips = direction < 0 ? 1 : 5;
    invalidate();
}
```

In `build()`, always `Overlay(canvas, pipChrome)` even when the tool menu is closed. `pipChrome` is a bottom `Row`: Button `◀`, five filled/empty pip `Draw`s (display only), Button `▶`. Left button: if `sneaking.getAsBoolean()` then `jumpPips(-1)` else `stepPips(-1)`. Right: `+1`. Buttons `onClick` must not start a stroke — same pattern as the existing menu row `.onClick(() -> {})` on the padding.

When the tool menu is open, stack menu **above** the pip strip (both in the overlay).

Do not rasterize pips in `paintCanvas`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :paper:test --tests dev.mintychochip.wizardry.paper.mapgui.GlyphScreenModelTest --rerun-tasks`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphScreen.java \
  paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/GlyphScreenModelTest.java
git commit -m "feat: add rank pip clicker overlay to the glyph pen"
```

---

### Task 4: Freeze token onto the glyph map

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphDraftStoreAdapter.java`
- Modify: `paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/GlyphDraftStoreAdapterTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`

- [ ] **Step 1: Write failing store tests**

Add methods on the adapter (test what you can without a live `ItemStack` PDC if the module's Paper tests do not boot a server). Prefer a package-visible codec:

```java
@Test void tokenCodecRoundTrips() {
    var token = new GlyphToken(Label.DAMAGE, 5);
    var encoded = GlyphDraftStoreAdapter.encodeToken(token, ManaTable.v1());
    assertEquals("damage", encoded.label());
    assertEquals(5, encoded.pips());
    assertEquals(7, encoded.mana());
}
```

Use a small record `FrozenToken(String label, int pips, int mana)`.

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement freeze keys and classify-save path**

Namespaced keys: `glyph_label` (STRING), `glyph_pips` (INTEGER), `glyph_mana` (INTEGER).

`saveToken(ItemStack, UUID, GlyphToken)` writes those keys and lore `damage ●●●●●` / `mana 7`. `loadToken(ItemStack)` returns `Optional<GlyphToken>` only if label parses and pips are legal.

On classify accept, `GlyphCommand` keeps the last `Label` on the screen (add `GlyphScreen.setPendingLabel`). Save writes draft **and** `new GlyphToken(label, screen.pips())` if a pending accepted label exists. If the label has no pip grammar, save `pips=1` regardless of the clicker.

Add `/glyph stamp <label> [pips]` (permission `wizardry.glyph.draw`) that creates a frozen map **without** classification so sharpness can be tested before the catalog has that class. Allowed labels: any `GlyphRoles.grammatical` label. Reject reserved. This is a testing/authoring escape hatch documented in `docs/glyphcraft-language.md`.

- [ ] **Step 4: Run** `./gradlew :paper:test --rerun-tasks` — PASS.

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphDraftStoreAdapter.java \
  paper/src/test/java/dev/mintychochip/wizardry/paper/mapgui/GlyphDraftStoreAdapterTest.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphScreen.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java
git commit -m "feat: freeze classified glyph tokens onto map items"
```

---

### Task 5: Tome — book of pages

**Files:**
- Create: `api/src/main/java/dev/mintychochip/wizardry/api/glyph/TomePages.java`
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/tome/GlyphTomeStore.java`
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/tome/GlyphTomeListener.java`
- Create: `common/src/test/java/dev/mintychochip/wizardry/common/glyph/TomePagesTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`
- Modify: `paper/src/main/resources/paper-plugin.yml`

- [ ] **Step 1: Write failing page-list tests** (pure Java, no Bukkit)

```java
@Test void insertSecondEffectIsRejected() {
    var empty = TomePages.empty();
    var once = empty.insert(new GlyphToken(Label.DAMAGE, 1));
    assertTrue(once.isPresent());
    assertTrue(once.get().insert(new GlyphToken(Label.HEAL, 1)).isEmpty());
}

@Test void loneFireMaySitUnfinished() {
    var pages = TomePages.empty().insert(new GlyphToken(Label.FIRE, 1)).orElseThrow();
    assertEquals(1, pages.tokens().size());
    assertInstanceOf(CompileResult.Error.class, GlyphCompilerImpl.INSTANCE.compile(pages.tokens()));
}

@Test void fireThenDamageIsCastable() {
    var pages = TomePages.empty()
            .insert(new GlyphToken(Label.FIRE, 1)).orElseThrow()
            .insert(new GlyphToken(Label.DAMAGE, 5)).orElseThrow();
    assertInstanceOf(CompileResult.Ok.class, GlyphCompilerImpl.INSTANCE.compile(pages.tokens()));
}

@Test void charmWillNotInsert() {
    assertTrue(TomePages.empty().insert(new GlyphToken(Label.SHARPNESS, 5)).isEmpty());
}

@Test void tearRemovesPage() {
    var pages = TomePages.empty().insert(new GlyphToken(Label.DAMAGE, 1)).orElseThrow();
    var torn = pages.tear(0);
    assertEquals(0, torn.pages().tokens().size());
    assertEquals(Label.DAMAGE, torn.torn().label());
}
```

`TomePages.insert` returns empty when:

- already 3 pages (`G0103`)
- compiler would return `G0104`, `G0105`, `G0106`, `G0108`, `G0101`, `G0102`, `G0110`, `G0109`
- **not** when the only error is `G0107` (unfinished is allowed while building)
- charm tokens never insert

`tear(index)` returns `Torn(TomePages pages, GlyphToken torn)`.

- [ ] **Step 2: Run — expect FAIL**

Run: `./gradlew :common:test --tests dev.mintychochip.wizardry.common.glyph.TomePagesTest --rerun-tasks`

- [ ] **Step 3: Implement `TomePages` and Paper store**

`TomePages` is an immutable `List<GlyphToken>` (draft bytes stay on Paper).

`GlyphTomeStore`:

- `createTome()` → `Material.BOOK` + `glyph_tome` + `glyph_tome_id` + empty page list + `glyph_tome_index` = 0
- `isTome` / not `scribe_book`
- serialize pages as UTF-8 lines `label|pips|mana` plus draft blobs in parallel PDC keys `glyph_tome_draft_0` …
- `insert(tome, map, sneak)` copies token from the map; if sneak, caller consumes the map
- `tear(tome)` → new glyph map via `store.createGlyphItem()` + draft + token
- `flip(tome, delta)` wraps the current index

Listener: player uses a frozen glyph map on a tome (right-click while tome in offhand **or** `/glyph bind`). Simplest v1: `/glyph bind` with tome in offhand, map in main hand. `/glyph tear` tears the current page into the cursor. `/glyph tome` gives an empty tome.

Register permission `wizardry.glyph.tome` default true.

Scribe book listener must ignore `glyph_tome` books.

- [ ] **Step 4: Run** `./gradlew :common:test :paper:test --rerun-tasks` — PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/dev/mintychochip/wizardry/api/glyph/TomePages.java \
  common/src/test/java/dev/mintychochip/wizardry/common/glyph/TomePagesTest.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/tome \
  paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java \
  paper/src/main/resources/paper-plugin.yml
git commit -m "feat: bind glyph maps into a tome as pages"
```

---

### Task 6: Cast tomes and bind Sharpness

**Files:**
- Create: `paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/CharmBinder.java`
- Create: `paper/src/test/java/dev/mintychochip/wizardry/paper/runtime/CharmBinderTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java`
- Modify: `paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java`

- [ ] **Step 1: Write failing binder tests**

Keep logic pure so tests do not need a live sword:

```java
@Test void sharpnessThreeIsLevelThree() {
    var bind = GlyphCompilerImpl.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).orElseThrow();
    assertEquals(3, CharmBinder.level(bind));
    assertTrue(CharmBinder.canHost(Material.DIAMOND_SWORD));
    assertFalse(CharmBinder.canHost(Material.WOODEN_HOE));
    assertFalse(CharmBinder.canHost(Material.BOOK));
}
```

`/glyph cast` path: compile `tome.tokens()`; on `Ok`, reuse the plugin's existing target-lookup + `SpellRuntime.cast`. On `Error`, send the first diagnostic. Do not spend mana.

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: Implement**

`CharmBinder.canHost` is true for `*SWORD` materials only. `apply` (Paper, not unit-tested live) sets `Enchantment.SHARPNESS` to `bind.rank()` if `canHost` and existing level < rank; consumes the map.

`/glyph cast`: main-hand tome → `GlyphCompilerImpl.INSTANCE.compile` → same cast callback as Scribe (`LookAhead` range default 32, `getTargetEntity`).

`/glyph enchant`: main-hand sharpness map, offhand or look-at is not required — apply to the **offhand** sword. Fail if offhand is not a sword.

- [ ] **Step 4: Run** `./gradlew :common:test :paper:test --rerun-tasks` — PASS.

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/wizardry/paper/runtime/CharmBinder.java \
  paper/src/test/java/dev/mintychochip/wizardry/paper/runtime/CharmBinderTest.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/mapgui/GlyphCommand.java \
  paper/src/main/java/dev/mintychochip/wizardry/paper/WizardryPlugin.java
git commit -m "feat: cast glyph tomes and bind sharpness maps to swords"
```

---

### Task 7: Player-facing language doc

**Files:**
- Create: `docs/glyphcraft-language.md`
- Modify: `README.md` (one sentence pointing at the doc)

- [ ] **Step 1: Write** `docs/glyphcraft-language.md` covering: one map = one glyph; pips clicker; freeze; tome pages; implicits; reserved words; `/glyph` commands; sharpness bind; mana is printed not spent.

- [ ] **Step 2: Commit**

```bash
git add docs/glyphcraft-language.md README.md
git commit -m "docs: add player-facing Glyphcraft tome language"
```

---

## Self-review

**Spec coverage**

| Spec requirement | Task |
|---|---|
| Role table over 24 labels + sharpness charm | 1 |
| Token pips + mana table | 1 |
| Combat compile + implicits + identities | 2 |
| Charm bind data | 2 |
| Pip overlay, not ink | 3 |
| Freeze token on map | 4 |
| Tome insert/tear, unfinished allowed, charm excluded | 5 |
| Cast via SpellRuntime | 6 |
| Sharpness on swords only | 6 |
| Player-facing doc | 7 |
| No EnchantGraph / no mana spend / no multi-glyph crop | honored (not scheduled) |

**Placeholders:** none remaining. `/glyph stamp` is an explicit authoring hatch, not a TBD.

**Type names:** `GlyphToken`, `GlyphRole`, `ManaTable`, `GlyphCompiler` / `GlyphCompilerImpl`, `CharmBind`, `TomePages`, `CharmBinder`, `FrozenToken` — used consistently.
