package dev.mintychochip.merlin.common.dsl;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mintychochip.merlin.api.dsl.Action;
import dev.mintychochip.merlin.api.dsl.CompileResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
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
            for (Path fixture : fixtures) {
                compareFixture(JSON.readTree(Files.readString(fixture)));
            }
        }
    }

    private static void compareFixture(JsonNode fixture) {
        assertEquals(2, fixture.path("schemaVersion").asInt());
        var result = ScribeCompiler.INSTANCE.compile(fixture.path("source").asText());
        var expected = fixture.path("result");
        if (expected.path("status").asText().equals("accepted")) {
            assertInstanceOf(CompileResult.Ok.class, result, fixture.path("id").asText());
            var actual = ((CompileResult.Ok) result).spell();
            assertEquals(expected.path("compilerVersion").asText(), actual.compilerVersion());
            assertEquals(expected.path("canonicalHex").asText(), actual.canonicalHex());
            assertEquals(expected.path("identitySha256").asText(), actual.identitySha256());
            var actions = expected.path("actions");
            assertEquals(actions.size(), actual.actions().size());
            for (int i = 0; i < actions.size(); i++) {
                var e = actions.get(i);
                var a = actual.actions().get(i);
                assertEquals(textOrNull(e.path("opcode")), opcode(a));
                assertEquals(textOrNull(e.path("noun")), noun(a));
                assertEquals(textOrNull(e.path("place")), place(a));
                assertEquals(textOrNull(e.path("aheadBits")), aheadBits(a));
                assertEquals(textOrNull(e.path("patient")), patient(a));
                assertEquals(textOrNull(e.path("riding")), riding(a));
                assertEquals(textOrNull(e.path("valueBits")), valueBits(a));
            }
        } else {
            assertInstanceOf(CompileResult.Error.class, result, fixture.path("id").asText());
            var diagnostics = ((CompileResult.Error) result).diagnostics();
            var ds = expected.path("diagnostics");
            assertEquals(ds.size(), diagnostics.size(), fixture.path("id").asText());
            for (int i = 0; i < ds.size(); i++) {
                var e = ds.get(i);
                var a = diagnostics.get(i);
                assertEquals(e.path("code").asText(), a.code());
                assertEquals(e.path("message").asText(), a.message());
                assertEquals(e.path("startByte").asInt(), a.span().startByte());
                assertEquals(e.path("endByte").asInt(), a.span().endByte(), fixture.path("id").asText() + " " + a.code());
                assertEquals(e.path("line").asInt(), a.span().line());
                assertEquals(e.path("column").asInt(), a.span().column());
            }
        }
    }

    private static String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static String opcode(Action action) {
        return switch (action) {
            case Action.LookAhead ignored -> "look_ahead";
            case Action.Summon ignored -> "summon";
            case Action.Burn ignored -> "burn";
            case Action.Mend ignored -> "mend";
            case Action.Shove ignored -> "shove";
            case Action.Strike ignored -> "strike";
            case Action.SendSkyward ignored -> "send_skyward";
            case Action.Vanish ignored -> "vanish";
            case Action.Rest ignored -> "rest";
        };
    }

    private static String noun(Action action) {
        return action instanceof Action.Summon summon ? summon.noun().name().toLowerCase(Locale.ROOT) : null;
    }

    private static Action.Place placeOf(Action action) {
        return switch (action) {
            case Action.Summon summon -> summon.place();
            case Action.Strike strike -> strike.place();
            default -> null;
        };
    }

    private static String place(Action action) {
        var place = placeOf(action);
        if (place == null) {
            return null;
        }
        return switch (place) {
            case Action.Place.Caster ignored -> "caster";
            case Action.Place.Self ignored -> "self";
            case Action.Place.Target ignored -> "target";
            case Action.Place.Ahead ignored -> "ahead";
        };
    }

    private static String aheadBits(Action action) {
        return placeOf(action) instanceof Action.Place.Ahead ahead ? bits(ahead.range()) : null;
    }

    private static String patient(Action action) {
        return switch (action) {
            case Action.Burn x -> x.patient().name().toLowerCase(Locale.ROOT);
            case Action.Mend x -> x.patient().name().toLowerCase(Locale.ROOT);
            case Action.Shove x -> x.patient().name().toLowerCase(Locale.ROOT);
            case Action.Vanish x -> x.patient().name().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private static String riding(Action action) {
        if (action instanceof Action.Summon summon && summon.riding() != null) {
            return summon.riding().name().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static String valueBits(Action action) {
        return switch (action) {
            case Action.LookAhead x -> bits(x.range());
            case Action.Burn x -> bits(x.amount());
            case Action.Mend x -> bits(x.amount());
            case Action.Shove x -> bits(x.amount());
            case Action.Vanish x -> bits(x.seconds());
            case Action.Rest x -> bits(x.seconds());
            default -> null;
        };
    }

    private static String bits(double v) {
        return String.format(Locale.ROOT, "%016x", Double.doubleToRawLongBits(v));
    }
}
