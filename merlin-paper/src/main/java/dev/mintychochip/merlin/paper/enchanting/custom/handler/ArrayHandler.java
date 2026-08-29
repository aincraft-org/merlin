package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class ArrayHandler implements OvercapEffectHandler, BowShootTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("array");
    private static final double MAX_SPREAD = 0.05;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBowShoot(LivingEntity shooter, Entity projectile, ItemStack bow, float force, int level) {
        if (shooter == null || projectile == null || level <= 0 || level > 2) return;

        World world = projectile.getWorld();
        Location location = projectile.getLocation();
        Vector originalVelocity = projectile.getVelocity();
        if (world == null || location == null || originalVelocity == null || projectile.getType() == null) return;

        int additional = 2 * level;
        for (int i = 0; i < additional; i++) {
            Entity spawned = world.spawnEntity(location, projectile.getType());
            if (spawned == null) continue;

            Vector velocity = originalVelocity.clone().add(new Vector(
                    spread(), spread(), spread()));
            spawned.setVelocity(velocity);
            if (spawned instanceof Projectile extra) extra.setShooter(shooter);
        }
    }

    private static double spread() {
        return ThreadLocalRandom.current().nextDouble(-MAX_SPREAD, MAX_SPREAD);
    }
}
