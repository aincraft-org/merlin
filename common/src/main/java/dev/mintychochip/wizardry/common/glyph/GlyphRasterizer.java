package dev.mintychochip.wizardry.common.glyph;
import dev.mintychochip.wizardry.api.glyph.GlyphBitmap;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphLimits;
import dev.mintychochip.wizardry.api.glyph.GlyphPoint;


public final class GlyphRasterizer {
    private GlyphRasterizer() {}

    public static GlyphBitmap renderFull(GlyphDraft draft) {
        byte[] pixels = new byte[GlyphLimits.CANVAS_WIDTH * GlyphLimits.CANVAS_HEIGHT];
        for (var stroke : draft.strokes()) {
            for (int i = 1; i < stroke.points().size(); i++) {
                double startWidth = i == 1 ? stroke.brushWidth() : stroke.widthAtSegment(i - 2);
                double endWidth = stroke.widthAtSegment(i - 1);
                drawLine(pixels, stroke.points().get(i - 1), stroke.points().get(i), startWidth, endWidth);
            }
            if (stroke.points().size() == 1) stamp(pixels, stroke.points().getFirst(), stroke.brushWidth());
        }
        return new GlyphBitmap(GlyphLimits.CANVAS_WIDTH, GlyphLimits.CANVAS_HEIGHT, pixels);
    }

    public static int[] paddedInkBounds(byte[] pixels, int padding) {
        int minX = GlyphLimits.CANVAS_WIDTH, minY = GlyphLimits.CANVAS_HEIGHT, maxX = -1, maxY = -1;
        for (int y = 0; y < GlyphLimits.CANVAS_HEIGHT; y++) for (int x = 0; x < GlyphLimits.CANVAS_WIDTH; x++) if ((pixels[y * GlyphLimits.CANVAS_WIDTH + x] & 0xff) != 0) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        if (maxX < 0) return new int[] {0, 0, -1, -1};
        return new int[] {
                Math.max(0, minX - padding),
                Math.max(0, minY - padding),
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

    public static GlyphBitmap renderNormalized(GlyphDraft draft, int size, int padding) {
        if (size <= 0 || padding < 0) throw new IllegalArgumentException("invalid normalized bitmap dimensions");
        var full = renderFull(draft);
        return resample(full, paddedInkBounds(full.pixels(), padding), size);
    }

    private static void drawLine(byte[] pixels, GlyphPoint start, GlyphPoint end, double startWidth, double endWidth) {
        int steps = interpolationSteps(start, end, startWidth, endWidth);
        for (int index = 0; index <= steps; index++) {
            double t = (double) index / steps;
            double width = startWidth + (endWidth - startWidth) * t;
            stamp(pixels, new GlyphPoint(
                    start.x() + (end.x() - start.x()) * t,
                    start.y() + (end.y() - start.y()) * t), width);
        }
    }

    private static int interpolationSteps(
            GlyphPoint start, GlyphPoint end, double startWidth, double endWidth) {
        double distance = Math.hypot(end.x() - start.x(), end.y() - start.y());
        double radiusDelta = Math.abs(endWidth - startWidth) / 2.0;
        int movementSteps = (int) Math.ceil(distance / 0.5);
        int radiusSteps = (int) Math.ceil(radiusDelta / 0.25);
        return Math.max(1, Math.max(movementSteps, radiusSteps));
    }

    private static void stamp(byte[] pixels, GlyphPoint point, double width) {
        double radius = width / 2.0;
        int bounds = Math.max(0, (int) Math.ceil(radius + 0.5));
        int cx = (int) Math.round(point.x());
        int cy = (int) Math.round(point.y());
        int maxX = GlyphLimits.CANVAS_WIDTH - 1;
        int maxY = GlyphLimits.CANVAS_HEIGHT - 1;
        for (int y = Math.max(0, cy - bounds); y <= Math.min(maxY, cy + bounds); y++)
            for (int x = Math.max(0, cx - bounds); x <= Math.min(maxX, cx + bounds); x++)
                if (Math.hypot(x - point.x(), y - point.y()) <= radius + 0.5)
                    pixels[y * GlyphLimits.CANVAS_WIDTH + x] = (byte) 255;
    }
}
