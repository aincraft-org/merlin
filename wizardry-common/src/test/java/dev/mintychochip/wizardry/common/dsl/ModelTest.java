package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import dev.mintychochip.wizardry.api.dsl.CompiledSpell;
import dev.mintychochip.wizardry.api.dsl.Diagnostic;
import dev.mintychochip.wizardry.api.dsl.Span;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModelTest {
    @Test
    void compiledSpellDefensivelyCopiesActionsAndCanonicalBytes() {
        var actions = new ArrayList<Action>();
        actions.add(new Action.Mend(Action.Patient.SELF, 1.0));
        byte[] canonical = {1, 2, 3};
        var spell = new CompiledSpell("scribe-compiler/0.2", "00", canonical, actions);

        actions.clear();
        canonical[0] = 9;

        assertEquals(1, spell.actions().size());
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
    void compileResultOkContainsTheSpell() {
        var spell = new CompiledSpell("v", "00", new byte[0], List.of());
        CompileResult result = new CompileResult.Ok(spell);

        assertInstanceOf(CompileResult.Ok.class, result);
        assertEquals(spell, ((CompileResult.Ok) result).spell());
    }

    @Test
    void compileResultErrorContainsDiagnostics() {
        var diagnostic = new Diagnostic("E0001", "bad", new Span(0, 0, 1, 1));
        var diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(diagnostic);
        CompileResult result = new CompileResult.Error(diagnostics);

        diagnostics.clear();
        assertInstanceOf(CompileResult.Error.class, result);
        assertEquals(List.of(diagnostic), ((CompileResult.Error) result).diagnostics());
    }

    @Test
    void compileResultVariantsRejectEmptyPayloads() {
        assertThrows(NullPointerException.class, () -> new CompileResult.Ok(null));
        assertThrows(IllegalArgumentException.class, () -> new CompileResult.Error(List.of()));
        assertThrows(NullPointerException.class, () -> new CompileResult.Error(null));
    }
}
