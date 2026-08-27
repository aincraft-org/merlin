package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;
import dev.mintychochip.merlin.api.glyph.GlyphStroke;
import dev.mintychochip.merlin.common.glyph.FlameOrb;
import dev.mintychochip.merlin.common.glyph.GlyphRasterizer;
import java.util.List;
import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

final class GlyphMapRendererTest {
    @Test void mapsEmptyAndFullIntensityToBlackAndWhite() {
        assertEquals(MapPalette.matchColor(new java.awt.Color(0, 0, 0)), GlyphMapRenderer.mapColor((byte) 0));
        assertEquals(MapPalette.matchColor(new java.awt.Color(255, 255, 255)), GlyphMapRenderer.mapColor((byte) 255));
    }

    @Test void preservesPartialIntensityOrdering() {
        assertNotEquals(GlyphMapRenderer.mapColor((byte) 32), GlyphMapRenderer.mapColor((byte) 192));
    }

    @Test void mapColorKeepsInkHues() {
        byte flame = GlyphMapRenderer.mapColor(255, 77, 0);
        byte frost = GlyphMapRenderer.mapColor(61, 220, 255);
        byte physical = GlyphMapRenderer.mapColor(232, 228, 217);
        assertNotEquals(flame, frost);
        assertNotEquals(flame, physical);
        assertNotEquals(frost, physical);
    }

    @Test void rgbRendererPaintsInkInsteadOfWhiteInk() {
        float[] rgb = new float[128 * 128 * 3];
        int o = (12 * 128 + 12) * 3;
        rgb[o] = 255 / 255f;
        rgb[o + 1] = 77 / 255f;
        rgb[o + 2] = 0;
        var renderer = new GlyphMapRenderer(rgb);
        assertEquals(GlyphMapRenderer.mapColor(255, 77, 0), renderer.colorAt(12, 12));
        assertNotEquals(GlyphMapRenderer.mapColor((byte) 255), renderer.colorAt(12, 12));
    }

    @Test void animatedRendererCyclesPrecomputedFrames() {
        // The cycling renderer must expose the same number of frames it was built with,
        // and every drawn frame must match one of the supplied phase frames.
        var fire = new GlyphStroke(List.of(new GlyphPoint(64, 64)), 8, 0, List.of(), GlyphElement.FLAME);
        var frost = new GlyphStroke(List.of(new GlyphPoint(64, 64)), 8, 0, List.of(), GlyphElement.FROST);
        var draft = new GlyphDraft(List.of(fire, frost));
        var frames = List.of(
                GlyphRasterizer.renderEmissiveRgb(draft, 0.0),
                GlyphRasterizer.renderEmissiveRgb(draft, 0.25),
                GlyphRasterizer.renderEmissiveRgb(draft, 0.5),
                GlyphRasterizer.renderEmissiveRgb(draft, 0.75));
        var renderer = new GlyphMapRenderer(frames);
        assertEquals(4, renderer.animatedFrameCount());
        // Every precomputed frame is byte-reachable via animatedFrameAt.
        for (int f = 0; f < 4; f++) {
            for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
                int o = (y * 128 + x) * 3;
                byte expected = GlyphMapRenderer.mapColor(
                        Math.round(Math.max(0f, Math.min(1f, frames.get(f)[o])) * 255f),
                        Math.round(Math.max(0f, Math.min(1f, frames.get(f)[o + 1])) * 255f),
                        Math.round(Math.max(0f, Math.min(1f, frames.get(f)[o + 2])) * 255f));
                assertEquals(expected, renderer.animatedFrameAt(f, x, y));
            }
        }
    }

    @Test void animatedRendererFlamePixelsDifferAcrossFrames() {
        // Walk the orb's real clock, the same way the saved map does. Bukkit's palette
        // can collapse two tier colors onto one byte at any single probe, so count the
        // bytes that differ across the whole canvas instead of spot-checking.
        var bar = new GlyphStroke(
                List.of(new GlyphPoint(40, 64), new GlyphPoint(90, 64)),
                8, 0, List.of(8.0), GlyphElement.FLAME);
        var draft = new GlyphDraft(List.of(bar));
        int count = FlameOrb.FRAME_COUNT;
        var renderer = new GlyphMapRenderer(
                f -> GlyphRasterizer.renderEmissiveRgb(draft, f / (double) count), count);
        assertEquals(count, renderer.animatedFrameCount());
        // Every frame past the first differs from it somewhere. Frames half a cycle
        // apart are the one exception: the orb's pulse and jitter both sit at the same
        // point of their sine there, so the pip itself repeats on that pair.
        for (int f = 1; f < count; f++) {
            if (f == count / 2) continue;
            int diffs = 0;
            for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
                if (renderer.animatedFrameAt(0, x, y) != renderer.animatedFrameAt(f, x, y)) diffs++;
            }
            assertTrue(diffs > 0, "frame " + f + " must differ from frame 0");
        }
        // Ink never spreads: pixels off the stroke are identical on every frame.
        for (int f = 1; f < count; f++) {
            for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
                if (Math.abs(y - 64) <= 5 && x >= 34 && x <= 96) continue;
                assertEquals(renderer.animatedFrameAt(0, x, y), renderer.animatedFrameAt(f, x, y),
                        "off-stroke pixel changed on frame " + f + " at " + x + "," + y);
            }
        }
    }

    @Test void animatedRendererRejectsEmptyFrames() {
        assertThrows(IllegalArgumentException.class, () -> new GlyphMapRenderer(List.of()));
    }

    @Test void animatedRendererColorAtReturnsFirstFrame() {
        // colorAt on an animated renderer must return frame 0, not NPE.
        var rgb = new float[128 * 128 * 3];
        int o = (12 * 128 + 12) * 3;
        rgb[o] = 1f; rgb[o + 1] = 0.3f; rgb[o + 2] = 0f;
        var frames = List.of(rgb.clone(), rgb.clone(), rgb.clone(), rgb.clone());
        var renderer = new GlyphMapRenderer(frames);
        assertEquals(GlyphMapRenderer.mapColor(255, 77, 0), renderer.colorAt(12, 12));
        // animatedFrameAt must agree with colorAt for frame 0.
        assertEquals(renderer.colorAt(12, 12), renderer.animatedFrameAt(0, 12, 12));
    }
    @Test void rejectsMismatchedGlyphBitmapDimensions() {
        var bitmap = new dev.mintychochip.merlin.api.glyph.GlyphBitmap(64, 64, new byte[64 * 64]);
        assertThrows(IllegalArgumentException.class, () -> new GlyphMapRenderer(bitmap));
    }
}
