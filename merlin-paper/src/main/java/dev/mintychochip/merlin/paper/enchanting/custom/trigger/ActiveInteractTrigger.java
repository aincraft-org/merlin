package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ActiveInteractTrigger {
    void onActiveInteract(Player player, ItemStack item, int level);
}
