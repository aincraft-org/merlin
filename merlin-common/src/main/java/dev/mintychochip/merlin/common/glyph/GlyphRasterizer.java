package dev.mintychochip.merlin.common.glyph;

import dev.mintychochip.merlin.api.glyph.GlyphBitmap;
import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.GlyphLimits;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;

public final class GlyphRasterizer {
    private static final float FLAME_MIN_COVERAGE = 0.02f;
    private static final float FLAME_G_RATIO = 0.6f;
    private static final float FLAME_B_RATIO = 0.25f;

    private GlyphRasterizer() {}

    public static float[] renderFullRgb(GlyphDraft draft) {
        float[] rgb = new float[GlyphLimits.CANVAS_WIDTH * GlyphLimits.CANVAS_HEIGHT * 3];
        for (var stroke : draft.strokes()) {
            for (int i = 1; i < stroke.points().size(); i++) {
                double startWidth = i == 1 ? stroke.brushWidth() : stroke.widthAtSegment(i - 2);
                double endWidth = stroke.widthAtSegment(i - 1);
                drawLine(rgb, stroke.points().get(i - 1), stroke.points().get(i), startWidth, endWidth, stroke.element());
            }
            if (stroke.points().size() == 1) stamp(rgb, stroke.points().getFirst(), stroke.brushWidth(), stroke.element());
        }
        return rgb;
    }

    public static GlyphBitmap renderFull(GlyphDraft draft) {
        float[] rgb = renderFullRgb(draft);
        byte[] pixels = new byte[GlyphLimits.CANVAS_WIDTH * GlyphLimits.CANVAS_HEIGHT];
        for (int i = 0; i < pixels.length; i++) {
            int o = i * 3;
            if (rgb[o] > 0 || rgb[o + 1] > 0 || rgb[o + 2] > 0) pixels[i] = (byte) 255;
        }
        return new GlyphBitmap(GlyphLimits.CANVAS_WIDTH, GlyphLimits.CANVAS_HEIGHT, pixels);
    }

    public static float[] renderEmissiveRgb(GlyphDraft draft) {
        return renderEmissiveRgb(draft, 0.0);
    }

    /**
     * Shades flame strokes as a run of orbs beaded along the spine. The stroke keeps
     * its geometry; only its colour changes. Both bead length and cross-section are
     * measured in the stroke's own half-width, so a hair-thin stroke gets short beads
     * and still shows every tier, and a fat one gets long beads at the same
     * proportions. No halo or emissive brighten is applied.
     */
    public static float[] renderEmissiveRgb(GlyphDraft draft, double phase) {
        float[] base = renderFullRgb(draft);
        float[] out = base.clone();
        float[] radial = strokeField(draft, FlameOrb.frameAt(phase));
        for (int i = 0; i < radial.length; i++) {
            int o = i * 3;
            if (!isFlame(base, o)) continue;
            float coverage = Math.min(1f, base[o]);
            float[] tier = FlameOrb.color(FlameOrb.tier(radial[i]));
            out[o] = tier[0] * coverage;
            out[o + 1] = tier[1] * coverage;
            out[o + 2] = tier[2] * coverage;
        }
        return out;
    }

    private static float[] strokeField(GlyphDraft draft, int frame) {
        float[] radial = new float[GlyphLimits.CANVAS_WIDTH * GlyphLimits.CANVAS_HEIGHT];
        java.util.Arrays.fill(radial, 1f);
        for (var stroke : draft.strokes()) {
            if (stroke.element() != GlyphElement.FLAME || stroke.points().isEmpty()) continue;
            var points = stroke.points();
            if (points.size() == 1) {
                var only = points.getFirst();
                segmentField(radial, frame, only, only, stroke.brushWidth(), stroke.brushWidth(), 0);
                continue;
            }
            double arc = 0;
            for (int i = 1; i < points.size(); i++) {
                var from = points.get(i - 1);
                var to = points.get(i);
                double length = Math.hypot(to.x() - from.x(), to.y() - from.y());
                double startWidth = i == 1 ? stroke.brushWidth() : stroke.widthAtSegment(i - 2);
                segmentField(radial, frame, from, to, startWidth,
                        stroke.widthAtSegment(i - 1), arc);
                arc += length;
            }
        }
        return radial;
    }

    private static void segmentField(float[] radial, int frame, GlyphPoint from, GlyphPoint to,
            double startWidth, double endWidth, double arcAtStart) {
        double alongX = to.x() - from.x();
        double alongY = to.y() - from.y();
        double lengthSq = alongX * alongX + alongY * alongY;
        double length = Math.sqrt(lengthSq);
        double pad = Math.max(startWidth, endWidth) / 2 + 1;
        int minX = Math.max(0, (int) Math.floor(Math.min(from.x(), to.x()) - pad));
        int maxX = Math.min(GlyphLimits.CANVAS_WIDTH - 1, (int) Math.ceil(Math.max(from.x(), to.x()) + pad));
        int minY = Math.max(0, (int) Math.floor(Math.min(from.y(), to.y()) - pad));
        int maxY = Math.min(GlyphLimits.CANVAS_HEIGHT - 1, (int) Math.ceil(Math.max(from.y(), to.y()) + pad));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x - from.x();
                double py = y - from.y();
                double t = lengthSq <= 0 ? 0 : Math.clamp((px * alongX + py * alongY) / lengthSq, 0, 1);
                double halfWidth = (startWidth + (endWidth - startWidth) * t) / 2;
                if (halfWidth <= 0) continue;
                double offX = px - alongX * t;
                double offY = py - alongY * t;
                double across = Math.hypot(offX, offY) / halfWidth;
                if (alongX * offY - alongY * offX < 0) across = -across;
                double beadHalfLength = halfWidth * FlameOrb.LENGTH_RATIO;
                double beads = (arcAtStart + length * t) / beadHalfLength;
                double along = triangle(beads);
                float n = (float) FlameOrb.strokeRadialAt(frame, along, across);
                int i = y * GlyphLimits.CANVAS_WIDTH + x;
                if (n < radial[i]) radial[i] = n;
            }
        }
    }

    /**
     * Folds a distance measured in bead half-lengths onto {@code [-1, 1]}, where 0 is a
     * bead's centre and ±1 is a join with its neighbour. Folding rather than wrapping
     * means the coordinate never jumps, so consecutive beads meet without a seam.
     */
    private static double triangle(double beads) {
        return 2 / Math.PI * Math.asin(Math.sin(Math.PI * beads / 2));
    }

    private static boolean isFlame(float[] base, int o) {
        float r = base[o];
        return r > FLAME_MIN_COVERAGE
                && base[o + 1] < r * FLAME_G_RATIO
                && base[o + 2] < r * FLAME_B_RATIO;
    }

    public static int[] paddedInkBounds(byte[] pixels, int padding) {
        int minX = GlyphLimits.CANVAS_WIDTH, minY = GlyphLimits.CANVAS_HEIGHT, maxX = -1, maxY = -1;
        for (int y = 0; y < GlyphLimits.CANVAS_HEIGHT; y++) for (int x = 0; x < GlyphLimits.CANVAS_WIDTH; x++) if ((pixels[y * GlyphLimits.CANVAS_WIDTH + x] & 0xff) != 0) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        if (maxX < 0) return new int[] {0, 0, -1, -1};
        return new int[] {
                Math.max(0, minX - padding), Math.max(0, minY - padding),
                Math.min(GlyphLimits.CANVAS_WIDTH - 1, maxX + padding),
                Math.min(GlyphLimits.CANVAS_HEIGHT - 1, maxY + padding)
        };
    }

    public static int[] paddedInkBounds(float[] rgb, int padding) {
        int minX = GlyphLimits.CANVAS_WIDTH, minY = GlyphLimits.CANVAS_HEIGHT, maxX = -1, maxY = -1;
        for (int y = 0; y < GlyphLimits.CANVAS_HEIGHT; y++) for (int x = 0; x < GlyphLimits.CANVAS_WIDTH; x++) {
            int o = (y * GlyphLimits.CANVAS_WIDTH + x) * 3;
            if (rgb[o] > 0 || rgb[o + 1] > 0 || rgb[o + 2] > 0) {
                minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) return new int[] {0, 0, -1, -1};
        return new int[] {
                Math.max(0, minX - padding), Math.max(0, minY - padding),
                Math.min(GlyphLimits.CANVAS_WIDTH - 1, maxX + padding),
                Math.min(GlyphLimits.CANVAS_HEIGHT - 1, maxY + padding)
        };
    }

    public static GlyphBitmap resample(GlyphBitmap full, int[] bounds, int size) {
        if (size <= 0) throw new IllegalArgumentException("invalid normalized bitmap dimensions");
        if (bounds[2] < 0) return new GlyphBitmap(size, size, new byte[size * size]);
        int minX = bounds[0], minY = bounds[1], maxX = bounds[2], maxY = bounds[3];
        byte[] p = full.pixels();
        byte[] out = new byte[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int sx = minX + (int) ((long) x * (maxX - minX + 1) / size);
            int sy = minY + (int) ((long) y * (maxY - minY + 1) / size);
            out[y * size + x] = p[sy * GlyphLimits.CANVAS_WIDTH + sx];
        }
        return new GlyphBitmap(size, size, out);
    }

    public static float[] resampleRgb(float[] rgb, int[] bounds, int size) {
        if (size <= 0) throw new IllegalArgumentException("invalid normalized bitmap dimensions");
        float[] out = new float[size * size * 3];
        if (bounds[2] < 0) return out;
        int minX = bounds[0], minY = bounds[1], maxX = bounds[2], maxY = bounds[3];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int sx = minX + (int) ((long) x * (maxX - minX + 1) / size);
            int sy = minY + (int) ((long) y * (maxY - minY + 1) / size);
            int src = (sy * GlyphLimits.CANVAS_WIDTH + sx) * 3;
            int dst = (y * size + x) * 3;
            out[dst] = rgb[src]; out[dst + 1] = rgb[src + 1]; out[dst + 2] = rgb[src + 2];
        }
        return out;
    }

    public static GlyphBitmap renderNormalized(GlyphDraft draft, int size, int padding) {
        if (size <= 0 || padding < 0) throw new IllegalArgumentException("invalid normalized bitmap dimensions");
        var full = renderFull(draft);
        return resample(full, paddedInkBounds(full.pixels(), padding), size);
    }

    private static void drawLine(float[] rgb, GlyphPoint start, GlyphPoint end,
            double startWidth, double endWidth, GlyphElement element) {
        int steps = interpolationSteps(start, end, startWidth, endWidth);
        for (int index = 0; index <= steps; index++) {
            double t = (double) index / steps;
            double width = startWidth + (endWidth - startWidth) * t;
            stamp(rgb, new GlyphPoint(
                    start.x() + (end.x() - start.x()) * t,
                    start.y() + (end.y() - start.y()) * t), width, element);
        }
    }

    private static int interpolationSteps(GlyphPoint start, GlyphPoint end,
            double startWidth, double endWidth) {
        double distance = Math.hypot(end.x() - start.x(), end.y() - start.y());
        double radiusDelta = Math.abs(endWidth - startWidth) / 2.0;
        int movementSteps = (int) Math.ceil(distance / 0.5);
        int radiusSteps = (int) Math.ceil(radiusDelta / 0.25);
        return Math.max(1, Math.max(movementSteps, radiusSteps));
    }

    private static void stamp(float[] rgb, GlyphPoint point, double width, GlyphElement element) {
        double radius = width / 2.0;
        int bounds = Math.max(0, (int) Math.ceil(radius + 0.5));
        int cx = (int) Math.round(point.x());
        int cy = (int) Math.round(point.y());
        int maxX = GlyphLimits.CANVAS_WIDTH - 1;
        int maxY = GlyphLimits.CANVAS_HEIGHT - 1;
        for (int y = Math.max(0, cy - bounds); y <= Math.min(maxY, cy + bounds); y++)
            for (int x = Math.max(0, cx - bounds); x <= Math.min(maxX, cx + bounds); x++) {
                float a = GlyphElement.coverage(Math.hypot(x - point.x(), y - point.y()), radius);
                if (a > 0) GlyphElement.blend(rgb, (y * GlyphLimits.CANVAS_WIDTH + x) * 3, element, a);
            }
    }
}
