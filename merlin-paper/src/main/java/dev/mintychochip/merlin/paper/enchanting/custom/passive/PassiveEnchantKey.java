package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Maps the passive enchant keys to their effect semantics. */
public enum PassiveEnchantKey {
    GEARS("gears"),
    SPRINGS("springs"),
    AQUATIC("aquatic"),
    GLOWING("glowing"),
    IMPLANTS("implants"),
    OBSIDIANSHIELD("obsidianshield"),
    OVERLOAD("overload"),
    WINGS("wings");

    private final String key;

    PassiveEnchantKey(String key) {
        this.key = key;
    }

    public NamespacedKey namespacedKey() {
        return new NamespacedKey("merlin", key);
    }

    public static Map<PassiveEnchantKey, Integer> passiveEffectsFor(ItemStack item) {
        Map<PassiveEnchantKey, Integer> result = new java.util.HashMap<>();
        if (item == null) return result;
        // Levels are read via the PDC overcap adapter; fall back to a static read of merlin keys.
        for (PassiveEnchantKey enchant : values()) {
            int level = readLevel(item, enchant);
            if (level > 0) result.put(enchant, level);
        }
        return result;
    }

    private static int readLevel(ItemStack item, PassiveEnchantKey enchant) {
        // Direct PDC read using the same compound key as OvercapItemAdapter.
        var container = item.getItemMeta() == null
                ? null
                : item.getItemMeta().getPersistentDataContainer();
        if (container == null) return 0;
        var tag = container.get(
                new NamespacedKey("merlin", "overcap_enchantments"),
                org.bukkit.persistence.PersistentDataType.TAG_CONTAINER);
        if (tag == null) return 0;
        Integer level = tag.get(enchant.namespacedKey(), org.bukkit.persistence.PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }
}