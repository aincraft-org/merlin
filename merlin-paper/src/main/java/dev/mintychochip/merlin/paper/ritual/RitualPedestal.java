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

public final class RitualPedestal {
    private final NamespacedKey markerKey;
    private final NamespacedKey recipeKey;

    public RitualPedestal(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "ritual_pedestal");
        recipeKey = new NamespacedKey(plugin, "ritual_pedestal");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DROPPER);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Ritual Pedestal").decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    public boolean isPedestal(TileState state) {
        return state != null && state.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public boolean isPedestalItem(ItemStack item) {
        if (item == null || item.getType() != Material.DROPPER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void mark(TileState state) {
        state.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        state.update();
    }

    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
        recipe.addIngredient(Material.COBBLESTONE);
        recipe.addIngredient(Material.DROPPER);
        Bukkit.addRecipe(recipe);
    }
}
