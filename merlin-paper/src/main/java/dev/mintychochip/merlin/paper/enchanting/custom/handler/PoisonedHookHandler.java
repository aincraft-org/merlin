package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.HookContactTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PoisonedHookHandler implements OvercapEffectHandler, HookContactTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("poisoned_hook");
    private static final int POISON_TICKS = 80;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onHookContact(Player player, LivingEntity hit, int level) {
        if (player == null || hit == null || level <= 0) return;
        if (hit.isDead()) return;

        int ticks = POISON_TICKS * level;
        int amplifier = Math.max(0, level - 1);
        PotionEffect existing = hit.getPotionEffect(PotionEffectType.POISON);
        if (existing != null && existing.getDuration() == PotionEffect.INFINITE_DURATION) return;
        if (existing != null && existing.getDuration() >= ticks && existing.getAmplifier() >= amplifier) return;

        hit.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, amplifier));
    }
}