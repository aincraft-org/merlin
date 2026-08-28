package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface ArmorDefenseTrigger {
    void onArmorDefense(Player defender, Entity attacker, MutableDamage damage, int level);
}
