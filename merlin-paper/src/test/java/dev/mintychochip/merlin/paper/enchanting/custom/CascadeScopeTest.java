package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class CascadeScopeTest {
    @Test
    void breaksNaturallyWhenDropsAreRequested() {
        Block block = mock(Block.class);
        ItemStack tool = mock(ItemStack.class);
        when(block.breakNaturally(tool, true)).thenReturn(true);
        CascadeScope scope = new CascadeScope(null, null, tool, 0);

        assertTrue(scope.breakBlockSafely(block, true));

        verify(block).breakNaturally(tool, true);
        verify(block, never()).setType(any(Material.class), anyBoolean());
    }

    @Test
    void clearsBlockWithoutDropsWhenDropsAreSuppressed() {
        Block block = mock(Block.class);
        when(block.isEmpty()).thenReturn(false);
        CascadeScope scope = new CascadeScope(null, null, mock(ItemStack.class), 0);

        assertTrue(scope.breakBlockSafely(block, false));

        verify(block).setType(Material.AIR, true);
        verify(block, never()).breakNaturally(any(ItemStack.class), anyBoolean());
    }

    @Test
    void doesNotClearAnAlreadyEmptyBlock() {
        Block block = mock(Block.class);
        when(block.isEmpty()).thenReturn(true);
        CascadeScope scope = new CascadeScope(null, null, mock(ItemStack.class), 0);

        assertFalse(scope.breakBlockSafely(block, false));

        verify(block, never()).setType(any(Material.class), anyBoolean());
    }
}
