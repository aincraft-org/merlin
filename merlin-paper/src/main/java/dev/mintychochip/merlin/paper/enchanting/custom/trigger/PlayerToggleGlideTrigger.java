package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;

public interface PlayerToggleGlideTrigger {
    void onToggleGlide(Player player, boolean isGliding, int level);
}
