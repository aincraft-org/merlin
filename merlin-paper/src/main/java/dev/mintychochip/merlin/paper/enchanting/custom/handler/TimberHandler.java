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

public final class TimberHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("timber");
    private static final Set<Material> LOGS = logMaterials();
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
        if (player == null || block == null || scope == null || level <= 0 || !LOGS.contains(block.getType())) return;

        long requested = 4L * level;
        int limit = (int) Math.min(Integer.MAX_VALUE, requested);
        int broken = 0;
        Set<Block> visited = new HashSet<>();
        ArrayDeque<Block> pending = new ArrayDeque<>();

        visited.add(block);
        pending.add(block);

        while (!pending.isEmpty() && broken < limit) {
            Block current = pending.removeFirst();
            for (BlockFace face : FACES) {
                if (broken >= limit) break;
                Block neighbor = current.getRelative(face);
                if (neighbor == null || !LOGS.contains(neighbor.getType()) || !visited.add(neighbor)) continue;
                if (!scope.breakBlockSafely(neighbor, true)) return;
                broken++;
                pending.addLast(neighbor);
            }
        }
    }

    private static Set<Material> logMaterials() {
        return Set.copyOf(EnumSet.of(
                Material.OAK_LOG, Material.STRIPPED_OAK_LOG,
                Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG,
                Material.BIRCH_LOG, Material.STRIPPED_BIRCH_LOG,
                Material.JUNGLE_LOG, Material.STRIPPED_JUNGLE_LOG,
                Material.ACACIA_LOG, Material.STRIPPED_ACACIA_LOG,
                Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG,
                Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG,
                Material.CHERRY_LOG, Material.STRIPPED_CHERRY_LOG,
                Material.PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_LOG,
                Material.OAK_WOOD, Material.STRIPPED_OAK_WOOD,
                Material.SPRUCE_WOOD, Material.STRIPPED_SPRUCE_WOOD,
                Material.BIRCH_WOOD, Material.STRIPPED_BIRCH_WOOD,
                Material.JUNGLE_WOOD, Material.STRIPPED_JUNGLE_WOOD,
                Material.ACACIA_WOOD, Material.STRIPPED_ACACIA_WOOD,
                Material.DARK_OAK_WOOD, Material.STRIPPED_DARK_OAK_WOOD,
                Material.MANGROVE_WOOD, Material.STRIPPED_MANGROVE_WOOD,
                Material.CHERRY_WOOD, Material.STRIPPED_CHERRY_WOOD,
                Material.PALE_OAK_WOOD, Material.STRIPPED_PALE_OAK_WOOD,
                Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK,
                Material.CRIMSON_STEM, Material.STRIPPED_CRIMSON_STEM,
                Material.WARPED_STEM, Material.STRIPPED_WARPED_STEM,
                Material.CRIMSON_HYPHAE, Material.STRIPPED_CRIMSON_HYPHAE,
                Material.WARPED_HYPHAE, Material.STRIPPED_WARPED_HYPHAE
        ));
    }
}
