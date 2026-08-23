package dev.mintychochip.wizardry.api.dsl;

import java.util.List;
import java.util.Objects;

public sealed interface CompileResult permits CompileResult.Ok, CompileResult.Error {
    record Ok(CompiledSpell spell) implements CompileResult {
        public Ok {
            spell = Objects.requireNonNull(spell, "spell");
        }
    }

    record Error(List<Diagnostic> diagnostics) implements CompileResult {
        public Error {
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
            if (diagnostics.isEmpty()) {
                throw new IllegalArgumentException("error must contain diagnostics");
            }
        }
    }

    static CompileResult ok(CompiledSpell spell) {
        return new Ok(spell);
    }

    static CompileResult error(List<Diagnostic> diagnostics) {
        return new Error(diagnostics);
    }
}
