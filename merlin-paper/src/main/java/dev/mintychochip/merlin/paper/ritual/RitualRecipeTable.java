package dev.mintychochip.merlin.paper.ritual;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.ml.Label;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class RitualRecipeTable {
    private static final Map<RitualKey, RitualRecipe> RECIPES = Map.ofEntries(
            Map.entry(new RitualKey(Label.fromId("damage"), Material.IRON_INGOT),
                    new RitualRecipe(Label.fromId("damage"), Material.IRON_INGOT, GlyphElement.PHYSICAL,
                            Material.STONE, Material.IRON_INGOT, 1, 3)),
            Map.entry(new RitualKey(Label.fromId("heal"), Material.GOLD_INGOT),
                    new RitualRecipe(Label.fromId("heal"), Material.GOLD_INGOT, GlyphElement.PHYSICAL,
                            Material.GOLD_BLOCK, Material.GOLD_INGOT, 1, 3)),
            Map.entry(new RitualKey(Label.fromId("push"), Material.COPPER_INGOT),
                    new RitualRecipe(Label.fromId("push"), Material.COPPER_INGOT, GlyphElement.PHYSICAL,
                            Material.PISTON, Material.COPPER_INGOT, 1, 3)),
            Map.entry(new RitualKey(Label.fromId("flame"), Material.REDSTONE),
                    new RitualRecipe(Label.fromId("flame"), Material.REDSTONE, GlyphElement.FLAME,
                            Material.COAL_BLOCK, Material.REDSTONE, 1, 3)),
            Map.entry(new RitualKey(Label.fromId("frost"), Material.AMETHYST_SHARD),
                    new RitualRecipe(Label.fromId("frost"), Material.AMETHYST_SHARD, GlyphElement.FROST,
                            Material.SNOW_BLOCK, Material.AMETHYST_SHARD, 1, 3))
    );

    public Optional<RitualRecipe> lookup(Label word, Material material) {
        return Optional.ofNullable(RECIPES.get(new RitualKey(word, material)));
    }

    public int yield(RitualRecipe recipe, int pips) {
        return Math.min(recipe.maxYield(), recipe.baseYield() + (pips - 1) / 2);
    }

    private record RitualKey(Label word, Material material) {}
}
