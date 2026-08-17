package dev.mintychochip.wizardry.api.ml;

import java.util.List;

public record Classification(List<ClassificationCandidate> candidates, boolean accepted) {
    public Classification {
        candidates = List.copyOf(candidates);
        if (candidates.size() > 32) throw new IllegalArgumentException("too many candidates");
    }
    public static Classification rejected(List<ClassificationCandidate> candidates) { return new Classification(candidates, false); }
}
