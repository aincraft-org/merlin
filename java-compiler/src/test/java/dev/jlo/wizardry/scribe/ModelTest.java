package dev.jlo.wizardry.scribe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jlo.wizardry.scribe.model.CompileResult;
import dev.jlo.wizardry.scribe.model.CompiledSpell;
import dev.jlo.wizardry.scribe.model.Diagnostic;
import dev.jlo.wizardry.scribe.model.Operation;
import dev.jlo.wizardry.scribe.model.Span;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModelTest {
    @Test
    void compiledSpellDefensivelyCopiesOperationsAndCanonicalBytes() {
        var operations = new ArrayList<Operation>();
        operations.add(new Operation.Heal(Operation.Target.SELF, 1.0));
        byte[] canonical = {1, 2, 3};
        var spell = new CompiledSpell("scribe-compiler/0.1", "mend", "00", canonical, operations);

        operations.clear();
        canonical[0] = 9;

        assertEquals(1, spell.operations().size());
        assertEquals(1, spell.canonical()[0]);
    }

    @Test
    void spanRejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new Span(-1, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Span(2, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Span(0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Span(0, 0, 1, 0));
    }

    @Test
    void compileResultCannotMixSuccessAndDiagnostics() {
        var diagnostic = new Diagnostic("E0001", "bad", new Span(0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CompileResult(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CompileResult(
                new CompiledSpell("v", "x", "00", new byte[0], List.of()),
                List.of(diagnostic)));
    }
}
