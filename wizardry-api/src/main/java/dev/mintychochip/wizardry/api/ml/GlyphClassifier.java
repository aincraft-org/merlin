package dev.mintychochip.wizardry.api.ml;

import dev.mintychochip.wizardry.api.glyph.GlyphDraft;

@FunctionalInterface
public interface GlyphClassifier {
    Classification classify(GlyphDraft draft);
}
