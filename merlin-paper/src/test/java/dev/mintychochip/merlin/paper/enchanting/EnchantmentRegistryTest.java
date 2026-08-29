package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class EnchantmentRegistryTest {
    @Test
    void filtersEligibleByEternaAndMaterial() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        List<EnchantmentDefinition> swordEnchants = registry.findForMaterial(Material.DIAMOND_SWORD);
        assertFalse(swordEnchants.isEmpty());

        // At 0 Eterna, only low base enchants eligible
        List<EnchantmentDefinition> lowEterna = registry.findEligible(Material.DIAMOND_SWORD, 0.0);
        assertTrue(lowEterna.stream().allMatch(d -> d.minEternaForLevel(1) <= 0.0));

        // At 50 Eterna, high tiers eligible
        List<EnchantmentDefinition> highEterna = registry.findEligible(Material.DIAMOND_SWORD, 50.0);
        assertTrue(highEterna.size() >= lowEterna.size());
    }

    @Test
    void preservesVanillaToolTargetEligibility() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();

        for (Material pickaxe : List.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)) {
            assertTrue(registry.get(org.bukkit.NamespacedKey.minecraft("fortune")).orElseThrow()
                    .canApplyTo(pickaxe));
            assertTrue(registry.get(org.bukkit.NamespacedKey.minecraft("efficiency")).orElseThrow()
                    .canApplyTo(pickaxe));
        }
        assertFalse(registry.get(org.bukkit.NamespacedKey.minecraft("fortune")).orElseThrow()
                .canApplyTo(Material.DIAMOND_AXE));
        assertFalse(registry.get(org.bukkit.NamespacedKey.minecraft("efficiency")).orElseThrow()
                .canApplyTo(Material.DIAMOND_SHOVEL));
    }
}
