package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;

public interface ExpGainTrigger {
    int onExpGain(Player player, int originalExpAmount, int level);
}
