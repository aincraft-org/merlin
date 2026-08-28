package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface EntityKillTrigger {
    void onEntityKill(Player killer, LivingEntity victim, int level);
}
