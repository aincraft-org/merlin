package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public interface BowShootTrigger {
    void onBowShoot(Player shooter, Projectile projectile, float force, int level);
}
