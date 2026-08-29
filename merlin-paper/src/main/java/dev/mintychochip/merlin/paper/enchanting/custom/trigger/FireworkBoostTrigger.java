package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Triggered when a player's firework explodes while they are gliding. */
public interface FireworkBoostTrigger {
    void onFireworkBoost(Player player, LivingEntity shooter, ItemStack elytra, int level);
}