package dev.mintychochip.merlin.paper.enchanting;

import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public record EnchantmentDefinition(
        NamespacedKey key,
        String displayName,
        int vanillaMaxLevel,
        int absoluteMaxLevel,
        int baseEternaRequired,
        int eternaPerLevel,
        int weight,
        Set<Material> targetMaterials,
        Optional<OvercapEffectHandler> overcapHandler
) {
    public int minEternaForLevel(int level) {
        return baseEternaRequired + (level - 1) * eternaPerLevel;
    }

    public boolean canApplyTo(Material material) {
        return targetMaterials.contains(material);
    }
}
