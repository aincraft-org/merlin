package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public interface BowShootTrigger {
    void onBowShoot(LivingEntity shooter, Entity projectile, ItemStack bow, float force, int level);
}
