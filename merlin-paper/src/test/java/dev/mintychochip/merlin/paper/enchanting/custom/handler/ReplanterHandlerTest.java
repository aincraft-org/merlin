package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class ReplanterHandlerTest {

    @Test
    void replantsMatureWheatAndKeepsDrops() {
        Player player = mock(Player.class);
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

        Block replant = mock(Block.class);
        when(replant.isEmpty()).thenReturn(true);
        when(replant.getRelative(0, -1, 0)).thenReturn(below);

        when(world.getBlockAt(location)).thenReturn(replant);

        Item drop = mock(Item.class);
        ItemStack dropStack = mock(ItemStack.class);
        when(drop.getItemStack()).thenReturn(dropStack);
        List<Item> drops = new ArrayList<>(List.of(drop));

        new ReplanterHandler().onBlockBreakPost(player, state, 1);

        verify(replant).setType(Material.WHEAT, false);
        verify(replant).setBlockData(data, true);
        verify(ageable).setAge(0);
        assertEquals(1, drops.size(), "Drops should not be removed");
    }

    @Test
    void doesNotReplantImmatureCrop() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);

        BlockData data = mock(BlockData.class, withSettings().extraInterfaces(Ageable.class));
        Ageable ageable = (Ageable) data;
        when(ageable.getAge()).thenReturn(3);
        when(ageable.getMaximumAge()).thenReturn(7);

        BlockState state = mock(BlockState.class);
        when(state.getBlockData()).thenReturn(data);
        when(state.getType()).thenReturn(Material.WHEAT);
        when(state.getWorld()).thenReturn(world);
        when(state.getLocation()).thenReturn(location);

        Block replant = mock(Block.class);
        when(world.getBlockAt(location)).thenReturn(replant);

        new ReplanterHandler().onBlockBreakPost(player, state, 1);

        verify(replant, never()).setType(any(), anyBoolean());
        verify(replant, never()).setBlockData(any(), anyBoolean());
    }

    @Test
    void doesNotReplantWithoutFarmland() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);

        BlockData data = mock(BlockData.class, withSettings().extraInterfaces(Ageable.class));
        Ageable ageable = (Ageable) data;
        when(ageable.getAge()).thenReturn(7);
        when(ageable.getMaximumAge()).thenReturn(7);

        BlockState state = mock(BlockState.class);
        when(state.getBlockData()).thenReturn(data);
        when(state.getType()).thenReturn(Material.WHEAT);
        when(state.getWorld()).thenReturn(world);
        when(state.getLocation()).thenReturn(location);

        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.DIRT);

        Block replant = mock(Block.class);
        when(replant.getRelative(0, -1, 0)).thenReturn(below);

        when(world.getBlockAt(location)).thenReturn(replant);

        new ReplanterHandler().onBlockBreakPost(player, state, 1);

        verify(replant, never()).setType(any(), anyBoolean());
    }

    @Test
    void replantsNetherWartOnSoulSand() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 1, 2, 3);

        BlockData data = mock(BlockData.class, withSettings().extraInterfaces(Ageable.class));
        Ageable ageable = (Ageable) data;
        when(ageable.getAge()).thenReturn(3);
        when(ageable.getMaximumAge()).thenReturn(3);
        when(data.clone()).thenReturn(data);

        BlockState state = mock(BlockState.class);
        when(state.getBlockData()).thenReturn(data);
        when(state.getType()).thenReturn(Material.NETHER_WART);
        when(state.getWorld()).thenReturn(world);
        when(state.getLocation()).thenReturn(location);

        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.SOUL_SAND);

        Block replant = mock(Block.class);
        when(replant.isEmpty()).thenReturn(true);
        when(replant.getRelative(0, -1, 0)).thenReturn(below);

        when(world.getBlockAt(location)).thenReturn(replant);

        new ReplanterHandler().onBlockBreakPost(player, state, 1);

        verify(replant).setType(Material.NETHER_WART, false);
        verify(replant).setBlockData(data, true);
    }

    @Test
    void doesNothingWhenLevelIsZero() {
        Player player = mock(Player.class);
        BlockState state = mock(BlockState.class);

        new ReplanterHandler().onBlockBreakPost(player, state, 0);

        verify(state, never()).getBlockData();
    }
}
