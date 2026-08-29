package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public interface EnvironmentalDamageTrigger {
    void onEnvironmentalDamage(Player player, DamageCause cause, MutableDamage damage, int level);
}
