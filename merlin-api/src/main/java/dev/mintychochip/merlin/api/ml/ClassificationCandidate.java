package dev.mintychochip.merlin.api.ml;

public record ClassificationCandidate(Label label, float score) {
    public ClassificationCandidate {
        if (label == null || !Float.isFinite(score) || score < 0 || score > 1) throw new IllegalArgumentException("invalid classification candidate");
    }
}
