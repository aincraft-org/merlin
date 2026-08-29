package dev.mintychochip.merlin.paper.enchanting;

import dev.mintychochip.merlin.paper.enchanting.handler.FortuneOvercapHandler;
import dev.mintychochip.merlin.paper.enchanting.handler.SharpnessOvercapHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ArrayHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.AegisHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.AngelicHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ArmoredHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ChunkyHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.CarrotPlanterHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ColdAspectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ColoramaHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ConfusingAspectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.DrillHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.EquilibriumHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ExperienceHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.FeatherHoovesHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.FlurryHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.HeatWaveHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.KnowledgeHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.LeapingHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.DodgeHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.MoltenTouchHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.NetherScourgeHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.OverflowingHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.HeavyHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.PlanterHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.PlunderHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.PotatoPlanterHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.PrismaticHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.QuenchingHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ReforgedHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.MoltenHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ReplanterHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ReplenishHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.RebreatherHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.StickyGripHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ReflectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.TelepathyHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.TimberHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.SafeguardHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.ToxinAspectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.TrenchHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.TankHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.UnbreakableHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.VacuumHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.VampirismHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.VorpalHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.handler.WisdomHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public final class EnchantmentRegistry {
    private final Map<NamespacedKey, EnchantmentDefinition> definitions = new ConcurrentHashMap<>();
    private final Set<NamespacedKey> disabledKeys;

    public EnchantmentRegistry() {
        this(Set.of());
    }

    public EnchantmentRegistry(Set<NamespacedKey> disabledKeys) {
        this.disabledKeys = Set.copyOf(disabledKeys == null ? Set.of() : disabledKeys);
    }

    public static EnchantmentRegistry defaultRegistry() {
        return defaultRegistry(Set.of());
    }

    public static EnchantmentRegistry defaultRegistry(Set<NamespacedKey> disabledKeys) {
        EnchantmentRegistry reg = new EnchantmentRegistry(disabledKeys);
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
        Set<Material> axes = Set.of(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
        );
        Set<Material> pickaxesAndShovels = Set.copyOf(List.of(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
        ));
        Set<Material> hoes = Set.of(
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
        );
        Set<Material> durableItems = Set.copyOf(List.of(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT
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
        Set<Material> allArmor = Set.of(
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
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

        registerCustom(reg, "sticky_grip", "Sticky Grip", 1, 5, 5, 10, weaponsAndTools, new StickyGripHandler());
        registerCustom(reg, "equilibrium", "Equilibrium", 5, 0, 5, 10, weapons, new EquilibriumHandler());
        registerCustom(reg, "nethers_scourge", "Nether's Scourge", 6, 0, 5, 10, weapons, new NetherScourgeHandler());
        registerCustom(reg, "cold_aspect", "Cold Aspect", 3, 5, 10, 10, weapons, new ColdAspectHandler());
        registerCustom(reg, "confusing_aspect", "Confusing Aspect", 3, 5, 10, 10, weapons, new ConfusingAspectHandler());
        registerCustom(reg, "toxin_aspect", "Toxin Aspect", 3, 5, 10, 10, weapons, new ToxinAspectHandler());
        registerCustom(reg, "knowledge", "Knowledge", 3, 10, 5, 10, weapons, new KnowledgeHandler());
        registerCustom(reg, "vorpal", "Vorpal", 3, 10, 5, 10, weapons, new VorpalHandler());
        registerCustom(reg, "vampirism", "Vampirism", 5, 5, 5, 10, weapons, new VampirismHandler());
        registerCustom(reg, "flurry", "Flurry", 3, 10, 5, 10, swords, new FlurryHandler());
        registerCustom(reg, "array", "Array", 2, 10, 10, 10, bows, new ArrayHandler());
        registerCustom(reg, "plunder", "Plunder", 3, 5, 5, 10, bows, new PlunderHandler());
        registerCustom(reg, "wisdom", "Wisdom", 3, 10, 5, 10, bows, new WisdomHandler());
        registerCustom(reg, "drill", "Drill", 3, 10, 5, 10, pickaxes, new DrillHandler());
        registerCustom(reg, "expertise", "Expertise", 3, 10, 5, 10, pickaxes);
        registerCustom(reg, "quenching", "Quenching", 4, 0, 5, 10, leggings, new QuenchingHandler());
        registerCustom(reg, "colorama", "Colorama", 1, 10, 5, 10, leatherArmor, new ColoramaHandler());
        registerCustom(reg, "leaping", "Leaping", 3, 5, 5, 10, Set.of(Material.SADDLE), new LeapingHandler());
        registerCustom(reg, "feather_hooves", "Feather Hooves", 1, 5, 5, 10, Set.of(Material.SADDLE), new FeatherHoovesHandler());
        registerCustom(reg, "molten_touch", "Molten Touch", 1, 15, 5, 10, customTools, new MoltenTouchHandler());
        registerCustom(reg, "prismatic", "Prismatic", 1, 15, 5, 10, Set.of(Material.SHEARS), new PrismaticHandler());
        registerCustom(reg, "overflowing", "Overflowing", 1, 10, 5, 10,
                Set.of(Material.BUCKET, Material.WATER_BUCKET), new OverflowingHandler());
        registerCustom(reg, "vacuum", "Vacuum", 1, 10, 5, 10,
                Set.of(Material.BUCKET, Material.WATER_BUCKET), new VacuumHandler());
        registerCustom(reg, "heat_wave", "Heat Wave", 1, 10, 5, 10, Set.of(Material.FLINT_AND_STEEL), new HeatWaveHandler());
        registerCustom(reg, "telepathy", "Telepathy", 1, 10, 5, 10, customTools, new TelepathyHandler());
        registerCustom(reg, "timber", "Timber", 3, 15, 10, 8, axes, new TimberHandler());
        registerCustom(reg, "trench", "Trench", 3, 15, 10, 8, pickaxesAndShovels, new TrenchHandler());
        registerCustom(reg, "replanter", "Replanter", 1, 10, 5, 10, hoes, new ReplanterHandler());
        registerCustom(reg, "planter", "Planter", 1, 10, 5, 8, hoes, new PlanterHandler());
        registerCustom(reg, "carrot_planter", "Carrot Planter", 1, 10, 5, 8, hoes, new CarrotPlanterHandler());
        registerCustom(reg, "potato_planter", "Potato Planter", 1, 10, 5, 8, hoes, new PotatoPlanterHandler());
        registerCustom(reg, "experience", "Experience", 3, 10, 5, 10, pickaxes, new ExperienceHandler());
        registerCustom(reg, "rebreather", "Rebreather", 3, 10, 5, 10, pickaxes, new RebreatherHandler());
        registerCustom(reg, "replenish", "Replenish", 3, 10, 5, 10, pickaxes, new ReplenishHandler());
        registerCustom(reg, "unbreakable", "Unbreakable", 1, 20, 20, 3, durableItems, new UnbreakableHandler());
        registerCustom(reg, "reforged", "Reforged", 5, 10, 5, 8, durableItems, new ReforgedHandler());
        registerCustom(reg, "aegis", "Aegis", 3, 10, 5, 10, allArmor, new AegisHandler());
        registerCustom(reg, "angelic", "Angelic", 3, 10, 5, 10, allArmor, new AngelicHandler());
        registerCustom(reg, "armored", "Armored", 3, 10, 5, 10, allArmor, new ArmoredHandler());
        registerCustom(reg, "chunky", "Chunky", 3, 10, 5, 10, allArmor, new ChunkyHandler());
        registerCustom(reg, "dodge", "Dodge", 3, 10, 5, 10, allArmor, new DodgeHandler());
        registerCustom(reg, "heavy", "Heavy", 3, 10, 5, 10, allArmor, new HeavyHandler());
        registerCustom(reg, "molten", "Molten", 3, 10, 5, 10, allArmor, new MoltenHandler());
        registerCustom(reg, "reflect", "Reflect", 3, 10, 5, 10, allArmor, new ReflectHandler());
        registerCustom(reg, "safeguard", "Safeguard", 3, 10, 5, 10, allArmor, new SafeguardHandler());
        registerCustom(reg, "tank", "Tank", 3, 10, 5, 10, allArmor, new TankHandler());
        return reg;
    }

    private static void registerCustom(EnchantmentRegistry registry, String key, String displayName,
                                       int absoluteMaxLevel, int baseEternaRequired, int eternaPerLevel,
                                       int weight, Set<Material> targetMaterials) {
        registerCustom(registry, key, displayName, absoluteMaxLevel, baseEternaRequired, eternaPerLevel,
                weight, targetMaterials, null);
    }

    private static void registerCustom(EnchantmentRegistry registry, String key, String displayName,
                                       int absoluteMaxLevel, int baseEternaRequired, int eternaPerLevel,
                                       int weight, Set<Material> targetMaterials, OvercapEffectHandler handler) {
        registry.register(new EnchantmentDefinition(
                new NamespacedKey("merlin", key), displayName, 0, absoluteMaxLevel,
                baseEternaRequired, eternaPerLevel, weight, targetMaterials, Optional.ofNullable(handler)));
    }

    public void register(EnchantmentDefinition def) {
        definitions.put(def.key(), def);
    }

    public Optional<EnchantmentDefinition> get(NamespacedKey key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public boolean isDisabled(NamespacedKey key) {
        return disabledKeys.contains(key);
    }

    public Set<NamespacedKey> disabledKeys() {
        return disabledKeys;
    }

    public List<EnchantmentDefinition> findForMaterial(Material mat) {
        return definitions.values().stream()
                .filter(d -> !isDisabled(d.key()))
                .filter(d -> d.canApplyTo(mat))
                .toList();
    }

    public List<EnchantmentDefinition> findEligible(Material mat, double eterna) {
        return definitions.values().stream()
                .filter(d -> !isDisabled(d.key()))
                .filter(d -> d.canApplyTo(mat))
                .filter(d -> d.minEternaForLevel(1) <= eterna)
                .toList();
    }
}
