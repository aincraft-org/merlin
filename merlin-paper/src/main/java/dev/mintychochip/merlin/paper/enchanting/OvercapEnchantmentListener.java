package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class OvercapEnchantmentListener implements Listener {
    private final OvercapItemAdapter adapter;
    private final EnchantmentRegistry registry;

    public OvercapEnchantmentListener(OvercapItemAdapter adapter, EnchantmentRegistry registry) {
        this.adapter = adapter;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Map<NamespacedKey, Integer> overcap = adapter.readOvercap(weapon);
        for (var entry : overcap.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> handler.onDamageDealt(event, entry.getValue()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Map<NamespacedKey, Integer> overcap = adapter.readOvercap(tool);
        for (var entry : overcap.entrySet()) {
            registry.get(entry.getKey()).flatMap(EnchantmentDefinition::overcapHandler)
                    .ifPresent(handler -> handler.onBlockBreak(event, entry.getValue()));
        }
    }
}
