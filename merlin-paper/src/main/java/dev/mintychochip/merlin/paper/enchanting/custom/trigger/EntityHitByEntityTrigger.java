package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** Triggered for enchantments carried by the entity receiving a hit. */
public interface EntityHitByEntityTrigger {
    void onEntityHitByEntity(LivingEntity victim, Entity attacker, MutableDamage damage, int level);
}
