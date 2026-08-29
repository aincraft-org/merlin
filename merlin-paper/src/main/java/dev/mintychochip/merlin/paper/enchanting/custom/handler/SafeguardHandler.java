package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ArmorDefenseTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SafeguardHandler implements OvercapEffectHandler, ArmorDefenseTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("safeguard");
    private static final int RESISTANCE_TICKS_PER_LEVEL = 60;
    private static final int MAX_AMPLIFIER = 4;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level) {
        if (defender == null || damage == null || damage.isCancelled() || level <= 0
                || damage.getFinalDamage() <= 0.0) return;

        int duration = (int) Math.min(Integer.MAX_VALUE, (long) RESISTANCE_TICKS_PER_LEVEL * level);
        int amplifier = Math.min(MAX_AMPLIFIER, level - 1);
        defender.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, amplifier));
    }
}
