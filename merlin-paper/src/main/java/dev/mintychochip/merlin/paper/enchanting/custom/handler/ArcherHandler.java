package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class ArcherHandler implements OvercapEffectHandler, BowShootTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("archer");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBowShoot(LivingEntity shooter, Entity projectile, ItemStack bow, float force, int level) {
        if (shooter == null || projectile == null || bow == null || level <= 0) return;
        if (!(projectile instanceof Projectile proj)) return;

        Vector velocity = proj.getVelocity();
        if (velocity == null) return;
        double multiplier = 1.0 + 0.10 * level;
        proj.setVelocity(velocity.multiply(multiplier));
    }
}