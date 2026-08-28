package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentListenerTest {
    @Test
    void adaptsEntityDamageEventAndDispatches() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player attacker = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(attacker.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(sword);

        LivingEntity victim = mock(LivingEntity.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamage()).thenReturn(10.0);

        final double[] modifiedDamage = new double[]{10.0};
        org.mockito.Mockito.doAnswer(invocation -> {
            modifiedDamage[0] = invocation.getArgument(0);
            return null;
        }).when(event).setDamage(org.mockito.ArgumentMatchers.anyDouble());

        // Configure dispatcher mock to verify depth is 1 inside dispatch and add bonus damage
        org.mockito.Mockito.doAnswer(invocation -> {
            assertEquals(1, CascadeGuard.getDepth());
            MutableDamage dmg = invocation.getArgument(2);
            dmg.addBonus(5.0);
            return null;
        }).when(dispatcher).dispatchEntityHit(eq(attacker), eq(victim), any(MutableDamage.class), eq(sword));

        listener.onAttack(event);
        verify(dispatcher).dispatchEntityHit(eq(attacker), eq(victim), any(MutableDamage.class), eq(sword));
        assertEquals(15.0, modifiedDamage[0], 0.001);
    }

    @Test
    void adaptsBlockBreakEventAndDispatches() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack pickaxe = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(pickaxe);

        Block block = mock(Block.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        listener.onBlockBreak(event);
        verify(dispatcher).dispatchBlockBreak(eq(player), eq(block), eq(pickaxe), any(CascadeScope.class));
    }

    @Test
    void skipsDispatchWhenCascadeDepthMaxed() {
        CustomEnchantmentDispatcher dispatcher = mock(CustomEnchantmentDispatcher.class);
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher);

        Player player = mock(Player.class);
        Block block = mock(Block.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlock()).thenReturn(block);

        // Run at max cascade depth
        CascadeGuard.runInScope(() -> {
            CascadeGuard.runInScope(() -> {
                CascadeGuard.runInScope(() -> {
                    assertEquals(3, CascadeGuard.getDepth());
                    listener.onBlockBreak(event);
                    verify(dispatcher, never()).dispatchBlockBreak(any(), any(), any(), any());
                });
            });
        });
    }
}
