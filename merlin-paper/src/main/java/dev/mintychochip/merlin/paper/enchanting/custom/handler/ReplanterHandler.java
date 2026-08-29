package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public final class ReplanterHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("replanter");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {}

    @Override
    public void onBlockBreakPost(Player player, BlockState state, int level) {
        if (player == null || state == null || level <= 0) return;

        BlockData data = state.getBlockData();
        if (!(data instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return;

        Material cropType = state.getType();
        if (!canReplant(cropType)) return;

        World world = state.getWorld();
        if (world == null) return;

        Block replant = state.getLocation() == null ? null : world.getBlockAt(state.getLocation());
        if (replant == null) return;
        if (!replant.isEmpty()) return;
        Block below = replant.getRelative(0, -1, 0);
        if (below == null || !isValidSoil(cropType, below.getType())) return;

        BlockData replantData = data.clone();
        if (!(replantData instanceof Ageable replantAgeable)) return;
        replantAgeable.setAge(0);
        replant.setType(cropType, false);
        replant.setBlockData(replantData, true);
    }

    private static boolean canReplant(Material cropType) {
        return switch (cropType) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, PITCHER_CROP -> true;
            default -> false;
        };
    }

    private static boolean isValidSoil(Material cropType, Material soilType) {
        if (cropType == Material.NETHER_WART) {
            return soilType == Material.SOUL_SAND || soilType == Material.SOUL_SOIL;
        }
        return soilType == Material.FARMLAND;
    }
}
