package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActiveInteractTrigger;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlanterHandler implements OvercapEffectHandler, ActiveInteractTrigger {
    private final NamespacedKey key;
    private final Material seedMaterial;
    private final Material cropMaterial;

    public PlanterHandler() {
        this("planter", Material.WHEAT_SEEDS, Material.WHEAT);
    }

    protected PlanterHandler(String key, Material seedMaterial, Material cropMaterial) {
        this.key = CustomEnchantmentSupport.customKey(Objects.requireNonNull(key, "key"));
        this.seedMaterial = Objects.requireNonNull(seedMaterial, "seedMaterial");
        this.cropMaterial = Objects.requireNonNull(cropMaterial, "cropMaterial");
    }

    @Override
    public final NamespacedKey key() {
        return key;
    }

    @Override
    public final void onActiveInteract(Player player, Action action, Block clickedBlock, ItemStack item, int level) {
        if (player == null || !player.isSneaking() || action != Action.RIGHT_CLICK_BLOCK
                || clickedBlock == null || clickedBlock.getType() != Material.FARMLAND
                || item == null || item.isEmpty() || !isHoe(item.getType()) || level <= 0) return;

        PlayerInventory inventory = player.getInventory();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block farmland = x == 0 && z == 0
                        ? clickedBlock
                        : clickedBlock.getRelative(x, 0, z);
                if (farmland == null || farmland.getType() != Material.FARMLAND) continue;

                Block crop = farmland.getRelative(BlockFace.UP);
                if (crop == null || !crop.isEmpty()) continue;
                if (!consumeSeed(inventory)) return;
                crop.setType(cropMaterial, false);
            }
        }
    }

    private boolean consumeSeed(PlayerInventory inventory) {
        if (inventory == null) return false;
        ItemStack[] contents = inventory.getStorageContents();
        if (contents == null) return false;

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.isEmpty() || stack.getType() != seedMaterial || stack.getAmount() <= 0) continue;

            if (stack.getAmount() == 1) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
            return true;
        }
        return false;
    }

    private static boolean isHoe(Material material) {
        return material != null && material.name().endsWith("_HOE");
    }
}
