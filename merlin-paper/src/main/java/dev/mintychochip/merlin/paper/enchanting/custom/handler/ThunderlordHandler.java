package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public final class ThunderlordHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("thunderlord");
    private static final int REQUIRED_HITS = 3;

    private final Map<UUID, Integer> hitCounters = new ConcurrentHashMap<>();

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (attacker == null || victim == null || damage == null || level <= 0) return;
        if (damage.isCancelled()) return;

        int hits = hitCounters.merge(victim.getUniqueId(), 1, Integer::sum);
        if (hits < REQUIRED_HITS) return;

        hitCounters.remove(victim.getUniqueId());
        World world = victim.getWorld();
        if (world == null) return;
        Location location = victim.getLocation();
        if (location == null) return;
        world.strikeLightning(location);
    }

    void clearCounter(UUID victimId) {
        if (victimId != null) hitCounters.remove(victimId);
    }
}