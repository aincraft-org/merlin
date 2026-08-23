package dev.mintychochip.wizardry.common.glyph;
import dev.mintychochip.wizardry.api.glyph.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GlyphTest {
    @Test void rejectsInvalidPointsAndCopiesStrokes() {
        assertThrows(IllegalArgumentException.class, () -> new GlyphPoint(Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new GlyphPoint(128, 1));
        var points = new java.util.ArrayList<>(List.of(new GlyphPoint(2, 2), new GlyphPoint(8, 8)));
        var stroke = new GlyphStroke(points, 1, 0);
        points.clear();
        assertEquals(2, stroke.points().size());
    }
    @Test void rasterizesFullAndNormalizedBitmapsDeterministically() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(2, 2), new GlyphPoint(8, 8)), 1, 0)));
        var full = GlyphRasterizer.renderFull(draft);
        var normalized = GlyphRasterizer.renderNormalized(draft, 32, 2);
        assertEquals(128 * 128, full.pixels().length);
        assertEquals(32 * 32, normalized.pixels().length);
        assertArrayEquals(normalized.pixels(), GlyphRasterizer.renderNormalized(draft, 32, 2).pixels());
    }
    @Test void mixedSegmentWidthsProduceDifferentLocalThickness() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(20, 20), new GlyphPoint(40, 20), new GlyphPoint(60, 20)),
                1, 0, List.of(6.0, 1.0))));
        var pixels = GlyphRasterizer.renderFull(draft).pixels();
        int wide = 0;
        for (int y = 0; y < 128; y++) for (int x = 20; x <= 40; x++) if (pixels[y * 128 + x] != 0) wide++;
        int narrow = 0;
        for (int y = 0; y < 128; y++) for (int x = 41; x <= 60; x++) if (pixels[y * 128 + x] != 0) narrow++;
        assertTrue(wide > narrow);
    }
    @Test void narrowFinalWidthFormsAVisibleTaperedTip() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(20, 20), new GlyphPoint(50, 20)),
                6, 0, List.of(1.0))));
        var pixels = GlyphRasterizer.renderFull(draft).pixels();
        int startThickness = 0;
        for (int y = 0; y < 128; y++) if (pixels[y * 128 + 20] != 0) startThickness++;
        int tipThickness = 0;
        for (int y = 0; y < 128; y++) if (pixels[y * 128 + 50] != 0) tipThickness++;
        assertTrue(startThickness > tipThickness);
    }
    @Test void stationaryDabProducesOneRoundFootprintWithoutTail() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(64, 64)), 6, 0)));
        var pixels = GlyphRasterizer.renderFull(draft).pixels();

        assertInk(pixels, 64, 64);
        assertInk(pixels, 61, 64);
        assertInk(pixels, 67, 64);
        assertInk(pixels, 64, 61);
        assertInk(pixels, 64, 67);
        assertBlank(pixels, 68, 64);
        assertBlank(pixels, 64, 68);
    }
    @Test void finalFootprintUsesFinalVelocityDerivedWidth() {
        var slow = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(20, 30), new GlyphPoint(40, 30), new GlyphPoint(60, 30)),
                4, 0, List.of(4.0, 6.0))));
        var fast = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(20, 50), new GlyphPoint(40, 50), new GlyphPoint(60, 50)),
                4, 0, List.of(4.0, 1.0))));

        var slowPixels = GlyphRasterizer.renderFull(slow).pixels();
        var fastPixels = GlyphRasterizer.renderFull(fast).pixels();
        assertTrue(columnInk(slowPixels, 60) > columnInk(fastPixels, 60));
    }
    @Test void sparseDiagonalWithChangingRadiusRemainsContinuous() {
        var draft = new GlyphDraft(List.of(new GlyphStroke(
                List.of(new GlyphPoint(16, 16), new GlyphPoint(96, 96)),
                6, 0, List.of(1.0))));
        var pixels = GlyphRasterizer.renderFull(draft).pixels();

        for (int coordinate = 16; coordinate <= 96; coordinate++) {
            assertTrue(hasInkNear(pixels, coordinate, coordinate));
        }
    }
    @Test void strokeDefaultsToPhysicalInk() {
        var stroke = new GlyphStroke(List.of(new GlyphPoint(2, 2)), 1, 0);
        assertEquals(GlyphElement.PHYSICAL, stroke.element());
    }

    @Test void overlappingElementStampsComputeMixedRgb() {
        var fire = new GlyphStroke(List.of(new GlyphPoint(64, 64)), 8, 0, List.of(), GlyphElement.FIRE);
        var frost = new GlyphStroke(List.of(new GlyphPoint(67, 64)), 8, 0, List.of(), GlyphElement.FROST);
        var rgb = GlyphRasterizer.renderFullRgb(new GlyphDraft(List.of(fire, frost)));
        float[] centerFire = pixel(rgb, 61, 64);
        float[] centerFrost = pixel(rgb, 70, 64);
        assertArrayEquals(GlyphElement.FIRE.rgb(), centerFire, 1e-5f);
        assertArrayEquals(GlyphElement.FROST.rgb(), centerFrost, 1e-5f);
        float[] overlap = pixel(rgb, 63, 64);
        assertTrue(overlap[0] > 0 && overlap[0] < 1);
        assertTrue(overlap[2] > 0);
        assertNotEquals(GlyphElement.FIRE.r(), overlap[0], 1e-3);
    }

    private static float[] pixel(float[] rgb, int x, int y) {
        int i = (y * 128 + x) * 3;
        return new float[] {rgb[i], rgb[i + 1], rgb[i + 2]};
    }

    @Test void captureSupportsUndoAndClear() {
        var capture = new GlyphCaptureSession();
        capture.beginStroke(1); capture.appendPoint(new GlyphPoint(1, 1)); capture.appendPoint(new GlyphPoint(2, 2)); capture.endStroke();
        assertEquals(1, capture.snapshot().strokes().size());
        capture.undo(); assertTrue(capture.snapshot().strokes().isEmpty());
        capture.clear(); assertTrue(capture.close().strokes().isEmpty());
    }
    private static void assertInk(byte[] pixels, int x, int y) {
        assertNotEquals(0, pixels[y * 128 + x] & 0xff);
    }
    private static void assertBlank(byte[] pixels, int x, int y) {
        assertEquals(0, pixels[y * 128 + x] & 0xff);
    }
    private static int columnInk(byte[] pixels, int x) {
        int count = 0;
        for (int y = 0; y < 128; y++) if ((pixels[y * 128 + x] & 0xff) != 0) count++;
        return count;
    }
    private static boolean hasInkNear(byte[] pixels, int cx, int cy) {
        for (int y = Math.max(0, cy - 1); y <= Math.min(127, cy + 1); y++)
            for (int x = Math.max(0, cx - 1); x <= Math.min(127, cx + 1); x++)
                if ((pixels[y * 128 + x] & 0xff) != 0) return true;
        return false;
    }
    private static GlyphDraft slash(double start) { return new GlyphDraft(List.of(new GlyphStroke(List.of(new GlyphPoint(start, start), new GlyphPoint(start + 6, start + 6)), 1, 0))); }
}
