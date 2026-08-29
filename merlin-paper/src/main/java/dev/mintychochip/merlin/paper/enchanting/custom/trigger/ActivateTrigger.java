package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import java.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Triggered by an explicit right-click activation of an enchanted item. */
public interface ActivateTrigger {
    Duration activationCooldown();

    boolean onActivate(int level, Player player, ItemStack item);
}
