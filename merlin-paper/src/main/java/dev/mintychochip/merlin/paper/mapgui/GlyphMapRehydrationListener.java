package dev.mintychochip.merlin.paper.mapgui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class GlyphMapRehydrationListener implements Listener {
    private final GlyphDraftStoreAdapter store;

    public GlyphMapRehydrationListener(GlyphDraftStoreAdapter store) {
        this.store = java.util.Objects.requireNonNull(store);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var inventory = event.getPlayer().getInventory();
        int slot = inventory.getHeldItemSlot();
        store.restoreRenderer(inventory.getItem(slot));
        store.restoreRenderer(inventory.getItemInOffHand());
        for (var item : inventory.getStorageContents()) store.restoreRenderer(item);
    }

    @EventHandler
    public void onHeldItemChanged(PlayerItemHeldEvent event) {
        store.restoreRenderer(event.getPlayer().getInventory().getItem(event.getNewSlot()));
    }

    static int[] slotsToRestore(int currentSlot, int newSlot) {
        return new int[] {newSlot >= 0 ? newSlot : currentSlot};
    }
}
