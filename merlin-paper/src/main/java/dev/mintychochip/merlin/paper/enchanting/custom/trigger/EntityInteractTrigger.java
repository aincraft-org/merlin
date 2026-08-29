package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface EntityInteractTrigger {
    void onEntityInteract(Player player, Entity rightClicked, ItemStack item, EquipmentSlot hand, int level);
}
