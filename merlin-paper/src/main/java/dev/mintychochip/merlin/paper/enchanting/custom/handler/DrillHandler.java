package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class DrillHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("drill");
    private static final Set<Material> ORES = oreMaterials();
    private static final BlockFace[] FACES = {
            BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
        if (player == null || block == null || scope == null || level <= 0 || !ORES.contains(block.getType())) return;

        long requested = 4L * level;
        int limit = (int) Math.min(Integer.MAX_VALUE, requested);
        int visitedBlocks = 1;
        Set<Block> visited = new HashSet<>();
        ArrayDeque<Block> pending = new ArrayDeque<>();
        visited.add(block);
        pending.add(block);

        while (!pending.isEmpty() && visitedBlocks < limit) {
            Block current = pending.removeFirst();
            for (BlockFace face : FACES) {
                if (visitedBlocks >= limit) break;
                Block neighbor = current.getRelative(face);
                if (neighbor == null || !visited.add(neighbor) || neighbor.getType() != block.getType()) continue;
                visitedBlocks++;
                if (!scope.breakBlockSafely(neighbor, true)) return;
                pending.addLast(neighbor);
            }
        }
    }

    private static Set<Material> oreMaterials() {
        return Set.copyOf(EnumSet.of(
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
        ));
    }
}
