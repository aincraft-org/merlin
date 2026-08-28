package dev.mintychochip.merlin.paper.enchanting.custom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record CascadeScope(
        World world,
        Player player,
        ItemStack tool,
        int currentDepth
) {
    public boolean breakBlockSafely(Block block, boolean dropItems) {
        if (!CascadeGuard.canCascade()) return false;
        final boolean[] success = new boolean[]{false};
        CascadeGuard.runInScope(() -> {
            success[0] = block.breakNaturally(tool, dropItems);
        });
        return success[0];
    }

    public void dropItemSafely(Location location, ItemStack item) {
        if (item == null || item.isEmpty() || location == null || location.getWorld() == null) return;
        location.getWorld().dropItemNaturally(location, item);
    }
}
