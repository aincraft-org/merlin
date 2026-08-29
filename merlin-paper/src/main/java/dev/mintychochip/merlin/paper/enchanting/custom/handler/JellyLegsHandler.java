package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EnvironmentalDamageTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class JellyLegsHandler implements OvercapEffectHandler, EnvironmentalDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("jelly_legs");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, int level) {
        if (player == null || damage == null || level <= 0) return;
        if (cause != DamageCause.FALL) return;

        damage.setCancelled(true);
    }
}