package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public interface EntityEnvironmentalDamageTrigger {
    void onEnvironmentalDamage(LivingEntity entity, DamageCause cause, MutableDamage damage, int level);
}
