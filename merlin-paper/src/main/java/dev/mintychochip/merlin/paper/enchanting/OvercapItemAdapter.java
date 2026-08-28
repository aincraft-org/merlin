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

    public void applyEnchantments(ItemStack item, Map<NamespacedKey, Integer> enchants) {
        if (item == null || item.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer sub = root.getAdapterContext().newPersistentDataContainer();

        List<Component> overcapLore = new ArrayList<>();
        for (var entry : enchants.entrySet()) {
            NamespacedKey key = entry.getKey();
            int level = entry.getValue();
            Enchantment vanilla = Enchantment.getByKey(key);
            EnchantmentDefinition def = registry.get(key).orElse(null);
            int vanillaMax = def != null ? def.vanillaMaxLevel() : (vanilla != null ? vanilla.getMaxLevel() : 1);

            if (level > vanillaMax) {
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
        item.setItemMeta(meta);
    }
}
