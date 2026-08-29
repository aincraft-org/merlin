package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public interface ActiveInteractTrigger {
    void onActiveInteract(Player player, Action action, Block clickedBlock, ItemStack item, int level);
}
