package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class EnchantmentDefinitionTest {
    @Test
    void calculatesMinEternaForLevel() {
        NamespacedKey key = NamespacedKey.minecraft("sharpness");
        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Sharpness", 5, 7, 5, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.empty());
        assertEquals(5, def.minEternaForLevel(1));
        assertEquals(10, def.minEternaForLevel(2));
        assertEquals(25, def.minEternaForLevel(5));
        assertEquals(35, def.minEternaForLevel(7));
    }

    @Test
    void checksMaterialApplicability() {
        NamespacedKey key = NamespacedKey.minecraft("fortune");
        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Fortune", 3, 5, 10, 5, 5, Set.of(Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE), Optional.empty());
        assertTrue(def.canApplyTo(Material.DIAMOND_PICKAXE));
        assertFalse(def.canApplyTo(Material.DIAMOND_SWORD));
    }
}
