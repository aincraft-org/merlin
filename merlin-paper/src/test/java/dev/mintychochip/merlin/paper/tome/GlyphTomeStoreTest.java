package dev.mintychochip.merlin.paper.tome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GlyphTomeStoreTest {
    @Test void pageLinesRoundTrip() {
        var tokens = List.of(new GlyphToken(Label.FIRE, 1), new GlyphToken(Label.DAMAGE, 5));
        var encoded = GlyphTomeStore.encodePages(tokens);
        assertEquals("fire|1|2\ndamage|5|7", encoded);
        assertEquals(tokens, GlyphTomeStore.decodePages(encoded));
    }
}
