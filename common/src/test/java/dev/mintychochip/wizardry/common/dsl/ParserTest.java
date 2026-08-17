package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.wizardry.common.dsl.lexer.Lexer;
import dev.mintychochip.wizardry.api.dsl.Operation;
import dev.mintychochip.wizardry.common.dsl.parser.Parser;
import dev.mintychochip.wizardry.api.dsl.Statement;
import org.junit.jupiter.api.Test;

final class ParserTest {
    @Test
    void parsesTheReferenceGrammar() {
        var source = "spell ember { target ray 16; damage target fire 4; push target 0.6; cooldown 3s; }";
        var parsed = Parser.parse(Lexer.lex(source).tokens(), source);
        assertTrue(parsed.diagnostics().isEmpty());
        assertEquals("ember", parsed.program().orElseThrow().name());
        assertEquals(4, parsed.program().orElseThrow().statements().size());
        var damage = assertInstanceOf(Statement.Damage.class,
                parsed.program().orElseThrow().statements().get(1));
        assertEquals(Operation.DamageType.FIRE, damage.damageType());
        assertEquals(4.0, damage.amount());
    }

    @Test
    void missingSemicolonReportsTheCurrentToken() {
        var source = "spell x { heal self 5 }";
        var parsed = Parser.parse(Lexer.lex(source).tokens(), source);
        assertEquals("E0102", parsed.diagnostics().getFirst().code());
        assertEquals(source.indexOf('}'), parsed.diagnostics().getFirst().span().startByte());
    }

    @Test
    void emptyTokensRejectWithoutThrowing() {
        var parsed = Parser.parse(java.util.List.of(), "");
        assertTrue(parsed.program().isEmpty());
        assertTrue(parsed.diagnostics().stream().allMatch(d -> d.span().line() > 0));
    }
}
