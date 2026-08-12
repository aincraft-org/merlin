package dev.jlo.wizardry.scribe.model;

import java.util.Objects;

public record Diagnostic(String code, String message, Span span) {
    public Diagnostic {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        span = Objects.requireNonNull(span, "span");
    }
}
