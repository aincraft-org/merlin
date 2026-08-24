package dev.mintychochip.merlin.api.glyph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GlyphElementTest {
    @Test void paletteMatchesLockedHex() {
        assertArrayEquals(new float[] {1f, 77 / 255f, 0f}, GlyphElement.FIRE.rgb(), 1e-6f);
        assertArrayEquals(new float[] {61 / 255f, 220 / 255f, 1f}, GlyphElement.FROST.rgb(), 1e-6f);
        assertArrayEquals(new float[] {180 / 255f, 74 / 255f, 1f}, GlyphElement.ARCANE.rgb(), 1e-6f);
        assertArrayEquals(new float[] {232 / 255f, 228 / 255f, 217 / 255f}, GlyphElement.PHYSICAL.rgb(), 1e-6f);
    }

    @Test void blendIsCoverageMix() {
        float[] pixel = {0f, 0f, 0f};
        GlyphElement.blend(pixel, 0, GlyphElement.FIRE, 1f);
        assertArrayEquals(GlyphElement.FIRE.rgb(), pixel, 1e-6f);
        GlyphElement.blend(pixel, 0, GlyphElement.FROST, 0.5f);
        assertEquals((GlyphElement.FIRE.r() + GlyphElement.FROST.r()) / 2f, pixel[0], 1e-5f);
        assertEquals((GlyphElement.FIRE.g() + GlyphElement.FROST.g()) / 2f, pixel[1], 1e-5f);
        assertEquals((GlyphElement.FIRE.b() + GlyphElement.FROST.b()) / 2f, pixel[2], 1e-5f);
    }

    @Test void coverageIsSoftDisk() {
        assertEquals(1f, GlyphElement.coverage(0, 3));
        assertEquals(0f, GlyphElement.coverage(10, 3));
        float edge = GlyphElement.coverage(3.2, 3);
        assertEquals(0.3f, edge, 1e-5f);
    }
}
