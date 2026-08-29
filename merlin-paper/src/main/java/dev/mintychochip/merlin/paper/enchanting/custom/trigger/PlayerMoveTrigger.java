package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface PlayerMoveTrigger {
    void onPlayerMove(Player player, Location from, Location to, int level);
}
