package dev.mintychochip.merlin.paper.enchanting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class OvercapItemAdapter {
    private final NamespacedKey overcapContainerKey;
    private final EnchantmentRegistry registry;

    public OvercapItemAdapter(Plugin plugin, EnchantmentRegistry registry) {
        this.overcapContainerKey = new NamespacedKey(plugin, "overcap_enchantments");
        this.registry = registry;
    }

    public Map<NamespacedKey, Integer> readOvercap(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Map.of();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer sub = root.get(overcapContainerKey, PersistentDataType.TAG_CONTAINER);
        if (sub == null) return Map.of();

        Map<NamespacedKey, Integer> result = new HashMap<>();
        for (NamespacedKey key : sub.getKeys()) {
            Integer lvl = sub.get(key, PersistentDataType.INTEGER);
            if (lvl != null) {
                result.put(key, lvl);
            }
        }
        return result;
    }

    public boolean applyEnchantments(ItemStack item, Map<NamespacedKey, Integer> enchants) {
        if (item == null || item.isEmpty() || enchants == null || enchants.isEmpty()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer sub = root.get(overcapContainerKey, PersistentDataType.TAG_CONTAINER);
        if (sub == null) {
            sub = root.getAdapterContext().newPersistentDataContainer();
        }

        List<Component> overcapLore = new ArrayList<>();
        for (var entry : enchants.entrySet()) {
            NamespacedKey key = entry.getKey();
            int level = entry.getValue();
            EnchantmentDefinition def = registry.get(key).orElse(null);
            Enchantment vanilla = def != null && def.vanillaMaxLevel() == 0
                    ? null : Enchantment.getByKey(key);
            boolean registeredCustom = def != null && vanilla == null;
            sub.remove(key);
            int vanillaMax = def != null ? def.vanillaMaxLevel() : (vanilla != null ? vanilla.getMaxLevel() : 1);

            if (registeredCustom && level > 0) {
                sub.set(key, PersistentDataType.INTEGER, level);
                overcapLore.add(Component.text(def.displayName() + " " + QuantaRollEngine.toRoman(level),
                        NamedTextColor.GRAY));
            } else if (!registeredCustom && level > vanillaMax) {
                sub.set(key, PersistentDataType.INTEGER, level);
                if (vanilla != null) {
                    meta.addEnchant(vanilla, vanillaMax, true);
                }
                String name = def != null ? def.displayName() : key.getKey();
                overcapLore.add(Component.text(name + " " + QuantaRollEngine.toRoman(level), NamedTextColor.GRAY));
            } else if (vanilla != null) {
                meta.addEnchant(vanilla, level, true);
            }
        }

        if (!sub.getKeys().isEmpty()) {
            root.set(overcapContainerKey, PersistentDataType.TAG_CONTAINER, sub);
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.addAll(overcapLore);
            meta.lore(lore);
        }
        return item.setItemMeta(meta);
    }
}
