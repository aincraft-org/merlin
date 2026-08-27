package dev.mintychochip.merlin.paper.ink;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MortarPestle {
    private final NamespacedKey markerKey;
    private final NamespacedKey recipeKey;

    public MortarPestle(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "mortar_pestle");
        recipeKey = new NamespacedKey(plugin, "mortar_pestle");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.BOWL);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(
                DataComponentTypes.ITEM_NAME,
                Component.text("Mortar & Pestle").decoration(TextDecoration.ITALIC, false));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        return item;
    }

    public boolean isMortar(ItemStack item) {
        if (item == null || item.getType() != Material.BOWL || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, create());
        recipe.addIngredient(Material.BOWL);
        recipe.addIngredient(Material.STICK);
        Bukkit.addRecipe(recipe);
    }
}
