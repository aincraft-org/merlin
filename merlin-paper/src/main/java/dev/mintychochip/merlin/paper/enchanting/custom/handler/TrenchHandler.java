package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class TrenchHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("trench");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
        if (player == null || block == null || scope == null || level <= 0) return;

        Location playerLoc = player.getLocation();
        double dx = playerLoc.getX() - (block.getX() + 0.5);
        double dy = playerLoc.getY() - (block.getY() + 0.5);
        double dz = playerLoc.getZ() - (block.getZ() + 0.5);

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        // Break a 3x3 area in the plane orthogonal to the dominant player->block vector.
        // The center block is skipped; the original break event already removes it.
        if (absX >= absY && absX >= absZ) {
            breakPlane(block, scope, PlaneAxis.Y, PlaneAxis.Z);
        } else if (absY >= absX && absY >= absZ) {
            breakPlane(block, scope, PlaneAxis.X, PlaneAxis.Z);
        } else {
            breakPlane(block, scope, PlaneAxis.X, PlaneAxis.Y);
        }
    }

    private static void breakPlane(Block center, CascadeScope scope, PlaneAxis axisA, PlaneAxis axisB) {
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;

                int modX = 0;
                int modY = 0;
                int modZ = 0;

                switch (axisA) {
                    case X -> modX = a;
                    case Y -> modY = a;
                    case Z -> modZ = a;
                }
                switch (axisB) {
                    case X -> modX = b;
                    case Y -> modY = b;
                    case Z -> modZ = b;
                }

                Block neighbor = center.getRelative(modX, modY, modZ);
                if (neighbor == null || neighbor.isEmpty()) continue;
                if (!scope.breakBlockSafely(neighbor, true)) return;
            }
        }
    }

    private enum PlaneAxis {
        X, Y, Z
    }
}
