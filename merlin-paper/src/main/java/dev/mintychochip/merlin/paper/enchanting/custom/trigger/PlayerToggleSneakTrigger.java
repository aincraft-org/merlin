package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;

public interface PlayerToggleSneakTrigger {
    void onToggleSneak(Player player, boolean isSneaking, int level);
}
