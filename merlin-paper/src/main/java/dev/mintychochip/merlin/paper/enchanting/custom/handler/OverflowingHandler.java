package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BucketEmptyTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class OverflowingHandler implements OvercapEffectHandler, BucketEmptyTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("overflowing");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBucketEmpty(Player player, Block clickedBlock, BlockFace face, ItemStack bucket, int level) {
        if (player == null || bucket == null || bucket.getType() != Material.WATER_BUCKET || level <= 0) return;

        PlayerInventory inventory = player.getInventory();
        if (inventory == null) return;
        EquipmentSlot hand = matchingHand(inventory, bucket);
        if (hand == null) return;
        bucket.setType(Material.WATER_BUCKET);
        setItem(inventory, hand, bucket);
    }

    private static void setItem(PlayerInventory inventory, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.HAND) inventory.setItemInMainHand(item);
        else inventory.setItemInOffHand(item);
    }

    private static EquipmentSlot matchingHand(PlayerInventory inventory, ItemStack eventItem) {
        ItemStack main = inventory.getItemInMainHand();
        if (sameItem(main, eventItem)) return EquipmentSlot.HAND;
        ItemStack off = inventory.getItemInOffHand();
        return sameItem(off, eventItem) ? EquipmentSlot.OFF_HAND : null;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first == second || (first != null && first.equals(second));
    }
}
