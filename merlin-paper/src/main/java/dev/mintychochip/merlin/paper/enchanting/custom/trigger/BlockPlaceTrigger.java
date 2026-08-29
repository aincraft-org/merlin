package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface BlockPlaceTrigger {
    void onBlockPlace(Player player, Block placedBlock, Block placedAgainst, ItemStack itemInHand, int level);
}
