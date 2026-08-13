package dev.jlo.wizardry.mapgui;

import dev.jlo.wizardry.glyph.GlyphCaptureSession;
import dev.jlo.wizardry.glyph.GlyphDraft;
import dev.jlo.wizardry.glyph.GlyphPoint;

public final class GlyphStrokeTracker {
    public static final long SAME_STROKE_MILLIS = 300;
    private final GlyphCaptureSession capture = new GlyphCaptureSession();
    private boolean active;
    private int lastX;
    private int lastY;
    private long lastAt;

    public void acceptClick(int x, int y, long nowMillis) {
        if (x < 0 || x >= 128 || y < 0 || y >= 128) throw new IllegalArgumentException("cursor outside glyph canvas");
        if (!active || (nowMillis >= lastAt && nowMillis - lastAt >= SAME_STROKE_MILLIS)) {
            finishActive();
            capture.beginStroke(nowMillis);
            capture.appendPoint(new GlyphPoint(x, y));
            active = true;
        } else {
            int steps = Math.max(Math.abs(x - lastX), Math.abs(y - lastY));
            for (int i = 1; i <= steps; i++) capture.appendPoint(new GlyphPoint(lastX + (x - lastX) * (double) i / steps, lastY + (y - lastY) * (double) i / steps));
        }
        lastX = x; lastY = y; lastAt = nowMillis;
    }
    public void pause(long nowMillis) { if (active && nowMillis >= lastAt && nowMillis - lastAt >= SAME_STROKE_MILLIS) finishActive(); }
    public void undo() { finishActive(); capture.undo(); }
    public void clear() { active = false; capture.clear(); }
    public GlyphDraft snapshot() { return capture.snapshot(); }
    public GlyphDraft close() { finishActive(); return capture.close(); }
    private void finishActive() { if (active) { capture.endStroke(); active = false; } }
}
