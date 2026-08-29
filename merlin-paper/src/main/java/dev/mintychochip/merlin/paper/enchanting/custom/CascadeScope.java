package dev.mintychochip.merlin.paper.enchanting.custom;

import org.bukkit.Location;
import org.bukkit.Material;
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
    /**
     * Breaks a block inside the cascade scope, optionally suppressing item drops.
     * Paper has no natural-break overload that suppresses drops, so the no-drop
     * path clears a non-empty block directly.
     */
    public boolean breakBlockSafely(Block block, boolean dropItems) {
        if (!CascadeGuard.canCascade()) return false;
        final boolean[] success = new boolean[]{false};
        CascadeGuard.runInScope(() -> {
            if (dropItems) {
                success[0] = tool == null ? block.breakNaturally() : block.breakNaturally(tool, true);
            } else if (!block.isEmpty()) {
                block.setType(Material.AIR, true);
                success[0] = true;
            }
        });
        return success[0];
    }

    public void dropItemSafely(Location location, ItemStack item) {
        if (item == null || item.isEmpty() || location == null || location.getWorld() == null) return;
        location.getWorld().dropItemNaturally(location, item);
    }
}
