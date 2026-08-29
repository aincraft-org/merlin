package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BucketFillTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class VacuumHandler implements OvercapEffectHandler, BucketFillTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("vacuum");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBucketFill(
            Player player, Block clickedBlock, BlockFace face, ItemStack bucket, EquipmentSlot hand, int level) {
        if (player == null || bucket == null || bucket.getType() != Material.BUCKET || level <= 0) return;
        if (hand == null) return;

        PlayerInventory inventory = player.getInventory();
        if (inventory == null) return;
        bucket.setType(Material.BUCKET);
        setItem(inventory, hand, bucket);
    }
    private static void setItem(PlayerInventory inventory, EquipmentSlot hand, ItemStack item) {
        inventory.setItem(hand, item);
    }

}
