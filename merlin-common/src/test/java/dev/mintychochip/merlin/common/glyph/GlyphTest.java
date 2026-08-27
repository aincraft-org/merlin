package dev.mintychochip.merlin.common.glyph;
import dev.mintychochip.merlin.api.glyph.*;
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
        var fire = new GlyphStroke(List.of(new GlyphPoint(64, 64)), 8, 0, List.of(), GlyphElement.FLAME);
        var frost = new GlyphStroke(List.of(new GlyphPoint(67, 64)), 8, 0, List.of(), GlyphElement.FROST);
        var rgb = GlyphRasterizer.renderFullRgb(new GlyphDraft(List.of(fire, frost)));
        float[] centerFire = pixel(rgb, 61, 64);
        float[] centerFrost = pixel(rgb, 70, 64);
        assertArrayEquals(GlyphElement.FLAME.rgb(), centerFire, 1e-5f);
        assertArrayEquals(GlyphElement.FROST.rgb(), centerFrost, 1e-5f);
        float[] overlap = pixel(rgb, 63, 64);
        assertTrue(overlap[0] > 0 && overlap[0] < 1);
        assertTrue(overlap[2] > 0);
        assertNotEquals(GlyphElement.FLAME.r(), overlap[0], 1e-3);
    }
    /** A long, straight flame stroke: the clearest view of a stroke's cross-section. */
    private static GlyphDraft flameBar() {
        return flameBar(8);
    }

    /** A 50px flame bar along y=64, from x=40 to x=90. */
    private static GlyphDraft flameBar(double width) {
        var bar = new GlyphStroke(
                List.of(new GlyphPoint(40, 64), new GlyphPoint(90, 64)),
                width, 0, List.of(width), GlyphElement.FLAME);
        return new GlyphDraft(List.of(bar));
    }

    @Test void flameStrokeKeepsFootprintAndAvoidsHaloOrBrighten() {
        var draft = flameBar();
        var base = GlyphRasterizer.renderFullRgb(draft);
        var flame = GlyphRasterizer.renderEmissiveRgb(draft, 0.0);
        int inked = 0;
        for (int i = 0; i < 128 * 128; i++) {
            int o = i * 3;
            boolean baseInk = base[o] > 0f || base[o + 1] > 0f || base[o + 2] > 0f;
            boolean flameInk = flame[o] > 0f || flame[o + 1] > 0f || flame[o + 2] > 0f;
            assertEquals(baseInk, flameInk, "ink footprint changed at index " + i);
            if (baseInk) inked++;
        }
        assertTrue(inked > 0, "fixture must produce flame ink");
        // No halo: well clear of the 8px-wide bar the canvas stays blank.
        float[] halo = pixel(flame, 64, 78);
        assertEquals(0f, halo[0], 1e-6f);
        assertEquals(0f, halo[1], 1e-6f);
        assertEquals(0f, halo[2], 1e-6f);
        // No brighten: fully covered pixels land exactly on an orb tier.
        for (int y = 61; y <= 67; y++) {
            float[] px = pixel(flame, 64, y);
            assertTrue(isOrbTier(px), "expected an orb tier at y=" + y
                    + ", got " + px[0] + "," + px[1] + "," + px[2]);
        }
    }

    @Test void flameStrokeBurnsHotInsideAndCoolsToBothEdges() {
        var draft = flameBar();
        var flame = GlyphRasterizer.renderEmissiveRgb(draft, 0.0);
        // The pip reads as fire because of concentric tiers, and a stroke must too. The
        // stretched orb lands its core on the spine and cools to either flank, so a
        // column through the bar burns hot in the middle and cooler at both edges.
        int hotColumn = -1;
        for (int x = 41; x <= 89 && hotColumn < 0; x++) {
            if (tierAt(flame, x, 64) == FlameOrb.Tier.CORE
                    && tierAt(flame, x, 61).ordinal() > FlameOrb.Tier.CORE.ordinal()
                    && tierAt(flame, x, 67).ordinal() > FlameOrb.Tier.CORE.ordinal()) hotColumn = x;
        }
        assertTrue(hotColumn > 0, "no column burns at core on the spine and cools to both edges");
        // Several distinct tiers are present; a flat tint would show only one.
        var seen = new java.util.HashSet<FlameOrb.Tier>();
        for (int y = 61; y <= 67; y++) seen.add(tierAt(flame, hotColumn, y));
        assertTrue(seen.size() >= 3, "stroke must show a tier ramp, saw " + seen);
    }

    @Test void flameStrokeShadesFromTheOrbsOwnRamp() {
        var draft = flameBar();
        double phase = 0.35;
        var flame = GlyphRasterizer.renderEmissiveRgb(draft, phase);
        var base = GlyphRasterizer.renderFullRgb(draft);
        int frame = FlameOrb.frameAt(phase);
        // Every fully covered pixel is the orb's own colour at that spot: the distance
        // down the spine wrapped into the bead it falls in, and the distance across
        // measured in half-widths.
        double along = alongAt(65, 8);
        for (int y = 62; y <= 66; y++) {
            int o = (y * 128 + 65) * 3;
            assertTrue(base[o] >= 0.999f, "row " + y + " must be fully covered");
            assertArrayEquals(
                    FlameOrb.color(FlameOrb.tier(FlameOrb.strokeRadialAt(frame, along, (y - 64) / 4.0))),
                    pixel(flame, 65, y),
                    1e-6f,
                    "row " + y + " must be the orb's own colour at that offset");
        }
    }

    /** Where {@code x} falls inside its bead on a bar of {@code width}, in half-lengths. */
    private static double alongAt(int x, double width) {
        double beadHalfLength = width / 2 * FlameOrb.LENGTH_RATIO;
        double beads = (x - 40) / beadHalfLength;
        return 2 / Math.PI * Math.asin(Math.sin(Math.PI * beads / 2));
    }

    @Test void flameStrokeBeadsIntoARunOfOrbsWithSpreadCores() {
        var flame = GlyphRasterizer.renderEmissiveRgb(flameBar(), 0.0);
        // A long stroke is a rope of orbs, not one smear: the core repeats, and each
        // run is spread along the spine rather than being a single hot pixel.
        var runs = coreRuns(flame);
        assertTrue(runs.size() >= 2, "a long stroke must bead into repeated cores, saw " + runs.size());
        for (int run : runs) assertTrue(run >= 2, "each core must spread along the spine, saw " + run);
        // Between beads the ink cools off, which is what makes the beads legible.
        var seen = new java.util.HashSet<FlameOrb.Tier>();
        for (int x = 40; x <= 90; x++) seen.add(tierAt(flame, x, 64));
        assertTrue(seen.size() >= 3, "the spine must cool between beads, saw " + seen);
    }

    @Test void flameStrokeBeadsInProportionToItsWidth() {
        // Bead length is measured in the stroke's own half-widths, so the same 50px bar
        // drawn thin packs in more beads than one drawn fat. That is what keeps every
        // tier visible on a hair-thin stroke instead of washing it out to one colour.
        int thin = coreRuns(GlyphRasterizer.renderEmissiveRgb(flameBar(3), 0.0)).size();
        int fat = coreRuns(GlyphRasterizer.renderEmissiveRgb(flameBar(12), 0.0)).size();
        assertTrue(thin > fat, "a thin stroke must bead more often than a fat one, "
                + thin + " vs " + fat);
        assertTrue(fat >= 1, "even a fat stroke must show a core");
    }

    @Test void flameStrokeJoinsItsBeadsWithoutASeam() {
        var flame = GlyphRasterizer.renderEmissiveRgb(flameBar(), 0.0);
        // Beads reach past their own cell and the coordinate down the spine folds rather
        // than wrapping, so a run of beads is continuous: away from the caps the spine
        // dips between cores without ever going cold, and never skips a tier doing it.
        for (int x = 46; x <= 84; x++) {
            var tier = tierAt(flame, x, 64);
            assertTrue(tier.ordinal() <= FlameOrb.Tier.ORANGE.ordinal(),
                    "the spine must stay warm between beads, saw " + tier + " at x=" + x);
            var next = tierAt(flame, x + 1, 64);
            assertTrue(Math.abs(tier.ordinal() - next.ordinal()) <= 1,
                    "the spine must not jump tiers at x=" + x + ", " + tier + " to " + next);
        }
    }

    /** Lengths of each separate run of core pixels along the bar's spine. */
    private static java.util.List<Integer> coreRuns(float[] flame) {
        var runs = new java.util.ArrayList<Integer>();
        int run = 0;
        for (int x = 40; x <= 90; x++) {
            if (tierAt(flame, x, 64) == FlameOrb.Tier.CORE) {
                run++;
            } else if (run > 0) {
                runs.add(run);
                run = 0;
            }
        }
        if (run > 0) runs.add(run);
        return runs;
    }



    @Test void flameStrokeRunsTheOrbAnimationClock() {
        var draft = flameBar();
        var frame0 = GlyphRasterizer.renderEmissiveRgb(draft, 0.0);
        assertArrayEquals(frame0, GlyphRasterizer.renderEmissiveRgb(draft, 0.01));
        assertArrayEquals(frame0, GlyphRasterizer.renderEmissiveRgb(draft, 0.049));
        assertFalse(java.util.Arrays.equals(frame0, GlyphRasterizer.renderEmissiveRgb(draft, 0.05)),
                "crossing a frame boundary must change the ink");
    }

    @Test void flameStrokeBreathesWithTheOrb() {
        var draft = flameBar();
        var peak = GlyphRasterizer.renderEmissiveRgb(draft, 5 / (double) FlameOrb.FRAME_COUNT);
        var trough = GlyphRasterizer.renderEmissiveRgb(draft, 15 / (double) FlameOrb.FRAME_COUNT);
        int breathed = 0;
        for (int x = 42; x <= 88; x++) {
            for (int y = 61; y <= 67; y++) if (tierAt(peak, x, y) != tierAt(trough, x, y)) breathed++;
        }
        assertTrue(breathed > 0, "the stroke must breathe with the orb");
    }

    @Test void flameStrokeCapsRoundOffLikeTheOrbsEdge() {
        var draft = flameBar();
        var flame = GlyphRasterizer.renderEmissiveRgb(draft, 0.0);
        // Past the bar's end the field measures from the endpoint, so the cap curves
        // instead of being cut square: the ink narrows row by row as it runs out.
        int spineReach = capReach(flame, 64);
        assertTrue(spineReach > 0, "the cap must carry ink past the spine's end");
        assertTrue(capReach(flame, 61) < spineReach, "the cap must narrow above the spine");
        assertTrue(capReach(flame, 67) < spineReach, "the cap must narrow below the spine");
    }

    /** How far left of the bar's start pixel {@code (40, y)} ink still reaches. */
    private static int capReach(float[] flame, int y) {
        int reach = 0;
        while (reach < 8) {
            float[] px = pixel(flame, 39 - reach, y);
            if (px[0] == 0f && px[1] == 0f && px[2] == 0f) break;
            reach++;
        }
        return reach;
    }

    /** The orb tier a rendered pixel corresponds to. */
    private static FlameOrb.Tier tierAt(float[] rgb, int x, int y) {
        float[] px = pixel(rgb, x, y);
        for (var tier : FlameOrb.Tier.values()) {
            float[] c = FlameOrb.color(tier);
            if (Math.abs(c[0] - px[0]) < 1e-6f
                    && Math.abs(c[1] - px[1]) < 1e-6f
                    && Math.abs(c[2] - px[2]) < 1e-6f) return tier;
        }
        throw new AssertionError("not an orb tier at " + x + "," + y
                + ": " + px[0] + "," + px[1] + "," + px[2]);
    }

    private static boolean isOrbTier(float[] px) {
        for (var tier : FlameOrb.Tier.values()) {
            float[] c = FlameOrb.color(tier);
            if (Math.abs(c[0] - px[0]) < 1e-6f
                    && Math.abs(c[1] - px[1]) < 1e-6f
                    && Math.abs(c[2] - px[2]) < 1e-6f) return true;
        }
        return false;
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
