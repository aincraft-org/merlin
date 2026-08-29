package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;

public interface PlayerFishTrigger {
    void onPlayerFish(Player player, FishHook hook, Entity caught, State state, int level);
}
