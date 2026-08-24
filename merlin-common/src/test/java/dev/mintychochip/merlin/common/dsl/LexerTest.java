package dev.mintychochip.merlin.common.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.dsl.Span;
import dev.mintychochip.merlin.common.dsl.lexer.Lexer;
import org.junit.jupiter.api.Test;

final class LexerTest {
    @Test
    void splitsLinesAndRecordsFourSpaceIndent() {
        var result = Lexer.lex("summon sheep\n    riding rocket\n");
        assertTrue(result.diagnostics().isEmpty());
        assertEquals(2, result.lines().size());
        assertEquals(0, result.lines().get(0).indentSpaces());
        assertEquals(4, result.lines().get(1).indentSpaces());
        assertEquals("summon", result.lines().get(0).tokens().get(0).text());
        assertEquals("riding", result.lines().get(1).tokens().get(0).text());
    }

    @Test
    void ignoresBlankLines() {
        var result = Lexer.lex("burn target\n\nrest 3 seconds");
        assertEquals(2, result.lines().size());
    }

    @Test
    void tabIndentIsDiagnostic() {
        var result = Lexer.lex("summon sheep\n\triding rocket");
        assertEquals("E0200", result.diagnostics().getFirst().code());
    }

    @Test
    void twoSpaceIndentIsDiagnostic() {
        var result = Lexer.lex("summon sheep\n  riding rocket");
        assertEquals("E0201", result.diagnostics().getFirst().code());
    }

    @Test
    void uppercaseWordIsDiagnostic() {
        var result = Lexer.lex("Burn target");
        assertEquals("E0202", result.diagnostics().getFirst().code());
    }

    @Test
    void bracesAndSemicolonsAreUnexpectedCharacters() {
        var result = Lexer.lex("spell ember { burn target; }");
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("E0002") || d.code().equals("E0222")));
    }

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
        var result = Lexer.lex("burn target 1.2.3");
        assertEquals("E0003", result.diagnostics().getFirst().code());
    }
}
