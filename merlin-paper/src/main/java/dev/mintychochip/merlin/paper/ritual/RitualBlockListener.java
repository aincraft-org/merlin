package dev.mintychochip.merlin.paper.ritual;

import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public final class RitualBlockListener implements Listener {
    private final RitualAnchor anchor;
    private final RitualPedestal pedestal;

    public RitualBlockListener(RitualAnchor anchor, RitualPedestal pedestal) {
        this.anchor = anchor;
        this.pedestal = pedestal;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.DROPPER) return;
        if (!(event.getBlock().getState() instanceof TileState state)) return;
        if (anchor.isAnchorItem(item)) {
            anchor.mark(state);
        } else if (pedestal.isPedestalItem(item)) {
            pedestal.mark(state);
        }
    }
}
