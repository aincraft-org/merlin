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
}
