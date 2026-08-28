package dev.mintychochip.merlin.paper.enchanting.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Over-cap handler for Fortune IV+.
 *
 * <p>Deliberate balance design:
 * Vanilla Fortune III caps at an average 2.5x drop multiplier.
 * For each level above vanilla max (level > 3), this handler grants an additional
 * guaranteed +1 bonus drop per extra level for applicable ores.
 */
public final class FortuneOvercapHandler implements OvercapEffectHandler {
    private static final NamespacedKey KEY = NamespacedKey.minecraft("fortune");
    private final Random random;

    private static final Set<Material> FORTUNE_ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.AMETHYST_CLUSTER
    );

    public FortuneOvercapHandler() {
        this(new Random());
    }

    public FortuneOvercapHandler(Random random) {
        this.random = random;
    }

    @Override
    public NamespacedKey key() {
        return KEY;
    }

    public int calculateExtraDrops(int level, int vanillaMax) {
        if (level <= vanillaMax) return 0;
        return level - vanillaMax;
    }

    public boolean isApplicableBlock(Material material) {
        return FORTUNE_ORES.contains(material);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event, int level) {
        if (!event.isDropItems()) return;
        Block block = event.getBlock();
        if (!isApplicableBlock(block.getType())) return;

        int extra = calculateExtraDrops(level, 3);
        if (extra <= 0) return;

        Collection<ItemStack> baseDrops = block.getDrops(event.getPlayer().getInventory().getItemInMainHand());
        for (ItemStack drop : baseDrops) {
            ItemStack bonus = drop.clone();
            bonus.setAmount(extra);
            block.getWorld().dropItemNaturally(block.getLocation(), bonus);
        }
    }
}
