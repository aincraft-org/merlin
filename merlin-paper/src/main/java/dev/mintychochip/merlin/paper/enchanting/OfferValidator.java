package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class OfferValidator {
    private static final Set<Material> VALID_CATALYSTS = Set.of(
            Material.AMETHYST_SHARD,
            Material.ECHO_SHARD,
            Material.GLOWSTONE_DUST
    );

    // Common vanilla enchantment conflict groups
    private static final Set<Set<String>> CONFLICT_GROUPS = Set.of(
            Set.of("sharpness", "smite", "bane_of_arthropods"),
            Set.of("protection", "fire_protection", "blast_protection", "projectile_protection"),
            Set.of("fortune", "silk_touch"),
            Set.of("infinity", "mending"),
            Set.of("loyalty", "riptide")
    );

    public sealed interface Result {
        record Valid(Map<NamespacedKey, Integer> enchantsToApply, int lapisCost, int xpCost) implements Result {}
        record Invalid(String reason) implements Result {}
    }

    public static Result validate(
            boolean isClosed,
            Player player,
            ItemStack target,
            ItemStack lapis,
            ItemStack catalyst,
            EnchantingOffer offer,
            EnchantmentRegistry registry,
            OvercapItemAdapter adapter
    ) {
        if (isClosed) {
            return new Result.Invalid("Session is closed.");
        }
        if (player == null) {
            return new Result.Invalid("Player cannot be null.");
        }
        if (offer == null || offer.enchantments().isEmpty()) {
            return new Result.Invalid("No offer selected.");
        }
        if (target == null || target.isEmpty()) {
            return new Result.Invalid("Target item is missing.");
        }
        Material targetMat = target.getType();

        // Validate catalyst slot if populated
        if (catalyst != null && !catalyst.isEmpty()) {
            if (!VALID_CATALYSTS.contains(catalyst.getType())) {
                return new Result.Invalid("Unrecognized catalyst item.");
            }
        }

        // Validate lapis
        if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < offer.lapisCost()) {
            return new Result.Invalid("Insufficient Lapis Lazuli (requires " + offer.lapisCost() + ").");
        }

        // Validate player XP level
        if (player.getLevel() < offer.xpLevelRequirement()) {
            return new Result.Invalid("Insufficient XP Level (requires Level " + offer.xpLevelRequirement() + ").");
        }

        // Gather existing enchantments
        Map<NamespacedKey, Integer> existing = new HashMap<>();
        if (target.hasItemMeta()) {
            for (var entry : target.getEnchantments().entrySet()) {
                Enchantment enchant = entry.getKey();
                if (enchant != null) {
                    existing.put(enchant.getKey(), entry.getValue());
                }
            }
        }
        if (adapter != null) {
            Map<NamespacedKey, Integer> overcap = adapter.readOvercap(target);
            for (var entry : overcap.entrySet()) {
                existing.put(entry.getKey(), Math.max(existing.getOrDefault(entry.getKey(), 0), entry.getValue()));
            }
        }

        Map<NamespacedKey, Integer> toApply = new HashMap<>();
        boolean hasUpgrade = false;

        for (var entry : offer.enchantments().entrySet()) {
            NamespacedKey key = entry.getKey();
            int offeredLevel = entry.getValue();

            var defOpt = registry.get(key);
            if (defOpt.isEmpty()) {
                return new Result.Invalid("Unknown enchantment key: " + key);
            }
            EnchantmentDefinition def = defOpt.get();
            if (!def.canApplyTo(targetMat)) {
                return new Result.Invalid("Enchantment " + def.displayName() + " cannot be applied to " + targetMat);
            }
            if (offeredLevel < 1 || offeredLevel > def.absoluteMaxLevel()) {
                return new Result.Invalid("Offered level " + offeredLevel + " is outside permitted bounds for " + def.displayName());
            }

            // Conflict checking
            String keyName = key.getKey();
            for (Set<String> conflictGroup : CONFLICT_GROUPS) {
                if (conflictGroup.contains(keyName)) {
                    for (String other : conflictGroup) {
                        if (!other.equals(keyName)) {
                            NamespacedKey otherKey = NamespacedKey.minecraft(other);
                            if (existing.containsKey(otherKey) || toApply.containsKey(otherKey)) {
                                return new Result.Invalid("Conflicting enchantment already present: " + other);
                            }
                        }
                    }
                }
            }

            int existingLevel = existing.getOrDefault(key, 0);
            if (offeredLevel > existingLevel) {
                hasUpgrade = true;
                toApply.put(key, offeredLevel);
            }
        }

        if (!hasUpgrade) {
            return new Result.Invalid("Item already possesses equal or greater enchantment levels.");
        }

        return new Result.Valid(toApply, offer.lapisCost(), offer.xpLevelCost());
    }

    private OfferValidator() {}
}
