package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PipFlameTest {
    @Test void litFlameDiffersFromUnlitWick() {
        int[] lit = PipFlame.pixels(true, 0);
        int[] unlit = PipFlame.pixels(false, 0);
        assertNotEquals(java.util.Arrays.hashCode(lit), java.util.Arrays.hashCode(unlit));
        assertTrue(PipFlame.litPixelCount(lit) > PipFlame.litPixelCount(unlit));
    }

    @Test void litFlameFlickersAcrossPhase() {
        int[] a = PipFlame.pixels(true, 0.0);
        int[] b = PipFlame.pixels(true, 0.45);
        int[] c = PipFlame.pixels(true, 0.8);
        assertFalse(java.util.Arrays.equals(a, b));
        assertFalse(java.util.Arrays.equals(a, c));
        assertFalse(java.util.Arrays.equals(b, c));
    }

    @Test void litPipsReuseTheSameFrameInstance() {
        int[] a = PipFlame.pixels(true, 0.3);
        int[] b = PipFlame.pixels(true, 0.3);
        assertSame(a, b);
        assertSame(PipFlame.pixels(false, 0.1), PipFlame.pixels(false, 0.9));
        assertNotSame(a, PipFlame.pixels(false, 0.3));
    }

    @Test void animationHasTwentySharedFrames() {
        assertEquals(20, PipFlame.frameCount());
        var seen = new java.util.HashSet<Integer>();
        for (int i = 0; i < 20; i++) {
            seen.add(System.identityHashCode(PipFlame.pixels(true, (i + 0.01) / 20.0)));
        }
        assertEquals(20, seen.size());
    }

    @Test void litFlameIsOrangeRedOrb() {
        for (int i = 0; i < PipFlame.frameCount(); i++) {
            int[] pixels = PipFlame.pixels(true, i / 20.0);
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
                assertTrue(Math.abs(r - g) > 12 || r >= 240, "no brown " + r + "," + g + "," + b);
            }
            assertTrue(lit >= 20, "orb should fill a round blob, lit=" + lit);
        }
    }
}
