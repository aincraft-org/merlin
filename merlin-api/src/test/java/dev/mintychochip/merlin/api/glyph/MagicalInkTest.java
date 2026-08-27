package dev.mintychochip.merlin.api.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MagicalInkTest {
    @Test void fullFireHasDefaultRemaining() {
        var ink = MagicalInk.full(GlyphElement.FLAME);
        assertEquals(GlyphElement.FLAME, ink.element());
        assertEquals(64, ink.remaining());
        assertEquals(64, ink.max());
        assertFalse(ink.empty());
    }

    @Test void spendDropsRemainingAndKeepsElement() {
        var spent = MagicalInk.full(GlyphElement.FROST).spend(1).orElseThrow();
        assertEquals(GlyphElement.FROST, spent.element());
        assertEquals(63, spent.remaining());
        assertEquals(64, spent.max());
    }

    @Test void spendOnEmptyIsRejected() {
        var empty = new MagicalInk(GlyphElement.ARCANE, 0, 64);
        assertTrue(empty.empty());
        assertTrue(empty.spend(1).isEmpty());
        assertEquals(0, empty.remaining());
    }

    @Test void filledElementsAreChipOrderWithoutEmptiesOrDuplicates() {
        var filled = MagicalInk.filledElements(java.util.List.of(
                MagicalInk.full(GlyphElement.FLAME),
                new MagicalInk(GlyphElement.PHYSICAL, 0, 64),
                MagicalInk.full(GlyphElement.FLAME),
                MagicalInk.full(GlyphElement.FROST)));
        assertEquals(java.util.List.of(GlyphElement.FLAME, GlyphElement.FROST), filled);
        assertEquals(
                java.util.List.of(GlyphElement.PHYSICAL, GlyphElement.ARCANE),
                MagicalInk.filledElements(java.util.List.of(
                        MagicalInk.full(GlyphElement.ARCANE), MagicalInk.full(GlyphElement.PHYSICAL))));
    }

    @Test void pickPrefersOffhandWhenFilledOtherwiseFirstChip() {
        var filled = java.util.List.of(GlyphElement.FLAME, GlyphElement.FROST);
        assertEquals(GlyphElement.FROST, MagicalInk.pick(GlyphElement.FROST, filled).orElseThrow());
        assertEquals(GlyphElement.FLAME, MagicalInk.pick(GlyphElement.PHYSICAL, filled).orElseThrow());
        assertEquals(GlyphElement.FLAME, MagicalInk.pick(null, filled).orElseThrow());
        assertTrue(MagicalInk.pick(null, java.util.List.of()).isEmpty());
    }

    @Test void afterSpendKeepsSelectionOrMovesToNextChip() {
        assertEquals(GlyphElement.FROST, MagicalInk.afterSpend(GlyphElement.FLAME, java.util.List.of(GlyphElement.FROST)).orElseThrow());
        assertEquals(GlyphElement.FLAME, MagicalInk.afterSpend(GlyphElement.FLAME, java.util.List.of(GlyphElement.FLAME, GlyphElement.ARCANE)).orElseThrow());
        assertTrue(MagicalInk.afterSpend(GlyphElement.FLAME, java.util.List.of()).isEmpty());
    }

    @Test void remainingOutsideRangeCannotBeConstructed() {
        assertThrows(IllegalArgumentException.class, () -> new MagicalInk(GlyphElement.PHYSICAL, -1, 64));
        assertThrows(IllegalArgumentException.class, () -> new MagicalInk(GlyphElement.PHYSICAL, 65, 64));
        assertThrows(IllegalArgumentException.class, () -> new MagicalInk(GlyphElement.PHYSICAL, 0, 0));
    }
}
