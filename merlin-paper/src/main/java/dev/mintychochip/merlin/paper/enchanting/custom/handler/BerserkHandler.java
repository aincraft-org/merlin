package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class BerserkHandler implements OvercapEffectHandler, EntityHitTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("berserk");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level) {
        if (attacker == null || victim == null || damage == null || level <= 0) return;
        if (damage.isCancelled()) return;

        int ticks = 60 * level;
        int amplifier = Math.min(4, Math.max(0, level - 1));

        attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, amplifier));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, amplifier));
    }
}