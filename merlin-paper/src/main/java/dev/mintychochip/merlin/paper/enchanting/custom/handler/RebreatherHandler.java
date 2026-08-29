package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.CascadeScope;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;

public final class RebreatherHandler implements OvercapEffectHandler, BlockBreakTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("rebreather");
    private static final int AIR_PER_LEVEL = 20;

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(Player player, Block block, int level, CascadeScope scope) {
        if (player == null || level <= 0) return;
        if (!player.isUnderWater()) return;
        int currentAir = player.getRemainingAir();
        int maximumAir = player.getMaximumAir();
        if (maximumAir <= currentAir) return;

        long restoredAir = (long) Math.max(0, currentAir) + (long) AIR_PER_LEVEL * level;
        player.setRemainingAir((int) Math.min(maximumAir, restoredAir));
    }
}
