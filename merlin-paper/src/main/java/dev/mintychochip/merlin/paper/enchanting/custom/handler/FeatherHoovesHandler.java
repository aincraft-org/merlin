package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityEnvironmentalDamageTrigger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class FeatherHoovesHandler implements OvercapEffectHandler, EntityEnvironmentalDamageTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("feather_hooves");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onEnvironmentalDamage(LivingEntity entity, DamageCause cause, MutableDamage damage, int level) {
        if (entity instanceof AbstractHorse && cause == DamageCause.FALL && damage != null && level > 0) {
            damage.setCancelled(true);
        }
    }
}
