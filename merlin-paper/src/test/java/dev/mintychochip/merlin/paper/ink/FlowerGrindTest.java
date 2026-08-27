package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class FlowerGrindTest {
    @Test
    void mapsGenerousFlowerGroupsToElements() {
        Map<Material, GlyphElement> expected = Map.ofEntries(
                Map.entry(Material.TORCHFLOWER, GlyphElement.FLAME),
                Map.entry(Material.POPPY, GlyphElement.FLAME),
                Map.entry(Material.BLUE_ORCHID, GlyphElement.FROST),
                Map.entry(Material.CORNFLOWER, GlyphElement.FROST),
                Map.entry(Material.ALLIUM, GlyphElement.ARCANE),
                Map.entry(Material.WITHER_ROSE, GlyphElement.ARCANE),
                Map.entry(Material.LILAC, GlyphElement.ARCANE),
                Map.entry(Material.OXEYE_DAISY, GlyphElement.PHYSICAL),
                Map.entry(Material.DANDELION, GlyphElement.PHYSICAL),
                Map.entry(Material.AZURE_BLUET, GlyphElement.PHYSICAL),
                Map.entry(Material.SUNFLOWER, GlyphElement.PHYSICAL));

        assertEquals(expected, FlowerGrind.mappings());
        expected.forEach((flower, element) -> assertEquals(element, FlowerGrind.elementFor(flower).orElseThrow()));
    }

    @Test
    void unmappedMaterialsAreNotConsumedAsFlowers() {
        assertTrue(FlowerGrind.elementFor(Material.ROSE_BUSH).isEmpty());
        assertTrue(FlowerGrind.elementFor(Material.BOWL).isEmpty());
        assertTrue(FlowerGrind.elementFor(null).isEmpty());
    }
}
