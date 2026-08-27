package dev.mintychochip.merlin.paper.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.Optional;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class RitualRecipeTableTest {
    @Test
    void findsKnownRecipeAndComputesYield() {
        RitualRecipeTable table = new RitualRecipeTable();
        Optional<RitualRecipe> found = table.lookup(Label.fromId("damage"), Material.IRON_INGOT);
        assertTrue(found.isPresent());
        assertEquals(GlyphElement.PHYSICAL, found.get().school());
        assertEquals(1, table.yield(found.get(), 1));
        assertEquals(3, table.yield(found.get(), 5));
    }

    @Test
    void unknownMaterialReturnsEmpty() {
        RitualRecipeTable table = new RitualRecipeTable();
        assertTrue(table.lookup(Label.fromId("damage"), Material.DIRT).isEmpty());
    }

    @Test
    void yieldClampsToMaxAndScalesWithPips() {
        RitualRecipeTable table = new RitualRecipeTable();
        RitualRecipe recipe = table.lookup(Label.fromId("flame"), Material.REDSTONE).orElseThrow();
        assertEquals(1, table.yield(recipe, 1));
        assertEquals(2, table.yield(recipe, 3));
        assertEquals(3, table.yield(recipe, 5));
        assertEquals(3, table.yield(recipe, 10));
    }
}
