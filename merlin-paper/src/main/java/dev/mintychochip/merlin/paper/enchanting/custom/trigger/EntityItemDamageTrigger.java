package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/** Triggered when Paper damages an item carried by a non-player entity. */
public interface EntityItemDamageTrigger {
    int onEntityItemDamage(Entity entity, ItemStack item, int originalDamageAmount, int level);
}
