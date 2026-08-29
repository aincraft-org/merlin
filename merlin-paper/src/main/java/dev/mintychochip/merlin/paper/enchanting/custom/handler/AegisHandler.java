package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EnvironmentalDamageTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AegisHandler implements OvercapEffectHandler, EnvironmentalDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("aegis");
    private static final int TICKS_PER_LEVEL = 100;
    private static final int MAX_AMPLIFIER = 4;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, int level) {
        if (player == null || cause != DamageCause.FALL || damage == null || damage.isCancelled()
                || damage.getFinalDamage() <= 0.0 || level <= 0) return;

        int duration = (int) Math.min(Integer.MAX_VALUE, (long) TICKS_PER_LEVEL * level);
        int amplifier = Math.min(MAX_AMPLIFIER, level - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier));
    }
}
