package dev.mintychochip.merlin.paper.ritual;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class RitualProducts {
    private final NamespacedKey markerKey;

    public RitualProducts(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "ritual_product");
    }

    public List<ItemStack> create(RitualRecipe recipe, int amount) {
        int yield = Math.max(1, Math.min(amount, recipe.maxYield()));
        ItemStack item = new ItemStack(recipe.output(), yield);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text(displayName(recipe)).decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return List.of(item);
    }

    private static String displayName(RitualRecipe recipe) {
        return switch (recipe.word().id()) {
            case "damage" -> "Aether Ingot";
            case "heal" -> "Sun Drop";
            case "push" -> "Kinetic Coil";
            case "flame" -> "Cinder Salt";
            case "frost" -> "Rime Crystal";
            default -> recipe.word().id() + " Essence";
        };
    }
}
