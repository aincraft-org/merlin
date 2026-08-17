package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.wizardry.api.dsl.Operation;
import org.junit.jupiter.api.Test;

final class CompilerTest {
    @Test
    void compilesCanonicalOperationsAndIdentity() {
        var result = ScribeCompiler.INSTANCE.compile("spell ember { target ray 16; damage target fire 4; push target 0.6; cooldown 3s; }");
        assertTrue(result.accepted());
        var spell = result.acceptedSpell().orElseThrow();
        assertEquals("ember", spell.name());
        assertEquals("scribe-compiler/0.1|spell|ember|\ntarget_ray|4030000000000000\ndamage|target|fire|4010000000000000\npush|target|3fe3333333333333\ncooldown|4008000000000000", new String(spell.canonical(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(4, spell.operations().size());
        assertEquals("6fbc35e3c8289bcad65bc7d63dadf0bc89a2880601e8c55f48b0c6f6ee2429f6", spell.identitySha256());
    }

    @Test
    void rejectsInvalidValuesAtomically() {
        var result = ScribeCompiler.INSTANCE.compile("spell x { damage target fire 0.4; }");
        assertFalse(result.accepted());
        assertTrue(result.acceptedSpell().isEmpty());
        assertEquals("E1001", result.diagnostics().getFirst().code());
    }

    @Test
    void acceptsInclusiveBoundaries() {
        assertTrue(ScribeCompiler.INSTANCE.compile("spell x { target ray 1; damage target physical 0.5; heal self 20; push target 0.1; cooldown 60s; }").accepted());
    }

    @Test
    void rejectsTooManyStatementsAndEffects() {
        String source = "spell x { damage target fire 1; damage target fire 1; damage target fire 1; damage target fire 1; damage target fire 1; }";
        var result = ScribeCompiler.INSTANCE.compile(source);
        assertFalse(result.accepted());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("E1004")));
    }

    @Test
    void sourceLimitsUseScalarsAndUtf8Bytes() {
        String valid = " ".repeat(4096 - "spell x { heal self 1; }".codePointCount(0, "spell x { heal self 1; }".length())) + "spell x { heal self 1; }";
        assertTrue(ScribeCompiler.INSTANCE.compile(valid).accepted());
        var tooMany = ScribeCompiler.INSTANCE.compile("é".repeat(4097));
        assertFalse(tooMany.accepted());
        assertEquals("E0004", tooMany.diagnostics().getFirst().code());
    }
    @Test
    void rejectsMultibyteUtf8OverflowWithoutThrowing() {
        var result = ScribeCompiler.INSTANCE.compile("é".repeat(8193));
        assertFalse(result.accepted());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("E0005")));
    }
}
