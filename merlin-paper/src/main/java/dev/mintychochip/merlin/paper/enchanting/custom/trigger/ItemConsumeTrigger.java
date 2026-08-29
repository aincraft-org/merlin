package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemConsumeTrigger {
    void onItemConsume(Player player, ItemStack consumedItem, int level);
}
