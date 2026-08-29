package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockDropTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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

        List<ItemStack> stacks = new ArrayList<>(items.size());
        for (Item item : items) {
            if (item == null) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) continue;
            stacks.add(stack);
        }
        if (stacks.isEmpty()) return;

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(new ItemStack[0]));

        for (Item item : items) {
            item.remove();
        }
        items.clear();

        if (!leftover.isEmpty() && blockState != null) {
            Location location = blockState.getLocation();
            World world = location.getWorld();
            if (world == null) world = player.getWorld();
            if (world != null) {
                for (ItemStack stack : leftover.values()) {
                    world.dropItemNaturally(location, stack);
                }
            }
        }
    }
}
