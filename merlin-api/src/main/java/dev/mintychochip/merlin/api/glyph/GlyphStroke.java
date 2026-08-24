package dev.mintychochip.merlin.api.glyph;

import java.util.List;
import java.util.Objects;

public record GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis, List<Double> segmentWidths, GlyphElement element) {
    public GlyphStroke {
        points = List.copyOf(points);
        segmentWidths = List.copyOf(segmentWidths);
        element = element == null ? GlyphElement.PHYSICAL : element;
        if (points.isEmpty() || points.size() > GlyphLimits.MAX_POINTS_PER_STROKE) throw new IllegalArgumentException("invalid glyph stroke point count");
        if (!Double.isFinite(brushWidth) || brushWidth <= 0 || brushWidth > 32) throw new IllegalArgumentException("invalid glyph brush width");
        if (segmentWidths.size() != Math.max(0, points.size() - 1)) throw new IllegalArgumentException("segment width count must match point count");
        if (segmentWidths.stream().anyMatch(width -> !Double.isFinite(width) || width <= 0 || width > 32)) throw new IllegalArgumentException("invalid segment brush width");
        Objects.requireNonNull(element);
    }

    public GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis) {
        this(points, brushWidth, startedAtMillis,
                java.util.stream.Stream.generate(() -> brushWidth).limit(Math.max(0, points.size() - 1)).toList(),
                GlyphElement.PHYSICAL);
    }

    public GlyphStroke(List<GlyphPoint> points, double brushWidth, long startedAtMillis, List<Double> segmentWidths) {
        this(points, brushWidth, startedAtMillis, segmentWidths, GlyphElement.PHYSICAL);
    }

    public double widthAtSegment(int index) {
        return segmentWidths.isEmpty() ? brushWidth : segmentWidths.get(index);
    }
}
