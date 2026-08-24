package dev.mintychochip.merlin.paper.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.ml.Label;
import dev.mintychochip.merlin.common.glyph.GlyphCompilerImpl;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class CharmBinderTest {
    @Test void sharpnessThreeIsLevelThree() {
        var bind = GlyphCompilerImpl.INSTANCE.charm(new GlyphToken(Label.SHARPNESS, 3)).orElseThrow();
        assertEquals(3, CharmBinder.level(bind));
        assertTrue(CharmBinder.canHost(Material.DIAMOND_SWORD));
        assertFalse(CharmBinder.canHost(Material.WOODEN_HOE));
        assertFalse(CharmBinder.canHost(Material.BOOK));
    }
}
