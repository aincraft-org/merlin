package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;

public interface PlayerToggleSprintTrigger {
    void onToggleSprint(Player player, boolean isSprinting, int level);
}
