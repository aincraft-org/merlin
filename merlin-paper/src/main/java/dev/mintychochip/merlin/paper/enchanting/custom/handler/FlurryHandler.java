package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityInteractTrigger;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class FlurryHandler implements OvercapEffectHandler, EntityInteractTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("flurry");
    private static final double SEARCH_RADIUS = 4.0;
    private static final double KNOCKBACK = 1.0;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityInteract(Player player, Entity rightClicked, org.bukkit.inventory.ItemStack item,
                                 org.bukkit.inventory.EquipmentSlot hand, int level) {
        if (player == null || !(rightClicked instanceof LivingEntity) || level <= 0) return;

        Location origin = player.getLocation();
        List<Entity> nearby = player.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);
        if (origin == null || nearby == null) return;

        long requested = 3L * level;
        int limit = (int) Math.min(Integer.MAX_VALUE, requested);
        int affected = 0;
        for (Entity entity : nearby) {
            if (affected >= limit) break;
            if (!(entity instanceof LivingEntity living) || living == player) continue;

            Location target = living.getLocation();
            if (target == null) continue;
            Vector outward = target.toVector().subtract(origin.toVector());
            if (outward.lengthSquared() > 0.0) outward.normalize().multiply(KNOCKBACK);
            else outward.zero();
            living.setVelocity(outward);
            affected++;
        }
    }
}
