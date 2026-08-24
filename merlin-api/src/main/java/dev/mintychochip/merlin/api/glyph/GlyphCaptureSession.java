package dev.mintychochip.merlin.api.glyph;

import java.util.ArrayList;
import java.util.List;

public class GlyphCaptureSession {
    private final List<GlyphStroke> completed = new ArrayList<>();
    private List<GlyphPoint> current;
    private List<Double> currentWidths;
    private long startedAt;
    private double brushWidth = 1;
    private GlyphDraft lastSnapshot = GlyphDraft.empty();

    public void beginStroke(long nowMillis) {
        if (current != null) throw new IllegalStateException("stroke already active");
        current = new ArrayList<>();
        currentWidths = new ArrayList<>();
        startedAt = nowMillis;
    }

    public void appendPoint(GlyphPoint point) {
        if (current == null) throw new IllegalStateException("no active stroke");
        current.add(point);
    }

    public void segmentWidth(double width) {
        if (current == null || current.size() < 2) throw new IllegalStateException("no segment available");
        if (!Double.isFinite(width) || width <= 0 || width > 32) throw new IllegalArgumentException("invalid segment brush width");
        currentWidths.add(width);
    }

    public void endStroke() {
        if (current == null) return;
        if (!current.isEmpty()) {
            var widths = new ArrayList<>(currentWidths);
            while (widths.size() < current.size() - 1) widths.add(brushWidth);
            completed.add(new GlyphStroke(current, brushWidth, startedAt, widths));
        }
        current = null;
        currentWidths = null;
        lastSnapshot = snapshot();
    }

    public void undo() {
        if (current != null) {
            current = null;
            currentWidths = null;
        } else if (!completed.isEmpty()) {
            completed.removeLast();
            lastSnapshot = snapshot();
        }
    }

    public void clear() {
        current = null;
        currentWidths = null;
        completed.clear();
        lastSnapshot = GlyphDraft.empty();
    }

    public GlyphDraft snapshot() {
        if (current == null || current.isEmpty()) return new GlyphDraft(completed);
        var strokes = new ArrayList<>(completed);
        var widths = new ArrayList<>(currentWidths);
        while (widths.size() < current.size() - 1) widths.add(brushWidth);
        strokes.add(new GlyphStroke(current, brushWidth, startedAt, widths));
        return new GlyphDraft(strokes);
    }

    public GlyphDraft close() {
        lastSnapshot = snapshot();
        return lastSnapshot;
    }

    public GlyphDraft lastSnapshot() { return lastSnapshot; }

    public void brushWidth(double width) {
        if (!Double.isFinite(width) || width <= 0 || width > 32) throw new IllegalArgumentException("invalid glyph brush width");
        brushWidth = width;
    }
}
