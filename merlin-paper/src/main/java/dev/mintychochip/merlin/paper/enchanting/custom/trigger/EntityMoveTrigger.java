package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface EntityMoveTrigger {
    void onEntityMove(LivingEntity entity, Location from, Location to, int level);
}
