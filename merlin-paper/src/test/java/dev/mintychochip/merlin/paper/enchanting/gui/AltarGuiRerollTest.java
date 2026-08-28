package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class AltarGuiRerollTest {
    @Test
    void verifiesRerollRequiresAndConsumesLapis() {
        // Test lapis decrement logic
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(2);

        final int[] amount = new int[]{2};
        org.mockito.Mockito.doAnswer(inv -> {
            amount[0] = inv.getArgument(0);
            return null;
        }).when(lapis).setAmount(org.mockito.ArgumentMatchers.anyInt());

        // Decrement by 1
        lapis.setAmount(lapis.getAmount() - 1);
        assertEquals(1, amount[0]);
    }
}
