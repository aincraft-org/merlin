package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.PlayerMoveTrigger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class WaterWalkerHandler implements OvercapEffectHandler, PlayerMoveTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("water_walker");
    private static final int RADIUS = 2;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onPlayerMove(Player player, Location from, Location to, int level) {
        if (player == null || to == null || level <= 0) return;
        if (!player.isOnGround()) return;

        World world = to.getWorld();
        if (world == null) return;

        int baseX = to.getBlockX();
        int baseY = to.getBlockY();
        int baseZ = to.getBlockZ();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (level == 1 && Math.abs(dx) + Math.abs(dz) > RADIUS) continue;
                Block block = world.getBlockAt(baseX + dx, baseY, baseZ + dz);
                if (block.getType() != Material.WATER) continue;

                Block below = block.getRelative(BlockFace.DOWN);
                if (below == null) continue;
                if (below.getType() == Material.AIR || below.getType() == Material.WATER) continue;

                block.setType(Material.FROSTED_ICE, false);
            }
        }
    }
}