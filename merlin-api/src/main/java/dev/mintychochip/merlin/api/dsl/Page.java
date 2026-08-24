package dev.mintychochip.merlin.api.dsl;

import java.util.List;

public record Page(List<Phrase> phrases, Span span) {
    public Page {
        phrases = List.copyOf(phrases);
    }
}
