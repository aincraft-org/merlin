package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class PlanterHandlerTest {

    @Test
    void plantsNineWheatCropsAndConsumesNineSeeds() {
        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        AtomicInteger seedAmount = new AtomicInteger(9);
        ItemStack seeds = mock(ItemStack.class);
        when(seeds.isEmpty()).thenReturn(false);
        when(seeds.getType()).thenReturn(Material.WHEAT_SEEDS);
        when(seeds.getAmount()).thenAnswer(invocation -> seedAmount.get());
        doAnswer(invocation -> {
            seedAmount.set(invocation.getArgument(0));
            return null;
        }).when(seeds).setAmount(anyInt());
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{seeds});
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == null) seedAmount.set(0);
            return null;
        }).when(inventory).setItem(anyInt(), isNull());

        ItemStack hoe = mock(ItemStack.class);
        when(hoe.isEmpty()).thenReturn(false);
        when(hoe.getType()).thenReturn(Material.DIAMOND_HOE);

        Block clicked = mock(Block.class);
        when(clicked.getType()).thenReturn(Material.FARMLAND);

        List<Block> crops = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block farmland = x == 0 && z == 0 ? clicked : mock(Block.class);
                when(farmland.getType()).thenReturn(Material.FARMLAND);
                Block crop = mock(Block.class);
                when(crop.isEmpty()).thenReturn(true);
                when(farmland.getRelative(BlockFace.UP)).thenReturn(crop);
                crops.add(crop);
                if (!(x == 0 && z == 0)) {
                    when(clicked.getRelative(x, 0, z)).thenReturn(farmland);
                }
            }
        }

        new PlanterHandler().onActiveInteract(player, Action.RIGHT_CLICK_BLOCK, clicked, hoe, 1);

        assertEquals(0, seedAmount.get());
        for (Block crop : crops) {
            verify(crop).setType(Material.WHEAT, false);
        }
        verify(inventory).setItem(0, null);
    }

    @Test
    void carrotAndPotatoPlantersUseMatchingCrops() {
        assertSpecializedPlanter(new CarrotPlanterHandler(), Material.CARROT, Material.CARROTS);
        assertSpecializedPlanter(new PotatoPlanterHandler(), Material.POTATO, Material.POTATOES);
    }

    @Test
    void requiresSneakingRightClickWithHoeOnFarmland() {
        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack hoe = mock(ItemStack.class);
        when(hoe.isEmpty()).thenReturn(false);
        when(hoe.getType()).thenReturn(Material.DIAMOND_HOE);
        Block clicked = mock(Block.class);
        when(clicked.getType()).thenReturn(Material.FARMLAND);

        new PlanterHandler().onActiveInteract(player, Action.RIGHT_CLICK_BLOCK, clicked, hoe, 1);

        verify(inventory, never()).getStorageContents();
        verify(clicked, never()).getRelative(anyInt(), anyInt(), anyInt());
    }

    private static void assertSpecializedPlanter(PlanterHandler handler, Material seedMaterial, Material cropMaterial) {
        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        AtomicInteger seedAmount = new AtomicInteger(1);
        ItemStack seed = mock(ItemStack.class);
        when(seed.isEmpty()).thenReturn(false);
        when(seed.getType()).thenReturn(seedMaterial);
        when(seed.getAmount()).thenAnswer(invocation -> seedAmount.get());
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{seed});
        doAnswer(invocation -> {
            if (invocation.getArgument(1) == null) seedAmount.set(0);
            return null;
        }).when(inventory).setItem(anyInt(), isNull());
        doAnswer(invocation -> {
            seedAmount.set(invocation.getArgument(0));
            return null;
        }).when(seed).setAmount(anyInt());

        ItemStack hoe = mock(ItemStack.class);
        when(hoe.isEmpty()).thenReturn(false);
        when(hoe.getType()).thenReturn(Material.DIAMOND_HOE);

        Block clicked = mock(Block.class);
        when(clicked.getType()).thenReturn(Material.FARMLAND);
        Block crop = mock(Block.class);
        when(crop.isEmpty()).thenReturn(true);
        when(clicked.getRelative(BlockFace.UP)).thenReturn(crop);

        handler.onActiveInteract(player, Action.RIGHT_CLICK_BLOCK, clicked, hoe, 1);

        verify(crop).setType(cropMaterial, false);
        assertEquals(0, seedAmount.get());
    }
}
