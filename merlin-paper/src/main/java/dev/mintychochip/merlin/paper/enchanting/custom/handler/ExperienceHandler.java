package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class ExperienceHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("experience");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
        if (player == null || block == null || level <= 0 || !isOre(block.getType())) return;
        player.giveExp(level);
    }

    private static boolean isOre(Material material) {
        return material != null && material.name().endsWith("_ORE");
    }
}
