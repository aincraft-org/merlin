package dev.mintychochip.wizardry.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mintychochip.wizardry.api.dsl.CompileResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class ConformanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void everyFixtureMatchesExactly() throws Exception {
        var resource = ConformanceTest.class.getClassLoader().getResource("conformance/fixtures");
        assertNotNull(resource);
        Path root = Path.of(resource.toURI());
        try (Stream<Path> files = Files.list(root)) {
            var fixtures = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            assertFalse(fixtures.isEmpty());
            for (Path fixture : fixtures) compareFixture(JSON.readTree(Files.readString(fixture)));
        }
    }

    private static void compareFixture(JsonNode fixture) throws Exception {
        assertEquals(1, fixture.path("schemaVersion").asInt());
        var result = ScribeCompiler.INSTANCE.compile(fixture.path("source").asText());
        var expected = fixture.path("result");
        if (expected.path("status").asText().equals("accepted")) {
            assertInstanceOf(CompileResult.Ok.class, result, fixture.path("id").asText());
            var actual = ((CompileResult.Ok) result).spell();
            assertEquals(expected.path("compilerVersion").asText(), actual.compilerVersion());
            assertEquals(expected.path("name").asText(), actual.name());
            assertEquals(expected.path("canonicalHex").asText(), actual.canonicalHex());
            assertEquals(expected.path("identitySha256").asText(), actual.identitySha256());
            var ops = expected.path("operations");
            assertEquals(ops.size(), actual.operations().size());
            for (int i = 0; i < ops.size(); i++) {
                var e = ops.get(i); var a = actual.operations().get(i);
                assertEquals(e.path("opcode").asText(), opcode(a));
                assertEquals(e.path("target").isNull() ? null : e.path("target").asText(), target(a));
                assertEquals(e.path("damageType").isNull() ? null : e.path("damageType").asText(), damageType(a));
                assertEquals(e.path("valueBits").isNull() ? null : e.path("valueBits").asText(), valueBits(a));
            }
        } else {
            assertInstanceOf(CompileResult.Error.class, result, fixture.path("id").asText());
            var diagnostics = ((CompileResult.Error) result).diagnostics();
            var ds = expected.path("diagnostics");
            assertEquals(ds.size(), diagnostics.size());
            for (int i = 0; i < ds.size(); i++) {
                var e = ds.get(i); var a = diagnostics.get(i);
                assertEquals(e.path("code").asText(), a.code());
                assertEquals(e.path("message").asText(), a.message());
                assertEquals(e.path("startByte").asInt(), a.span().startByte());
                assertEquals(e.path("line").asInt(), a.span().line());
                assertEquals(e.path("endByte").asInt(), a.span().endByte(), fixture.path("id").asText() + " " + a.code());
            }
        }
    }
    private static String opcode(dev.mintychochip.wizardry.api.dsl.Operation o) { return switch (o) {
        case dev.mintychochip.wizardry.api.dsl.Operation.TargetRay ignored -> "target_ray";
        case dev.mintychochip.wizardry.api.dsl.Operation.Damage ignored -> "damage";
        case dev.mintychochip.wizardry.api.dsl.Operation.Heal ignored -> "heal";
        case dev.mintychochip.wizardry.api.dsl.Operation.Push ignored -> "push";
        case dev.mintychochip.wizardry.api.dsl.Operation.Cooldown ignored -> "cooldown";
    }; }
    private static String target(dev.mintychochip.wizardry.api.dsl.Operation o) { return switch (o) {
        case dev.mintychochip.wizardry.api.dsl.Operation.Damage x -> x.target().name().toLowerCase();
        case dev.mintychochip.wizardry.api.dsl.Operation.Heal x -> x.target().name().toLowerCase();
        case dev.mintychochip.wizardry.api.dsl.Operation.Push x -> x.target().name().toLowerCase();
        default -> null;
    }; }
    private static String damageType(dev.mintychochip.wizardry.api.dsl.Operation o) { return o instanceof dev.mintychochip.wizardry.api.dsl.Operation.Damage x ? x.damageType().name().toLowerCase() : null; }
    private static String valueBits(dev.mintychochip.wizardry.api.dsl.Operation o) { return switch (o) {
        case dev.mintychochip.wizardry.api.dsl.Operation.TargetRay x -> bits(x.range());
        case dev.mintychochip.wizardry.api.dsl.Operation.Damage x -> bits(x.amount());
        case dev.mintychochip.wizardry.api.dsl.Operation.Heal x -> bits(x.amount());
        case dev.mintychochip.wizardry.api.dsl.Operation.Push x -> bits(x.strength());
        case dev.mintychochip.wizardry.api.dsl.Operation.Cooldown x -> bits(x.seconds());
    }; }
    private static String bits(double v) { return String.format(java.util.Locale.ROOT, "%016x", Double.doubleToRawLongBits(v)); }
}
