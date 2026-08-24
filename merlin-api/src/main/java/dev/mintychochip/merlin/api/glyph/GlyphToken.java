package dev.mintychochip.merlin.api.glyph;

import dev.mintychochip.merlin.api.ml.Label;

public record GlyphToken(Label label, int pips) {
    public GlyphToken {
        if (label == null) throw new IllegalArgumentException("label");
        if (pips < 1 || pips > 5) throw new IllegalArgumentException("pips");
        if (!GlyphRoles.hasPips(GlyphRoles.of(label)) && pips != 1) {
            throw new IllegalArgumentException("pips");
        }
    }
    public GlyphRole role() { return GlyphRoles.of(label); }
}
