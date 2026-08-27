package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class InkFlameTest {
    @Test void flameFlickersAcrossPhase() {
        int[] a = InkFlame.pixels(0.0);
        int[] b = InkFlame.pixels(0.45);
        int[] c = InkFlame.pixels(0.8);
        assertFalse(java.util.Arrays.equals(a, b));
        assertFalse(java.util.Arrays.equals(a, c));
        assertFalse(java.util.Arrays.equals(b, c));
    }

    @Test void framesAreSharedByCache() {
        int[] a = InkFlame.pixels(0.2);
        int[] b = InkFlame.pixels(0.2);
        assertSame(a, b);
    }

    @Test void frameCountMatchesPipFlame() {
        assertEquals(PipFlame.FRAME_COUNT, InkFlame.FRAME_COUNT);
    }

    @Test void flameIsOrangeRedOrb() {
        int[] pixels = InkFlame.pixels(0.0);
        int lit = 0;
        for (int pixel : pixels) {
            if (pixel == 0) continue;
            lit++;
            int r = (pixel >> 16) & 255;
            int g = (pixel >> 8) & 255;
            int b = pixel & 255;
            boolean hotCore = r >= 240 && g >= 180;
            boolean flame = r >= 200 && r >= g && b <= 80;
            assertTrue(hotCore || flame, "expected orange/red, got " + r + "," + g + "," + b);
        }
        assertTrue(lit >= 12, "orb should fill a round blob, lit=" + lit);
    }
}
