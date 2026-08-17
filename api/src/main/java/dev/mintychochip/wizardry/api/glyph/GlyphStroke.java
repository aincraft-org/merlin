package dev.mintychochip.wizardry.api.glyph;

import java.util.List;

public record GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis, List<Double> segmentWidths) {
    public GlyphStroke {
        points = List.copyOf(points);
        segmentWidths = List.copyOf(segmentWidths);
        if (points.isEmpty() || points.size() > GlyphLimits.MAX_POINTS_PER_STROKE) throw new IllegalArgumentException("invalid glyph stroke point count");
        if (!Double.isFinite(brushWidth) || brushWidth <= 0 || brushWidth > 32) throw new IllegalArgumentException("invalid glyph brush width");
        if (segmentWidths.size() != Math.max(0, points.size() - 1)) throw new IllegalArgumentException("segment width count must match point count");
        if (segmentWidths.stream().anyMatch(width -> !Double.isFinite(width) || width <= 0 || width > 32)) throw new IllegalArgumentException("invalid segment brush width");
    }

    public GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis) {
        this(points, brushWidth, startedAtMillis,
                java.util.stream.Stream.generate(() -> brushWidth).limit(Math.max(0, points.size() - 1)).toList());
    }

    public double widthAtSegment(int index) {
        return segmentWidths.isEmpty() ? brushWidth : segmentWidths.get(index);
    }
}
