package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    void dispatchesRerollClickOnRerollSlot() {
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
        verify(session).handleRerollClick();
    }

    @Test
    void cleansUpSessionOnPlayerQuit() {
        AltarGuiListener listener = new AltarGuiListener();
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        AltarGuiSession session = mock(AltarGuiSession.class);
        when(session.getPlayer()).thenReturn(player);

        listener.registerSession(session);
        assertEquals(1, listener.getActiveSessions().size());

        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);
        when(quitEvent.getPlayer()).thenReturn(player);

        listener.onQuit(quitEvent);
        verify(session).handleClose();
        assertEquals(0, listener.getActiveSessions().size());
    }

    @Test
    void cleansUpSessionOnPlayerDeath() {
        AltarGuiListener listener = new AltarGuiListener();
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        AltarGuiSession session = mock(AltarGuiSession.class);
        when(session.getPlayer()).thenReturn(player);

        listener.registerSession(session);
        assertEquals(1, listener.getActiveSessions().size());

        PlayerDeathEvent deathEvent = mock(PlayerDeathEvent.class);
        when(deathEvent.getEntity()).thenReturn(player);

        listener.onDeath(deathEvent);
        verify(session).handleClose();
        assertEquals(0, listener.getActiveSessions().size());
    }

    @Test
    void closeAllSessionsReclaimsEveryActiveSession() {
        AltarGuiListener listener = new AltarGuiListener();
        Player player1 = mock(Player.class);
        when(player1.getUniqueId()).thenReturn(UUID.randomUUID());
        AltarGuiSession session1 = mock(AltarGuiSession.class);
        when(session1.getPlayer()).thenReturn(player1);

        Player player2 = mock(Player.class);
        when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        AltarGuiSession session2 = mock(AltarGuiSession.class);
        when(session2.getPlayer()).thenReturn(player2);

        listener.registerSession(session1);
        listener.registerSession(session2);
        assertEquals(2, listener.getActiveSessions().size());

        listener.closeAllSessions();
        verify(session1).handleClose();
        verify(session2).handleClose();
        assertEquals(0, listener.getActiveSessions().size());
    }
}
