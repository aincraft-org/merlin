package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.NamespacedKey;

public record EnchantingOffer(
        int tier,
        int xpLevelCost,
        int xpLevelRequirement,
        int lapisCost,
        Map<NamespacedKey, Integer> enchantments,
        String previewHint
) {}
