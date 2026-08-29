package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class ToolEnchantmentIntegrationTest {
    @Test
    void dispatcherReachesRebreatherBlockBreakHandler() {
        NamespacedKey key = key("rebreather");
        ItemStack tool = mock(ItemStack.class);
        OvercapItemAdapter adapter = adapterFor(tool, key);
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapter, EnchantmentRegistry.defaultRegistry());

        Player player = mock(Player.class);
        when(player.isUnderWater()).thenReturn(true);
        when(player.getRemainingAir()).thenReturn(100);
        when(player.getMaximumAir()).thenReturn(300);

        dispatcher.dispatchBlockBreak(player, mock(Block.class), tool, new CascadeScope(null, player, tool, 0));

        verify(player).setRemainingAir(120);
    }

    @Test
    void dispatcherReachesReforgedItemDamageHandler() {
        NamespacedKey key = key("reforged");
        ItemStack tool = mock(ItemStack.class);
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapterFor(tool, key), EnchantmentRegistry.defaultRegistry());

        assertEquals(4, dispatcher.dispatchItemDamage(null, tool, 5));
    }

    @Test
    void dispatcherReachesPlanterActiveInteractHandler() {
        NamespacedKey key = key("planter");
        ItemStack hoe = mock(ItemStack.class);
        when(hoe.isEmpty()).thenReturn(false);
        when(hoe.getType()).thenReturn(Material.DIAMOND_HOE);
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapterFor(hoe, key), EnchantmentRegistry.defaultRegistry());

        Player player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack seed = mock(ItemStack.class);
        when(seed.isEmpty()).thenReturn(false);
        when(seed.getType()).thenReturn(Material.WHEAT_SEEDS);
        when(seed.getAmount()).thenReturn(1);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{seed});

        Block farmland = mock(Block.class);
        when(farmland.getType()).thenReturn(Material.FARMLAND);
        Block crop = mock(Block.class);
        when(crop.isEmpty()).thenReturn(true);
        when(farmland.getRelative(org.bukkit.block.BlockFace.UP)).thenReturn(crop);

        dispatcher.dispatchActiveInteract(player, Action.RIGHT_CLICK_BLOCK, farmland, hoe);

        verify(crop).setType(Material.WHEAT, false);
    }

    @Test
    void listenerReachesPostBreakReplanterHandler() {
        NamespacedKey key = key("replanter");
        ItemStack hoe = mock(ItemStack.class);
        when(hoe.isEmpty()).thenReturn(false);
        when(hoe.getType()).thenReturn(Material.DIAMOND_HOE);
        ItemStack postBreakHoe = mock(ItemStack.class);
        when(postBreakHoe.isEmpty()).thenReturn(false);
        when(postBreakHoe.getType()).thenReturn(Material.DIAMOND_HOE);
        when(hoe.clone()).thenReturn(postBreakHoe);
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        when(adapter.readOvercap(hoe)).thenReturn(Map.of(key, 1));
        when(adapter.readOvercap(postBreakHoe)).thenReturn(Map.of(key, 1));
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapter, EnchantmentRegistry.defaultRegistry());
        ArrayList<Runnable> scheduled = new ArrayList<>();
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher, scheduled::add);

        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(hoe);

        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);
        BlockData data = mock(BlockData.class, withSettings().extraInterfaces(Ageable.class));
        Ageable ageable = (Ageable) data;
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);
        when(data.clone()).thenReturn(data);

        BlockState state = mock(BlockState.class);
        when(state.getBlockData()).thenReturn(data);
        when(state.getType()).thenReturn(Material.WHEAT);
        when(state.getWorld()).thenReturn(world);
        when(state.getLocation()).thenReturn(location);

        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.FARMLAND);
        Block replanted = mock(Block.class);
        when(replanted.isEmpty()).thenReturn(true);
        when(replanted.getRelative(0, -1, 0)).thenReturn(below);
        when(world.getBlockAt(location)).thenReturn(replanted);

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getState()).thenReturn(state);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);
        when(event.isCancelled()).thenReturn(false);
        when(event.isDropItems()).thenReturn(false);

        listener.onBlockBreak(event);

        assertEquals(1, scheduled.size());
        scheduled.get(0).run();
        verify(replanted).setType(Material.WHEAT, false);
        verify(replanted).setBlockData(data, true);
    }

    private static OvercapItemAdapter adapterFor(ItemStack item, NamespacedKey key) {
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 1));
        return adapter;
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("merlin", name);
    }
}
