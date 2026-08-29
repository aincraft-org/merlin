package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface PlayerJumpTrigger {
    void onPlayerJump(Player player, Location from, Location to, int level);
}
