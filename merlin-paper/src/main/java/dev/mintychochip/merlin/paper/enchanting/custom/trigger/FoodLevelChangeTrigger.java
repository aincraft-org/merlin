package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;

public interface FoodLevelChangeTrigger {
    int onFoodLevelChange(Player player, int currentFoodLevel, int proposedFoodLevel, int level);
}
