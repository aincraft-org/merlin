package dev.mintychochip.merlin.api.glyph;

import java.util.List;

public record GlyphDraft(List<GlyphStroke> strokes) {
    public GlyphDraft {
        strokes = List.copyOf(strokes);
        if (strokes.size() > GlyphLimits.MAX_STROKES) throw new IllegalArgumentException("too many glyph strokes");
    }
    public static GlyphDraft empty() { return new GlyphDraft(List.of()); }
}
