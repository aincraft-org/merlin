package dev.mintychochip.merlin.paper.listener;

import dev.mintychochip.merlin.paper.MerlinPlugin;
import dev.mintychochip.merlin.paper.dialog.ScribeDialog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ScribeBookListener implements Listener {
    private final MerlinPlugin plugin;
    private final ScribeDialog dialog;
    public ScribeBookListener(MerlinPlugin plugin, ScribeDialog dialog) { this.plugin = plugin; this.dialog = dialog; }
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (!plugin.books().isScribeBook(item)) return;
        event.setCancelled(true);
        dialog.show(event.getPlayer(), item, System.currentTimeMillis());
    }
}
