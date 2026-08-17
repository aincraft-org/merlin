package dev.mintychochip.wizardry.api.dsl;

import java.util.List;
import java.util.Optional;

public record CompileResult(CompiledSpell spell, List<Diagnostic> diagnostics) {
    public CompileResult {
        diagnostics = List.copyOf(diagnostics);
        boolean accepted = spell != null;
        if (accepted == !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("result must contain exactly a spell or diagnostics");
        }
    }

    public static CompileResult accepted(CompiledSpell spell) {
        return new CompileResult(spell, List.of());
    }

    public static CompileResult rejected(List<Diagnostic> diagnostics) {
        return new CompileResult(null, diagnostics);
    }

    public Optional<CompiledSpell> acceptedSpell() {
        return Optional.ofNullable(spell);
    }

    public boolean accepted() {
        return spell != null;
    }
}
