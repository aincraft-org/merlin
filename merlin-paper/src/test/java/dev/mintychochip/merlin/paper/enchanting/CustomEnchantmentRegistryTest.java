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
            entry("heat_wave", "Heat Wave", 1),
            entry("telepathy", "Telepathy", 1),
            entry("timber", "Timber", 3),
            entry("trench", "Trench", 3),
            entry("replanter", "Replanter", 1),
            entry("planter", "Planter", 1),
            entry("carrot_planter", "Carrot Planter", 1),
            entry("potato_planter", "Potato Planter", 1),
            entry("experience", "Experience", 3),
            entry("rebreather", "Rebreather", 3),
            entry("replenish", "Replenish", 3),
            entry("unbreakable", "Unbreakable", 1),
            entry("reforged", "Reforged", 5),
            entry("aegis", "Aegis", 3),
            entry("angelic", "Angelic", 3),
            entry("armored", "Armored", 3),
            entry("chunky", "Chunky", 3),
            entry("dodge", "Dodge", 3),
            entry("heavy", "Heavy", 3),
            entry("molten", "Molten", 3),
            entry("reflect", "Reflect", 3),
            entry("safeguard", "Safeguard", 3),
            entry("tank", "Tank", 3),
            entry("bleed", "Bleed", 3),
            entry("blind", "Blind", 3),
            entry("block", "Block", 3),
            entry("berserk", "Berserk", 3),
            entry("critical", "Critical", 3),
            entry("double_strike", "Double Strike", 3),
            entry("thunderlord", "Thunderlord", 3),
            entry("archer", "Archer", 3),
            entry("marksman", "Marksman", 3),
            entry("sniper", "Sniper", 3),
            entry("auto_reel", "Auto Reel", 3),
            entry("bait", "Bait", 3),
            entry("hook", "Hook", 3),
            entry("snap", "Snap", 3),
            entry("lava_walker", "Lava Walker", 3),
            entry("water_walker", "Water Walker", 3),
            entry("plummet", "Plummet", 3),
            entry("jelly_legs", "Jelly Legs", 3),
            entry("gears", "Gears", 3),
            entry("springs", "Springs", 3),
            entry("aquatic", "Aquatic", 3),
            entry("glowing", "Glowing", 3),
            entry("implants", "Implants", 3),
            entry("obsidianshield", "Obsidianshield", 3),
            entry("overload", "Overload", 3),
            entry("wings", "Wings", 3)
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
            if (entry.getKey().equals("expertise")) {
                assertTrue(definition.overcapHandler().isEmpty());
            } else {
                assertTrue(definition.overcapHandler().isPresent(), entry.getKey());
                assertEquals(key(entry.getKey()), definition.overcapHandler().orElseThrow().key());
            }
        }
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
        assertTargets(registry, "telepathy", Set.of(Material.DIAMOND_PICKAXE), Material.DIAMOND_SWORD);
        assertTargets(registry, "timber", Set.of(Material.DIAMOND_AXE), Material.DIAMOND_PICKAXE);
        assertTargets(registry, "trench", Set.of(Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL),
                Material.DIAMOND_SWORD);
        assertTargets(registry, "replanter", Set.of(Material.DIAMOND_HOE), Material.DIAMOND_PICKAXE);
        Set<Material> armorMaterials = Set.of(
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
        );
        for (String name : Set.of("aegis", "angelic", "armored", "chunky", "dodge",
                "heavy", "molten", "reflect", "safeguard", "tank")) {
            assertTargets(registry, name, armorMaterials, Material.DIAMOND_SWORD);
        }
        assertTargets(registry, "planter", Set.of(Material.DIAMOND_HOE), Material.DIAMOND_PICKAXE);
        assertTargets(registry, "carrot_planter", Set.of(Material.DIAMOND_HOE), Material.DIAMOND_PICKAXE);
        assertTargets(registry, "potato_planter", Set.of(Material.DIAMOND_HOE), Material.DIAMOND_PICKAXE);
        assertTargets(registry, "experience", Set.of(Material.DIAMOND_PICKAXE), Material.DIAMOND_SWORD);
        assertTargets(registry, "rebreather", Set.of(Material.DIAMOND_PICKAXE), Material.DIAMOND_SWORD);
        assertTargets(registry, "replenish", Set.of(Material.DIAMOND_PICKAXE), Material.DIAMOND_SWORD);
        assertTargets(registry, "unbreakable",
                Set.of(Material.DIAMOND_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT),
                Material.DIAMOND_CHESTPLATE);
        assertTargets(registry, "reforged",
                Set.of(Material.DIAMOND_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT),
                Material.DIAMOND_CHESTPLATE);
    }

    @Test
    void disabledKeysRemainVisibleButAreExcludedFromQueries() {
        Set<NamespacedKey> disabled = Set.of(key("expertise"), key("heat_wave"));
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry(disabled);

        assertTrue(registry.isDisabled(key("expertise")));
        assertTrue(registry.isDisabled(key("heat_wave")));

        // Still visible for admin/debug inspection
        assertTrue(registry.get(key("expertise")).isPresent());
        assertTrue(registry.get(key("heat_wave")).isPresent());

        // Blocked from offers / applications
        assertEquals(0, registry.findForMaterial(Material.FLINT_AND_STEEL).stream()
                .filter(d -> disabled.contains(d.key())).count());

        Material diamondSword = Material.DIAMOND_SWORD;
        Set<NamespacedKey> expected = Set.of(key("equilibrium"), key("vampirism"));
        assertTrue(registry.findEligible(diamondSword, 100.0).stream()
                .anyMatch(d -> expected.contains(d.key())));
        assertEquals(0, registry.findEligible(diamondSword, 100.0).stream()
                .filter(d -> disabled.contains(d.key())).count());
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
