package dev.mintychochip.merlin.api.glyph;

public record GlyphPoint(double x, double y) {
    public GlyphPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x >= GlyphLimits.CANVAS_WIDTH || y < 0 || y >= GlyphLimits.CANVAS_HEIGHT)
            throw new IllegalArgumentException("glyph point outside finite canvas bounds");
    }
}
