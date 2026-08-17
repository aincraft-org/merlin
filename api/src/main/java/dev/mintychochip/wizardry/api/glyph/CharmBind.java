package dev.mintychochip.wizardry.api.glyph;

import dev.mintychochip.wizardry.api.ml.Label;

public record CharmBind(Label label, int rank) {
    public CharmBind {
        if (label == null) throw new IllegalArgumentException("label");
    }
}
