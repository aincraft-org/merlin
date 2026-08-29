package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActiveInteractTrigger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public final class HeatWaveHandler implements OvercapEffectHandler, ActiveInteractTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("heat_wave");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onActiveInteract(Player player, Action action, Block clickedBlock, ItemStack item, int level) {
        if (player == null || action != Action.RIGHT_CLICK_BLOCK || clickedBlock == null || level <= 0) return;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block target = clickedBlock.getRelative(x, 0, z);
                if (target != null && target.isEmpty()) target.setType(Material.FIRE, true);
            }
        }
    }
}
