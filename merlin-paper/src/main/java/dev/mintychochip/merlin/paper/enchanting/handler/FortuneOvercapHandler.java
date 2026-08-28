package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockBreakEvent;

public final class FortuneOvercapHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = NamespacedKey.minecraft("fortune");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, int level) {
        // Overcap fortune logic placeholder for drop multiplier extension
    }
}
