package dev.mintychochip.merlin.paper.enchanting;

import dev.mintychochip.merlin.paper.enchanting.handler.FortuneOvercapHandler;
import dev.mintychochip.merlin.paper.enchanting.handler.SharpnessOvercapHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public final class EnchantmentRegistry {
    private final Map<NamespacedKey, EnchantmentDefinition> definitions = new ConcurrentHashMap<>();

    public static EnchantmentRegistry defaultRegistry() {
        EnchantmentRegistry reg = new EnchantmentRegistry();
        Set<Material> swords = Set.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        );
        Set<Material> tools = Set.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
        );
        Set<Material> pickaxes = Set.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
        );
        Set<Material> weapons = Set.copyOf(List.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
        ));
        Set<Material> customTools = Set.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
        );
        Set<Material> weaponsAndTools = Set.copyOf(List.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
        ));
        Set<Material> bows = Set.of(Material.BOW);
        Set<Material> leggings = Set.of(
                Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
        );
        Set<Material> leatherArmor = Set.of(
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
        );
        Set<Material> armor = Set.of(
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );


        // Vanilla definitions and their existing handlers remain unchanged.
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("sharpness"), "Sharpness", 5, 7, 0, 5, 10, swords,
                Optional.of(new SharpnessOvercapHandler())));
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("smite"), "Smite", 5, 7, 0, 5, 5, swords,
                Optional.empty()));
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("fortune"), "Fortune", 3, 5, 10, 8, 3, tools,
                Optional.of(new FortuneOvercapHandler())));
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("efficiency"), "Efficiency", 5, 7, 0, 4, 10, tools,
                Optional.empty()));
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("protection"), "Protection", 4, 6, 0, 5, 10, armor,
                Optional.empty()));
        reg.register(new EnchantmentDefinition(
                NamespacedKey.minecraft("unbreaking"), "Unbreaking", 3, 5, 0, 5, 8, swords,
                Optional.empty()));

        registerCustom(reg, "sticky_grip", "Sticky Grip", 1, 5, 5, 10, weaponsAndTools);
        registerCustom(reg, "equilibrium", "Equilibrium", 5, 0, 5, 10, weapons);
        registerCustom(reg, "nethers_scourge", "Nether's Scourge", 6, 0, 5, 10, weapons);
        registerCustom(reg, "cold_aspect", "Cold Aspect", 3, 5, 10, 10, weapons);
        registerCustom(reg, "confusing_aspect", "Confusing Aspect", 3, 5, 10, 10, weapons);
        registerCustom(reg, "toxin_aspect", "Toxin Aspect", 3, 5, 10, 10, weapons);
        registerCustom(reg, "knowledge", "Knowledge", 3, 10, 5, 10, weapons);
        registerCustom(reg, "vorpal", "Vorpal", 3, 10, 5, 10, weapons);
        registerCustom(reg, "vampirism", "Vampirism", 5, 5, 5, 10, weapons);
        registerCustom(reg, "flurry", "Flurry", 3, 10, 5, 10, swords);
        registerCustom(reg, "array", "Array", 2, 10, 10, 10, bows);
        registerCustom(reg, "plunder", "Plunder", 3, 5, 5, 10, bows);
        registerCustom(reg, "wisdom", "Wisdom", 3, 10, 5, 10, bows);
        registerCustom(reg, "drill", "Drill", 3, 10, 5, 10, pickaxes);
        registerCustom(reg, "expertise", "Expertise", 3, 10, 5, 10, pickaxes);
        registerCustom(reg, "quenching", "Quenching", 4, 0, 5, 10, leggings);
        registerCustom(reg, "colorama", "Colorama", 1, 10, 5, 10, leatherArmor);
        registerCustom(reg, "leaping", "Leaping", 3, 5, 5, 10, Set.of(Material.SADDLE));
        registerCustom(reg, "feather_hooves", "Feather Hooves", 1, 5, 5, 10, Set.of(Material.SADDLE));
        registerCustom(reg, "molten_touch", "Molten Touch", 1, 15, 5, 10, customTools);
        registerCustom(reg, "prismatic", "Prismatic", 1, 15, 5, 10, Set.of(Material.SHEARS));
        registerCustom(reg, "overflowing", "Overflowing", 1, 10, 5, 10,
                Set.of(Material.BUCKET, Material.WATER_BUCKET));
        registerCustom(reg, "vacuum", "Vacuum", 1, 10, 5, 10,
                Set.of(Material.BUCKET, Material.WATER_BUCKET));
        registerCustom(reg, "heat_wave", "Heat Wave", 1, 10, 5, 10, Set.of(Material.FLINT_AND_STEEL));
        return reg;
    }

    private static void registerCustom(EnchantmentRegistry registry, String key, String displayName,
                                       int absoluteMaxLevel, int baseEternaRequired, int eternaPerLevel,
                                       int weight, Set<Material> targetMaterials) {
        registry.register(new EnchantmentDefinition(
                new NamespacedKey("merlin", key), displayName, 0, absoluteMaxLevel,
                baseEternaRequired, eternaPerLevel, weight, targetMaterials, Optional.empty()));
    }

    public void register(EnchantmentDefinition def) {
        definitions.put(def.key(), def);
    }

    public Optional<EnchantmentDefinition> get(NamespacedKey key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public List<EnchantmentDefinition> findForMaterial(Material mat) {
        return definitions.values().stream().filter(d -> d.canApplyTo(mat)).toList();
    }

    public List<EnchantmentDefinition> findEligible(Material mat, double eterna) {
        return definitions.values().stream()
                .filter(d -> d.canApplyTo(mat))
                .filter(d -> d.minEternaForLevel(1) <= eterna)
                .toList();
    }
}
