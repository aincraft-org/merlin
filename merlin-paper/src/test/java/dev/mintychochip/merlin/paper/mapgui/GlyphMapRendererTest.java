package dev.mintychochip.merlin.paper.mapgui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

final class GlyphMapRendererTest {
    @Test void mapsEmptyAndFullIntensityToBlackAndWhite() {
        assertEquals(MapPalette.matchColor(new java.awt.Color(0, 0, 0)), GlyphMapRenderer.mapColor((byte) 0));
        assertEquals(MapPalette.matchColor(new java.awt.Color(255, 255, 255)), GlyphMapRenderer.mapColor((byte) 255));
    }

    @Test void preservesPartialIntensityOrdering() {
        assertNotEquals(GlyphMapRenderer.mapColor((byte) 32), GlyphMapRenderer.mapColor((byte) 192));
    }
}
