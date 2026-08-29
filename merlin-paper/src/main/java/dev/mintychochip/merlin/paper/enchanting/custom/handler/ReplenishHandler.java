package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class ReplenishHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("replenish");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
        if (player == null || block == null || level <= 0 || !isOre(block.getType())) return;

        int foodBefore = player.getFoodLevel();
        float saturationBefore = player.getSaturation();
        int foodAfter = Math.min(20, Math.max(0, foodBefore) + level);
        float saturationAfter = Math.min(foodAfter, Math.max(0.0f, saturationBefore) + level * 0.5f);

        if (foodAfter != foodBefore) player.setFoodLevel(foodAfter);
        if (saturationAfter != saturationBefore) player.setSaturation(saturationAfter);
    }

    private static boolean isOre(Material material) {
        return material != null && material.name().endsWith("_ORE");
    }
}
