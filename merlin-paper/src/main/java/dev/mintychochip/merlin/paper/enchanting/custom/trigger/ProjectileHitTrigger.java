package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

public interface ProjectileHitTrigger {
    void onProjectileHit(LivingEntity shooter, Projectile projectile, Entity hitEntity, Block hitBlock, int level);
}
