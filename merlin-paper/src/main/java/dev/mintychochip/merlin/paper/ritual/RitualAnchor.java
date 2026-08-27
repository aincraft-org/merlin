package dev.mintychochip.merlin.paper.ritual;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class RitualAnchor {
    private final NamespacedKey markerKey;
    private final NamespacedKey recipeKey;

    public RitualAnchor(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "ritual_anchor");
        recipeKey = new NamespacedKey(plugin, "ritual_anchor");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DROPPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Ritual Anchor").decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    public boolean isAnchor(TileState state) {
        return state != null && state.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public boolean isAnchorItem(ItemStack item) {
        if (item == null || item.getType() != Material.DROPPER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void mark(TileState state) {
        state.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        state.update();
    }

    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
        recipe.addIngredient(Material.OBSIDIAN);
        recipe.addIngredient(Material.DROPPER);
        Bukkit.addRecipe(recipe);
    }
}
