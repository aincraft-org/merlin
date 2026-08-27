package dev.mintychochip.merlin.paper.ink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class MortarPestleTest {
    @Test
    void recognizesMarkedBowlAsMortar() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");
        MortarPestle mortar = new MortarPestle(plugin);

        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.BOWL);
        when(item.hasItemMeta()).thenReturn(true);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(item.getItemMeta()).thenReturn(meta);
        when(pdc.has(new NamespacedKey("merlin", "mortar_pestle"), PersistentDataType.BYTE)).thenReturn(true);

        assertTrue(mortar.isMortar(item));
    }

    @Test
    void rejectsUnmarkedItemsAndNull() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");
        MortarPestle mortar = new MortarPestle(plugin);

        ItemStack bowl = mock(ItemStack.class);
        when(bowl.getType()).thenReturn(Material.BOWL);
        when(bowl.hasItemMeta()).thenReturn(true);
        ItemMeta bowlMeta = mock(ItemMeta.class);
        PersistentDataContainer bowlPdc = mock(PersistentDataContainer.class);
        when(bowlMeta.getPersistentDataContainer()).thenReturn(bowlPdc);
        when(bowl.getItemMeta()).thenReturn(bowlMeta);
        when(bowlPdc.has(new NamespacedKey("merlin", "mortar_pestle"), PersistentDataType.BYTE)).thenReturn(false);

        assertFalse(mortar.isMortar(bowl));

        ItemStack stick = mock(ItemStack.class);
        when(stick.getType()).thenReturn(Material.STICK);
        when(stick.hasItemMeta()).thenReturn(false);

        assertFalse(mortar.isMortar(stick));
        assertFalse(mortar.isMortar(null));
    }
}
