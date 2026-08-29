package dev.mintychochip.merlin.paper.enchanting.custom.trigger;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface BucketFillTrigger {
    void onBucketFill(Player player, Block clickedBlock, BlockFace face, ItemStack bucket, int level);
}
