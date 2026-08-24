package dev.mintychochip.merlin.api.ml;

import dev.mintychochip.merlin.api.glyph.GlyphDraft;

@FunctionalInterface
public interface GlyphClassifier {
    Classification classify(GlyphDraft draft);
}
