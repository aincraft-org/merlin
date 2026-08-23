package dev.mintychochip.wizardry.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class GlyphMapRehydrationListenerTest {
    @Test void currentAndNewHeldSlotsAreSelectedForRehydration() {
        assertArrayEquals(new int[] {4}, GlyphMapRehydrationListener.slotsToRestore(4, -1));
        assertArrayEquals(new int[] {7}, GlyphMapRehydrationListener.slotsToRestore(4, 7));
    }
}
