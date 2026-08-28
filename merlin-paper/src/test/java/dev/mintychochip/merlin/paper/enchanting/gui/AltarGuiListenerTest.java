package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class AltarGuiListenerTest {
    @Test
    void cancelsDragIntoNonInputSlots() {
        AltarGuiListener listener = new AltarGuiListener();
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        Inventory inv = mock(Inventory.class);
        AltarInventoryHolder holder = mock(AltarInventoryHolder.class);
        when(event.getInventory()).thenReturn(inv);
        when(inv.getHolder()).thenReturn(holder);

        ItemStack dragged = mock(ItemStack.class);
        when(dragged.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(dragged.isEmpty()).thenReturn(false);
        when(event.getOldCursor()).thenReturn(dragged);
        when(event.getRawSlots()).thenReturn(Set.of(0, 1, 2)); // Meter and filler slots

        listener.onDrag(event);
        verify(event).setCancelled(true);
    }

    @Test
    void cancelsDragOfInvalidMaterialIntoLapisSlot() {
        AltarGuiListener listener = new AltarGuiListener();
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        Inventory inv = mock(Inventory.class);
        AltarInventoryHolder holder = mock(AltarInventoryHolder.class);
        when(event.getInventory()).thenReturn(inv);
        when(inv.getHolder()).thenReturn(holder);

        ItemStack dragged = mock(ItemStack.class);
        when(dragged.getType()).thenReturn(Material.DIRT);
        when(dragged.isEmpty()).thenReturn(false);
        when(event.getOldCursor()).thenReturn(dragged);
        when(event.getRawSlots()).thenReturn(Set.of(AltarGuiSession.SLOT_LAPIS));

        listener.onDrag(event);
        verify(event).setCancelled(true);
    }

    @Test
    void dispatchesEnchantActionOnTierClick() {
        AltarGuiListener listener = new AltarGuiListener();
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Inventory inv = mock(Inventory.class);
        AltarInventoryHolder holder = mock(AltarInventoryHolder.class);
        AltarGuiSession session = mock(AltarGuiSession.class);

        when(event.getInventory()).thenReturn(inv);
        when(inv.getHolder()).thenReturn(holder);
        when(holder.session()).thenReturn(session);
        when(event.getRawSlot()).thenReturn(AltarGuiSession.SLOT_TIER_1);

        listener.onClick(event);
        verify(event).setCancelled(true);
        verify(session).handleEnchantClick(1);
    }

    @Test
    void dispatchesRerollOnRerollClick() {
        AltarGuiListener listener = new AltarGuiListener();
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Inventory inv = mock(Inventory.class);
        AltarInventoryHolder holder = mock(AltarInventoryHolder.class);
        AltarGuiSession session = mock(AltarGuiSession.class);

        when(event.getInventory()).thenReturn(inv);
        when(inv.getHolder()).thenReturn(holder);
        when(holder.session()).thenReturn(session);
        when(event.getRawSlot()).thenReturn(AltarGuiSession.SLOT_REROLL);

        listener.onClick(event);
        verify(event).setCancelled(true);
        verify(session).rerollOffers();
    }
}
