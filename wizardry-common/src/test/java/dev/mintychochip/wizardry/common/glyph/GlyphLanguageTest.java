package dev.mintychochip.wizardry.common.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.glyph.GlyphRole;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.glyph.ManaTable;
import dev.mintychochip.wizardry.api.ml.Label;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GlyphLanguageTest {
    @Test void everyLabelHasARole() {
        for (var label : Label.values()) {
            assertTrue(GlyphRoles.of(label) != null);
        }
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

    @Test void fireAndDamageFiveBurnsTarget() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(
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
        var a = GlyphCompilerImpl.INSTANCE.compile(List.of(
                new GlyphToken(Label.DAMAGE, 5), new GlyphToken(Label.FIRE, 1)));
        var b = GlyphCompilerImpl.INSTANCE.compile(List.of(
                new GlyphToken(Label.FIRE, 1), new GlyphToken(Label.DAMAGE, 5)));
        assertEquals(
                ((CompileResult.Ok) a).spell().identitySha256(),
                ((CompileResult.Ok) b).spell().identitySha256());
    }

    @Test void healSelfHasNoLookAhead() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(
                new GlyphToken(Label.HEAL, 1), new GlyphToken(Label.SELF, 1)));
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("b1677feaea78c242a11508f5aab0e1afb3d72de57242f685de42e648e12d5152",
                spell.identitySha256());
        assertEquals(1, spell.actions().size());
        var mend = assertInstanceOf(Action.Mend.class, spell.actions().getFirst());
        assertEquals(Action.Patient.SELF, mend.patient());
    }

    @Test void pushFiveClampsShoveToThree() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(new GlyphToken(Label.PUSH, 5)));
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("b4078f15c51d789db2324912a80dd454d4676020d83bf6e2500f432a241354c4",
                spell.identitySha256());
        var shove = assertInstanceOf(Action.Shove.class, spell.actions().get(1));
        assertEquals(3.0, shove.amount());
    }

    @Test void loneFireIsUnfinished() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(new GlyphToken(Label.FIRE, 1)));
        var error = assertInstanceOf(CompileResult.Error.class, result);
        assertEquals("G0107", error.diagnostics().getFirst().code());
    }

    @Test void twoEffectsAndReservedAndShieldAndCharmMix() {
        assertEquals("G0104", code(Label.DAMAGE, Label.HEAL));
        assertEquals("G0102", code(Label.ON_HIT));
        assertEquals("G0111", code(Label.SHIELD));
        assertEquals("G0108", GlyphCompilerImpl.INSTANCE.compile(List.of(
                new GlyphToken(Label.SHARPNESS, 3),
                new GlyphToken(Label.DAMAGE, 1))) instanceof CompileResult.Error e
                ? e.diagnostics().getFirst().code() : "?");
        assertTrue(GlyphCompilerImpl.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).isPresent());
        assertEquals(3, GlyphCompilerImpl.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).orElseThrow().rank());
        assertTrue(GlyphCompilerImpl.INSTANCE.charm(new GlyphToken(Label.DAMAGE, 1)).isEmpty());
    }

    @Test void emptyPagesIsG0100() {
        var error = assertInstanceOf(CompileResult.Error.class, GlyphCompilerImpl.INSTANCE.compile(List.of()));
        assertEquals("G0100", error.diagnostics().getFirst().code());
    }

    @Test void fourPagesIsG0103() {
        assertEquals("G0103", code(Label.DAMAGE, Label.FIRE, Label.SELF, Label.PHYSICAL));
    }

    @Test void twoSchoolsIsG0105() {
        assertEquals("G0105", code(Label.FIRE, Label.FROST, Label.DAMAGE));
    }

    @Test void twoPatientsIsG0106() {
        assertEquals("G0106", code(Label.SELF, Label.TARGET, Label.DAMAGE));
    }

    @Test void damageOneIdentity() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(new GlyphToken(Label.DAMAGE, 1)));
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("95c2ca180d7ce4539a86fea12a49d323926dd04a085d5bf08ab7c27ee04f55a1",
                spell.identitySha256());
    }

    @Test void loneHealTargetsAndLooksAhead() {
        var result = GlyphCompilerImpl.INSTANCE.compile(List.of(new GlyphToken(Label.HEAL, 1)));
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals(2, spell.actions().size());
        assertInstanceOf(Action.LookAhead.class, spell.actions().get(0));
        var mend = assertInstanceOf(Action.Mend.class, spell.actions().get(1));
        assertEquals(Action.Patient.TARGET, mend.patient());
        assertEquals(1.0, mend.amount());
    }

    private static String code(Label... labels) {
        var tokens = new java.util.ArrayList<GlyphToken>();
        for (var label : labels) tokens.add(new GlyphToken(label, 1));
        var result = GlyphCompilerImpl.INSTANCE.compile(tokens);
        return assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code();
    }
}
