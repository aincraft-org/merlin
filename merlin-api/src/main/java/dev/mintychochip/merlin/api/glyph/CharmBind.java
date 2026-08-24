package dev.mintychochip.merlin.api.glyph;

import dev.mintychochip.merlin.api.ml.Label;

public record CharmBind(Label label, int rank) {
    public CharmBind {
        if (label == null) throw new IllegalArgumentException("label");
    }
}
