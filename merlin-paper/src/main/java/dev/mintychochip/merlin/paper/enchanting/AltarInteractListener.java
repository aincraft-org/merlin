package dev.mintychochip.merlin.paper.enchanting;

import dev.mintychochip.merlin.paper.enchanting.gui.AltarGuiSession;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class AltarInteractListener implements Listener {
    private final AltarScanner scanner;
    private final EnchantmentRegistry registry;
    private final QuantaRollEngine rollEngine;
    private final OvercapItemAdapter itemAdapter;

    public AltarInteractListener(AltarScanner scanner, EnchantmentRegistry registry,
                                 QuantaRollEngine rollEngine, OvercapItemAdapter itemAdapter) {
        this.scanner = scanner;
        this.registry = registry;
        this.rollEngine = rollEngine;
        this.itemAdapter = itemAdapter;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTableClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.ENCHANTING_TABLE) return;

        event.setCancelled(true);
        AltarProfile profile = scanner.scan(event.getClickedBlock().getLocation());
        AltarGuiSession session = new AltarGuiSession(
                event.getPlayer(), event.getClickedBlock().getLocation(), profile, registry, rollEngine, itemAdapter);
        session.open();
    }
}
