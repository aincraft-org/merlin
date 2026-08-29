package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentRegistryTest {
    private static final Map<String, Expected> EXPECTED = Map.ofEntries(
            entry("sticky_grip", "Sticky Grip", 1),
            entry("equilibrium", "Equilibrium", 5),
            entry("nethers_scourge", "Nether's Scourge", 6),
            entry("cold_aspect", "Cold Aspect", 3),
            entry("confusing_aspect", "Confusing Aspect", 3),
            entry("toxin_aspect", "Toxin Aspect", 3),
            entry("knowledge", "Knowledge", 3),
            entry("vorpal", "Vorpal", 3),
            entry("vampirism", "Vampirism", 5),
            entry("flurry", "Flurry", 3),
            entry("array", "Array", 2),
            entry("plunder", "Plunder", 3),
            entry("wisdom", "Wisdom", 3),
            entry("molten_touch", "Molten Touch", 1),
            entry("drill", "Drill", 3),
            entry("expertise", "Expertise", 3),
            entry("quenching", "Quenching", 4),
            entry("colorama", "Colorama", 1),
            entry("leaping", "Leaping", 3),
            entry("feather_hooves", "Feather Hooves", 1),
            entry("prismatic", "Prismatic", 1),
            entry("overflowing", "Overflowing", 1),
            entry("vacuum", "Vacuum", 1),
            entry("heat_wave", "Heat Wave", 1)
    );

    @Test
    void registersAllArchivedDefinitionsWithExactNamesAndMaximums() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();

        assertEquals(EXPECTED.size(), EXPECTED.keySet().stream()
                .map(key -> registry.get(key(key)).orElse(null))
                .filter(java.util.Objects::nonNull)
                .count());
        for (var entry : EXPECTED.entrySet()) {
            EnchantmentDefinition definition = registry.get(key(entry.getKey())).orElse(null);
            assertNotNull(definition, entry.getKey());
            assertEquals(entry.getValue().displayName(), definition.displayName());
            assertEquals(entry.getValue().maximum(), definition.absoluteMaxLevel());
            assertEquals(0, definition.vanillaMaxLevel());
            assertTrue(definition.overcapHandler().isEmpty());
        }
        assertTrue(registry.get(key("expertise")).orElseThrow().overcapHandler().isEmpty());
        assertEquals(2, registry.get(key("array")).orElseThrow().absoluteMaxLevel());
    }

    @Test
    void usesCompleteArchivedTargetCategories() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        assertTargets(registry, "sticky_grip", Set.of(Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
                Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE), Material.BOW);
        assertTargets(registry, "equilibrium", Set.of(Material.DIAMOND_SWORD, Material.DIAMOND_AXE), Material.BOW);
        assertTargets(registry, "molten_touch", Set.of(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE,
                Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE), Material.DIAMOND_SWORD);
        assertTargets(registry, "drill", Set.of(Material.NETHERITE_PICKAXE), Material.NETHERITE_AXE);
        assertTargets(registry, "quenching", Set.of(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS,
                Material.NETHERITE_LEGGINGS), Material.DIAMOND_CHESTPLATE);
        assertTargets(registry, "colorama", Set.of(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS), Material.DIAMOND_CHESTPLATE);
        assertTargets(registry, "leaping", Set.of(Material.SADDLE), Material.LEATHER_BOOTS);
        assertTargets(registry, "prismatic", Set.of(Material.SHEARS), Material.IRON_SWORD);
        assertTargets(registry, "overflowing", Set.of(Material.BUCKET, Material.WATER_BUCKET), Material.SHEARS);
        assertTargets(registry, "heat_wave", Set.of(Material.FLINT_AND_STEEL), Material.BUCKET);
    }

    private static void assertTargets(EnchantmentRegistry registry, String name,
                                      Set<Material> accepted, Material rejected) {
        EnchantmentDefinition definition = registry.get(key(name)).orElseThrow();
        for (Material material : accepted) assertTrue(definition.canApplyTo(material), name + " accepts " + material);
        assertFalse(definition.canApplyTo(rejected), name + " rejects " + rejected);
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("merlin", name);
    }

    private static Map.Entry<String, Expected> entry(String key, String name, int maximum) {
        return Map.entry(key, new Expected(name, maximum));
    }

    private record Expected(String displayName, int maximum) {}
}
