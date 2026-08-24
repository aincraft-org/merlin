package dev.mintychochip.merlin.paper.mapgui;

import dev.mintychochip.merlin.api.glyph.GlyphCaptureSession;
import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphLimits;
import dev.mintychochip.merlin.api.glyph.GlyphPoint;

public final class GlyphStrokeTracker {
    public static final double MIN_BRUSH_WIDTH = 1;
    public static final double MAX_BRUSH_WIDTH = 6;
    public static final long STROKE_PAUSE_MILLIS = 600;
    private static final double SMOOTHING_TIME_MILLIS = 12.0;
    private final GlyphCaptureSession capture = new GlyphCaptureSession();
    private boolean active;
    private int lastX;
    private int lastY;
    private long lastAt;
    private boolean hasSmoothedWidth;
    private double smoothedWidth;
    public GlyphStrokeTracker() {}

    public GlyphStrokeTracker(GlyphDraft draft) {
        for (var stroke : draft.strokes()) {
            capture.brushWidth(stroke.brushWidth());
            capture.beginStroke(stroke.startedAtMillis());
            capture.appendPoint(stroke.points().getFirst());
            for (int i = 1; i < stroke.points().size(); i++) {
                capture.appendPoint(stroke.points().get(i));
                capture.segmentWidth(stroke.segmentWidths().get(i - 1));
            }
            capture.endStroke();
        }
    }


    public synchronized void beginStroke(int x, int y, long nowMillis) {
        checkPoint(x, y);
        finishActive();
        resetSmoothing();
        capture.brushWidth(MAX_BRUSH_WIDTH);
        capture.beginStroke(nowMillis);
        capture.appendPoint(new GlyphPoint(x, y));
        active = true;
        lastX = x;
        lastY = y;
        lastAt = nowMillis;
    }

    public synchronized void appendPoint(int x, int y, long nowMillis) {
        checkPoint(x, y);
        if (!active) {
            beginStroke(x, y, nowMillis);
            return;
        }
        int steps = Math.max(Math.abs(x - lastX), Math.abs(y - lastY));
        if (steps == 0) {
            lastAt = nowMillis;
            return;
        }
        long elapsedMillis = elapsedMillis(lastAt, nowMillis);
        double velocity = steps / Math.max(1.0, (double) elapsedMillis);
        double targetWidth = widthForVelocity(velocity);
        double width = smoothWidth(targetWidth, elapsedMillis);
        for (int i = 1; i <= steps; i++) {
            if (capture.snapshot().strokes().getLast().points().size() >= GlyphLimits.MAX_POINTS_PER_STROKE) {
                capture.endStroke();
                capture.beginStroke(nowMillis);
                capture.appendPoint(new GlyphPoint(lastX, lastY));
                resetSmoothing();
                width = smoothWidth(targetWidth, elapsedMillis);
            }
            capture.appendPoint(new GlyphPoint(
                    lastX + (x - lastX) * (double) i / steps,
                    lastY + (y - lastY) * (double) i / steps));
            capture.segmentWidth(width);
        }
        lastX = x;
        lastY = y;
        lastAt = nowMillis;
    }

    public synchronized void acceptClick(int x, int y, long nowMillis) {
        if (active && elapsedMillis(lastAt, nowMillis) >= STROKE_PAUSE_MILLIS) endStroke(nowMillis);
        appendPoint(x, y, nowMillis);
    }

    public synchronized void endStroke(long nowMillis) {
        finishActive();
    }

    public synchronized void pause(long nowMillis) { endStroke(nowMillis); }
    public synchronized void undo() { finishActive(); capture.undo(); }
    public synchronized void clear() { active = false; resetSmoothing(); capture.clear(); }
    public synchronized GlyphDraft snapshot() { return capture.snapshot(); }
    static double widthForVelocity(double velocity) {
        double normalized = Math.min(1.0, Math.max(0.0, velocity / 0.5));
        return MAX_BRUSH_WIDTH - normalized * (MAX_BRUSH_WIDTH - MIN_BRUSH_WIDTH);
    }
    public synchronized GlyphDraft close() { finishActive(); return capture.close(); }
    private void finishActive() {
        if (active) {
            capture.endStroke();
            active = false;
        }
        resetSmoothing();
    }

    private double smoothWidth(double targetWidth, long elapsedMillis) {
        if (!hasSmoothedWidth) {
            smoothedWidth = targetWidth;
            hasSmoothedWidth = true;
            return smoothedWidth;
        }
        double elapsed = Math.min(1000.0, (double) elapsedMillis);
        double factor = 1.0 - Math.exp(-elapsed / SMOOTHING_TIME_MILLIS);
        factor = Math.max(0.0, Math.min(1.0, factor));
        smoothedWidth += (targetWidth - smoothedWidth) * factor;
        smoothedWidth = Math.max(MIN_BRUSH_WIDTH, Math.min(MAX_BRUSH_WIDTH, smoothedWidth));
        return smoothedWidth;
    }

    private void resetSmoothing() {
        hasSmoothedWidth = false;
        smoothedWidth = 0.0;
    }

    private static long elapsedMillis(long earlier, long later) {
        if (later <= earlier) return 0;
        long elapsed = later - earlier;
        return elapsed < 0 ? Long.MAX_VALUE : elapsed;
    }
    private static void checkPoint(int x, int y) {
        if (x < 0 || x >= 128 || y < 0 || y >= 128) throw new IllegalArgumentException("cursor outside glyph canvas");
    }
}