package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface ShearEntityTrigger {
    void onShearEntity(Player player, Entity shearedEntity, ItemStack shears, EquipmentSlot hand, int level);
}
