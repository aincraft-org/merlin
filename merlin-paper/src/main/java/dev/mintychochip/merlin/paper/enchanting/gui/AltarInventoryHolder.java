package dev.mintychochip.merlin.paper.enchanting.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AltarInventoryHolder implements InventoryHolder {
    private final AltarGuiSession session;
    private Inventory inventory;

    public AltarInventoryHolder(AltarGuiSession session) {
        this.session = session;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public AltarGuiSession session() {
        return session;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
