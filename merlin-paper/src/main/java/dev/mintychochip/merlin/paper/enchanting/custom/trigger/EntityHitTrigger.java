package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.LivingEntity;

public interface EntityHitTrigger {
    void onEntityHit(LivingEntity attacker, LivingEntity victim, MutableDamage damage, int level);
}
