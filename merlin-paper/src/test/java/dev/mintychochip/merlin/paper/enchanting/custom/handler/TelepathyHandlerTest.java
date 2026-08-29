package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
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
}
