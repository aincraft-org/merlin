package dev.mintychochip.merlin.common.ml;

import dev.mintychochip.merlin.api.ml.*;
import dev.mintychochip.merlin.api.glyph.*;
import dev.mintychochip.merlin.common.glyph.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class MlContractsTest {
    @Test void preprocessingShapeOrderAndMasks() {
        var d = new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(0, 0), new GlyphPoint(127, 127)), 2, 0)));
        var p = new GlyphPreprocessor().preprocess(d);
        assertEquals(64, p.vectors().length);
        assertEquals(32, p.vectors()[0].length);
        assertEquals(8, p.vectors()[0][0].length);
        assertEquals(3, p.raster().length);
        assertTrue(p.mask()[0][0]);
        assertEquals(1f, p.vectors()[0][0][6]);
        assertEquals(1f, p.vectors()[0][31][4]);
        assertTrue(ink(p.raster(), 63, 63) > 0);
    }

    @Test void rasterIsConnectedBrushBitImageNotResampledDots() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(8, 32), new GlyphPoint(120, 32)), 6, 0)));
        var raster = new GlyphPreprocessor().preprocess(draft).raster();
        int best = 0, bestInk = -1;
        for (int y = 0; y < 64; y++) {
            int inked = 0;
            for (int x = 0; x < 64; x++) if (ink(raster, x, y) > 0) inked++;
            if (inked > bestInk) {
                bestInk = inked;
                best = y;
            }
        }
        int first = -1, last = -1, ink = 0;
        for (int x = 0; x < 64; x++) if (ink(raster, x, best) > 0) {
            if (first < 0) first = x;
            last = x;
            ink++;
        }
        assertTrue(ink >= 50, "expected a solid brush shaft, got " + ink + " pixels");
        assertTrue(first <= 5 && last >= 58);
        assertEquals(last - first + 1, ink);
    }

    @Test void thickerBrushCoversMoreRasterPixels() {
        var thin = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 64), new GlyphPoint(100, 64)), 2, 0)))).raster();
        var thick = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 64), new GlyphPoint(100, 64)), 8, 0)))).raster();
        assertTrue(countInk(thick) > countInk(thin));
    }

    @Test void onePointStrokeRasterIsRoundDab() {
        var raster = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(64, 64)), 6, 0)))).raster();
        assertArrayEquals(GlyphElement.PHYSICAL.rgb(), new float[] {raster[0][32][32], raster[1][32][32], raster[2][32][32]}, 1e-5f);
        assertTrue(ink(raster, 31, 32) > 0);
        assertEquals(0f, ink(raster, 0, 0));
    }

    @Test void fireStrokeKeepsEmberChannel() {
        var raster = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(
                new GlyphStroke(List.of(new GlyphPoint(64, 64)), 6, 0, List.of(), GlyphElement.FLAME)))).raster();
        assertArrayEquals(GlyphElement.FLAME.rgb(), new float[] {raster[0][32][32], raster[1][32][32], raster[2][32][32]}, 1e-5f);
    }

    @Test void translatedGlyphHasTheSameModelFeatures() {
        var center = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 32), new GlyphPoint(108, 32)), 6, 0))));
        var shifted = new GlyphPreprocessor().preprocess(new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(20, 96), new GlyphPoint(108, 96)), 6, 0))));
        for (int c = 0; c < 3; c++) {
            assertArrayEquals(center.raster()[c][32], shifted.raster()[c][32]);
            for (int y = 0; y < 64; y++) assertArrayEquals(center.raster()[c][y], shifted.raster()[c][y]);
        }
        assertEquals(center.vectors()[0][0][0], shifted.vectors()[0][0][0], 1e-5);
        assertEquals(center.vectors()[0][0][1], shifted.vectors()[0][0][1], 1e-5);
    }

    @Test void emptyRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GlyphPreprocessor().preprocess(GlyphDraft.empty()));
    }

    @Test void corruptBundleRejected() throws Exception {
        Path d = Files.createTempDirectory("bundle");
        Files.writeString(d.resolve("manifest.json"), "{}");
        assertThrows(ModelBundle.BundleException.class, () -> ModelBundle.load(d));
    }

    private static float ink(float[][][] raster, int x, int y) {
        return Math.max(raster[0][y][x], Math.max(raster[1][y][x], raster[2][y][x]));
    }

    private static int countInk(float[][][] raster) {
        int ink = 0;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if (ink(raster, x, y) > 0) ink++;
        return ink;
    }
}
