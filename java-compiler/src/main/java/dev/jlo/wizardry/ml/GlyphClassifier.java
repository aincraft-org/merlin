package dev.jlo.wizardry.ml;

import dev.jlo.wizardry.glyph.GlyphDraft;

@FunctionalInterface
public interface GlyphClassifier {
    Classification classify(GlyphDraft draft);
}
