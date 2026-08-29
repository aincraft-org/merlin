package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BlindHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("blind");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (attacker == null || victim == null || damage == null || level <= 0) return;
        if (damage.isCancelled()) return;

        int ticks = 100 * level;
        int amplifier = Math.max(0, level - 1);

        PotionEffect existing = victim.getPotionEffect(PotionEffectType.BLINDNESS);
        if (existing != null && existing.getDuration() == PotionEffect.INFINITE_DURATION) return;
        if (existing != null && existing.getDuration() >= ticks && existing.getAmplifier() >= amplifier) return;

        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, amplifier));
    }
}