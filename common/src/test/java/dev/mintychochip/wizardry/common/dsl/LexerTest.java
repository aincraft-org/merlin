package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.api.dsl.Span;
import org.junit.jupiter.api.Test;

final class LexerTest {
    @Test
    void multibyteDiagnosticsUseUtf8SpansAndScalarColumns() {
        var result = Lexer.lex("é中😀@");
        assertEquals(4, result.diagnostics().size());
        assertEquals(new Span(0, 2, 1, 1), result.diagnostics().get(0).span());
        assertEquals(new Span(2, 5, 1, 2), result.diagnostics().get(1).span());
        assertEquals(new Span(5, 9, 1, 3), result.diagnostics().get(2).span());
        assertEquals(new Span(9, 10, 1, 4), result.diagnostics().get(3).span());
    }

    @Test
    void malformedDecimalIsLexicalFailure() {
        var result = Lexer.lex("spell x { heal self 1.2.3; }");
        assertEquals("E0003", result.diagnostics().getFirst().code());
    }
}
