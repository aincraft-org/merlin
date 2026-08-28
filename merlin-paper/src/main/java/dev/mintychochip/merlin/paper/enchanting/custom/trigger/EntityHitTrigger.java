package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface EntityHitTrigger {
    void onEntityHit(Player attacker, LivingEntity victim, MutableDamage damage, int level);
}
