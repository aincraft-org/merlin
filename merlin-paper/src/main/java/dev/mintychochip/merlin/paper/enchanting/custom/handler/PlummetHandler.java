package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EnvironmentalDamageTrigger;
import java.util.Collection;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class PlummetHandler implements OvercapEffectHandler, EnvironmentalDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("plummet");
    private static final double RADIUS = 3.0;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, int level) {
        if (player == null || damage == null || cause != DamageCause.FALL || level <= 0) return;
        if (damage.isCancelled()) return;
        if (player.isDead()) return;

        Collection<Entity> nearby = player.getNearbyEntities(RADIUS, RADIUS, RADIUS);
        double aoe = 2.0 * level;
        for (Entity entity : nearby) {
            if (entity == player || !(entity instanceof LivingEntity living)) continue;
            if (living.isDead()) continue;
            living.damage(aoe, player);
        }
    }
}