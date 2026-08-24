package dev.mintychochip.merlin.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import dev.mintychochip.merlin.api.dsl.Action;
import dev.mintychochip.merlin.api.dsl.CompileResult;
import org.junit.jupiter.api.Test;

final class CompilerTest {
    @Test
    void compilesShepherdIdentity() {
        var source = "summon sheep\n    riding rocket\nsend skyward\nrest 8 seconds";
        var result = ScribeCompiler.INSTANCE.compile(source);
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("scribe-compiler/0.2", spell.compilerVersion());
        assertEquals(
                "scribe-compiler/0.2\nsummon|sheep|caster|rocket\nsend_skyward\nrest|4020000000000000",
                new String(spell.canonical(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("a9b2c90b95ee7bc020fbe2c5ecfe1bdcab8fa8da653a783a8af040f85fbb32dd", spell.identitySha256());
        var summon = assertInstanceOf(Action.Summon.class, spell.actions().getFirst());
        assertEquals(Action.Noun.SHEEP, summon.noun());
        assertEquals(Action.Noun.ROCKET, summon.riding());
    }

    @Test
    void blankLinesDoNotChangeIdentity() {
        var compact = ScribeCompiler.INSTANCE.compile("burn target");
        var spaced = ScribeCompiler.INSTANCE.compile("\n\nburn target\n\n");
        var a = assertInstanceOf(CompileResult.Ok.class, compact).spell();
        var b = assertInstanceOf(CompileResult.Ok.class, spaced).spell();
        assertEquals(a.identitySha256(), b.identitySha256());
        assertEquals("43792a1038718ee17e933de2a64c20eff7075f0e16981d83260f4a0b3568502f", a.identitySha256());
    }

    @Test
    void kindlingLookMatchesGoldenIdentity() {
        var result = ScribeCompiler.INSTANCE.compile("look ahead 16\nburn target 4\nrest 3 seconds");
        var spell = assertInstanceOf(CompileResult.Ok.class, result).spell();
        assertEquals("f4997fe3119ebbed9813da7cf263cd44e81983156ca02c148f32cd8e4ff7f969", spell.identitySha256());
    }

    @Test
    void sendWithoutSummonIsError() {
        var result = ScribeCompiler.INSTANCE.compile("send skyward");
        assertEquals("E1014", assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code());
    }

    @Test
    void twoLooksAreError() {
        var result = ScribeCompiler.INSTANCE.compile("look ahead 8\nlook ahead 16\nburn target");
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E1012")));
    }

    @Test
    void pageWithOnlyRestIsError() {
        var result = ScribeCompiler.INSTANCE.compile("rest 3 seconds");
        assertEquals("E1011", assertInstanceOf(CompileResult.Error.class, result).diagnostics().getFirst().code());
    }

    @Test
    void fiveEffectsAreError() {
        var source = "burn target\nmend self\nshove target\nstrike target\nvanish self for 1 seconds";
        var result = ScribeCompiler.INSTANCE.compile(source);
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E1004")));
    }

    @Test
    void acceptsInclusiveBoundaries() {
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(
                "look ahead 1\nburn target 0.5\nmend self 20\nshove target 0.1"));
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(
                "look ahead 32\nshove target 3\nrest 60 seconds"));
    }

    @Test
    void rejectsOutOfRangeMagnitudes() {
        assertEquals("E1001", code("burn target 0.4"));
        assertEquals("E1002", code("shove target 0.05"));
        assertEquals("E1015", code("look ahead 0\nburn target"));
        assertEquals("E1016", code("vanish self for 0 seconds"));
    }

    @Test
    void retiredSpellSourceIsError() {
        var result = ScribeCompiler.INSTANCE.compile("spell ember { target ray 16; damage target fire 4; }");
        assertInstanceOf(CompileResult.Error.class, result);
    }

    @Test
    void sourceLimitsUseScalarsAndUtf8Bytes() {
        String valid = "burn target" + " ".repeat(4096 - "burn target".codePointCount(0, "burn target".length()));
        assertInstanceOf(CompileResult.Ok.class, ScribeCompiler.INSTANCE.compile(valid));
        var tooMany = ScribeCompiler.INSTANCE.compile("é".repeat(4097));
        assertEquals("E0004", assertInstanceOf(CompileResult.Error.class, tooMany).diagnostics().getFirst().code());
    }

    @Test
    void rejectsMultibyteUtf8OverflowWithoutThrowing() {
        var result = ScribeCompiler.INSTANCE.compile("é".repeat(8193));
        assertTrue(assertInstanceOf(CompileResult.Error.class, result).diagnostics().stream()
                .anyMatch(d -> d.code().equals("E0005")));
    }

    private static String code(String source) {
        return assertInstanceOf(CompileResult.Error.class, ScribeCompiler.INSTANCE.compile(source))
                .diagnostics().getFirst().code();
    }
}
