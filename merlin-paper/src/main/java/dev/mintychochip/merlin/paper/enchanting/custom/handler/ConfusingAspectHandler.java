package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ConfusingAspectHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("confusing_aspect");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (attacker == null || victim == null || damage == null || level <= 0) return;

        int requestedTicks = 100 * level;
        PotionEffect existing = victim.getPotionEffect(PotionEffectType.NAUSEA);
        if (existing != null
                && (existing.getDuration() == PotionEffect.INFINITE_DURATION
                        || existing.getDuration() >= requestedTicks)) return;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, requestedTicks, 0));
    }
}
