package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

public final class CriticalHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("critical");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
		if (attacker == null || victim == null || damage == null || level <= 0) return;
        if (damage.isCancelled()) return;
        if (attacker.isOnGround()) return;

        damage.multiply(1.0 + 0.10 * level);
    }
}