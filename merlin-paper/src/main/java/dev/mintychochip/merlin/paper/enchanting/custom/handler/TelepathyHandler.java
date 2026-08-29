package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockDropTrigger;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TelepathyHandler implements OvercapEffectHandler, BlockDropTrigger {
    private static final NamespacedKey KEY = new NamespacedKey("merlin", "telepathy");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockDrop(Player player, BlockState blockState, List<Item> items, int level) {
        if (player == null || items == null || items.isEmpty() || level <= 0) return;

        for (Item item : items) {
            if (item == null) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) continue;
            player.getInventory().addItem(stack);
            item.remove();
        }
        items.clear();
    }
}
