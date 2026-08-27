package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import org.junit.jupiter.api.Test;

final class InkStoreTest {
    @Test void parseAcceptsElementNames() {
        assertEquals(GlyphElement.PHYSICAL, InkStore.parseElement("physical").orElseThrow());
        assertEquals(GlyphElement.FLAME, InkStore.parseElement("flame").orElseThrow());
        assertEquals(GlyphElement.FLAME, InkStore.parseElement("fire").orElseThrow());
        assertEquals(GlyphElement.FROST, InkStore.parseElement("frost").orElseThrow());
        assertEquals(GlyphElement.ARCANE, InkStore.parseElement("arcane").orElseThrow());
        assertTrue(InkStore.parseElement("ember").isEmpty());
        assertTrue(InkStore.parseElement("nope").isEmpty());
        assertTrue(InkStore.parseElement(null).isEmpty());
    }

    @Test void displayNamesAreElementInks() {
        assertEquals("Physical Ink", InkStore.displayName(GlyphElement.PHYSICAL));
        assertEquals("Flame Ink", InkStore.displayName(GlyphElement.FLAME));
        assertEquals("Frost Ink", InkStore.displayName(GlyphElement.FROST));
        assertEquals("Arcane Ink", InkStore.displayName(GlyphElement.ARCANE));
    }

    @Test void tintMatchesElementPalette() {
        assertEquals(org.bukkit.Color.fromRGB(232, 228, 217), InkStore.tint(GlyphElement.PHYSICAL));
        assertEquals(org.bukkit.Color.fromRGB(255, 77, 0), InkStore.tint(GlyphElement.FLAME));
        assertEquals(org.bukkit.Color.fromRGB(61, 220, 255), InkStore.tint(GlyphElement.FROST));
        assertEquals(org.bukkit.Color.fromRGB(180, 74, 255), InkStore.tint(GlyphElement.ARCANE));
    }
}
