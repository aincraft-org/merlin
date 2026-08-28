package dev.mintychochip.merlin.paper.enchanting.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class AltarGuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof AltarInventoryHolder holder)) return;
        AltarGuiSession session = holder.session();

        int slot = event.getRawSlot();
        if (slot < 54) {
            // Click inside GUI
            if (slot == AltarGuiSession.SLOT_TARGET || slot == AltarGuiSession.SLOT_LAPIS || slot == AltarGuiSession.SLOT_CATALYST) {
                // Allowed input slots
                return;
            }
            event.setCancelled(true);
            if (slot == AltarGuiSession.SLOT_TIER_1) session.handleEnchantClick(1);
            else if (slot == AltarGuiSession.SLOT_TIER_2) session.handleEnchantClick(2);
            else if (slot == AltarGuiSession.SLOT_TIER_3) session.handleEnchantClick(3);
            else if (slot == AltarGuiSession.SLOT_REROLL) session.rerollOffers();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof AltarInventoryHolder)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && slot != AltarGuiSession.SLOT_TARGET && slot != AltarGuiSession.SLOT_LAPIS && slot != AltarGuiSession.SLOT_CATALYST) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AltarInventoryHolder holder) {
            holder.session().handleClose();
        }
    }
}
