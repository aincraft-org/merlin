package dev.mintychochip.wizardry.api.dsl;

import dev.mintychochip.wizardry.api.dsl.Span;
import java.util.List;

public record Program(String name, List<Statement> statements, Span span) {
    public Program {
        statements = List.copyOf(statements);
    }
}
