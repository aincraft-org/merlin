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
        Set<Material> armor = Set.of(
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );

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
        return reg;
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
