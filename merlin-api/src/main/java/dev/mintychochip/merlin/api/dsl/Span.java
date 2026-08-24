package dev.mintychochip.merlin.api.dsl;

public record Span(int startByte, int endByte, int line, int column) {
    public Span {
        if (startByte < 0 || endByte < startByte) {
            throw new IllegalArgumentException("invalid UTF-8 byte span");
        }
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line and column must be one-based");
        }
    }
}
