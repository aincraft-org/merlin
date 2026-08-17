package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.wizardry.api.dsl.Action;
import dev.mintychochip.wizardry.api.dsl.Phrase;
import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import org.junit.jupiter.api.Test;

final class ParserTest {
    @Test
    void parsesShepherdFlare() {
        var source = "summon sheep\n    riding rocket\nsend skyward\nrest 8 seconds";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.diagnostics().isEmpty());
        var phrases = parsed.page().orElseThrow().phrases();
        assertEquals(3, phrases.size());
        var summon = assertInstanceOf(Phrase.Summon.class, phrases.get(0));
        assertEquals(Action.Noun.SHEEP, summon.noun());
        assertEquals(Action.Noun.ROCKET, summon.riding());
        assertInstanceOf(Action.Place.Caster.class, summon.place());
        assertInstanceOf(Phrase.SendSkyward.class, phrases.get(1));
        assertEquals(8.0, assertInstanceOf(Phrase.Rest.class, phrases.get(2)).seconds());
    }

    @Test
    void strikeWithoutPlaceOrPatientIsError() {
        var parsed = Parser.parse(Lexer.lex("strike"), "strike");
        assertEquals("E0221", parsed.diagnostics().getFirst().code());
    }

    @Test
    void orphanRidingIsError() {
        var parsed = Parser.parse(Lexer.lex("    riding rocket"), "    riding rocket");
        assertEquals("E0217", parsed.diagnostics().getFirst().code());
    }

    @Test
    void depthTwoIndentIsError() {
        var source = "summon sheep\n    riding rocket\n        riding fangs";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.diagnostics().stream().anyMatch(d -> d.code().equals("E0216") || d.code().equals("E0201")));
    }

    @Test
    void retiredSpellWrapperIsRejected() {
        var source = "spell ember { burn target 4 }";
        var parsed = Parser.parse(Lexer.lex(source), source);
        assertTrue(parsed.page().isEmpty());
        assertFalse(parsed.diagnostics().isEmpty());
    }

    @Test
    void emptyTokensRejectWithoutThrowing() {
        var parsed = Parser.parse(Lexer.lex(""), "");
        assertTrue(parsed.diagnostics().stream().allMatch(d -> d.span().line() > 0));
    }
}
