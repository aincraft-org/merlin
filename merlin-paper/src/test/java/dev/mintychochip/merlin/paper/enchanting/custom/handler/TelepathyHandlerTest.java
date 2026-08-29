package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class TelepathyHandlerTest {
    @Test
    void teleportsMinedDropsToPlayerInventory() {
        TelepathyHandler handler = new TelepathyHandler();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.DIAMOND);
        when(stack.isEmpty()).thenReturn(false);

        Item item = mock(Item.class);
        when(item.getItemStack()).thenReturn(stack);

        List<Item> drops = new ArrayList<>(List.of(item));
        BlockState state = mock(BlockState.class);

        handler.onBlockDrop(player, state, drops, 1);

        assertTrue(drops.isEmpty(), "Drop entities should be consumed");
        verify(inventory).addItem(stack);
        verify(item).remove();
    }

    @Test
    void dropsLeftoversWhenInventoryIsFull() {
        TelepathyHandler handler = new TelepathyHandler();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        Location location = new Location(world, 1, 2, 3);

        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(Material.DIAMOND);
        when(stack.isEmpty()).thenReturn(false);

        ItemStack leftover = mock(ItemStack.class);
        HashMap<Integer, ItemStack> overflow = new HashMap<>();
        overflow.put(0, leftover);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(overflow);

        Item item = mock(Item.class);
        when(item.getItemStack()).thenReturn(stack);

        BlockState state = mock(BlockState.class);
        when(state.getLocation()).thenReturn(location);

        List<Item> drops = new ArrayList<>(List.of(item));

        handler.onBlockDrop(player, state, drops, 1);

        assertEquals(0, drops.size(), "Original drops should be removed");
        verify(item).remove();
        verify(world).dropItemNaturally(location, leftover);
    }

    @Test
    void doesNothingWhenLevelIsZero() {
        TelepathyHandler handler = new TelepathyHandler();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);

        ItemStack stack = mock(ItemStack.class);
        Item item = mock(Item.class);
        when(item.getItemStack()).thenReturn(stack);

        List<Item> drops = new ArrayList<>(List.of(item));

        handler.onBlockDrop(player, null, drops, 0);

        assertEquals(1, drops.size());
        verify(inventory, never()).addItem(any(ItemStack[].class));
    }
}
