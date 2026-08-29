package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ProjectileHitTrigger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

public final class SniperHandler implements OvercapEffectHandler, ProjectileHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("sniper");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onProjectileHit(
            LivingEntity shooter, Projectile projectile, Entity hitEntity, Block hitBlock, int level) {
        if (shooter == null || projectile == null || hitEntity == null || level <= 0) return;
        if (!(hitEntity instanceof LivingEntity victim)) return;
        if (victim.isDead()) return;

        Location impact = projectile.getLocation();
        Location eye = victim.getEyeLocation();
        if (impact == null || eye == null) return;
        if (impact.getY() < eye.getY() - 0.5) return;

        double damage = projectile instanceof Arrow arrow ? arrow.getDamage() : 2.0;
        victim.damage(damage * level, shooter);
    }
}