package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockDropTrigger;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MoltenTouchHandler implements OvercapEffectHandler, BlockDropTrigger {
    private static final NamespacedKey KEY = CustomEnchantmentSupport.customKey("molten_touch");

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    @Override
    public void onBlockDrop(Player player, BlockState blockState, List<Item> items, int level) {
        if (player == null || blockState == null || items == null || items.isEmpty() || level <= 0) return;

        for (Item item : items) {
            if (item == null) continue;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) continue;
            Material result = smeltedResult(stack.getType());
            if (result == null) continue;

            stack.setType(result);
            item.setItemStack(stack);

        }
    }

    private static Material smeltedResult(Material material) {
        if (material == null) return null;
        return switch (material) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case SAND, RED_SAND -> Material.GLASS;
            default -> null;
        };
    }
}
