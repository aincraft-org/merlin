package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.PlayerDropItemTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class StickyGripHandler implements OvercapEffectHandler, PlayerDropItemTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("sticky_grip");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public boolean shouldCancelDrop(Player player, ItemStack item, int level) {
        return player != null && item != null && !item.isEmpty() && level > 0;
    }
}
