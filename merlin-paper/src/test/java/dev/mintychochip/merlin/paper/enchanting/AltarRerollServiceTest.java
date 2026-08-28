package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class AltarRerollServiceTest {
    @Test
    void rejectsWhenSessionClosed() {
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(5);

        AltarRerollService.Result res = AltarRerollService.processReroll(true, lapis);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("closed"));
    }

    @Test
    void rejectsWhenLapisMissingOrNull() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, null);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void rejectsWhenWrongMaterial() {
        ItemStack wrong = mock(ItemStack.class);
        when(wrong.getType()).thenReturn(Material.DIAMOND);
        when(wrong.getAmount()).thenReturn(5);

        AltarRerollService.Result res = AltarRerollService.processReroll(false, wrong);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void rejectsWhenAmountLessThanOne() {
        ItemStack emptyLapis = mock(ItemStack.class);
        when(emptyLapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(emptyLapis.getAmount()).thenReturn(0);

        AltarRerollService.Result res = AltarRerollService.processReroll(false, emptyLapis);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void decrementsLapisAndReturnsSuccessWhenLapisAvailable() {
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);

        AltarRerollService.Result res = AltarRerollService.processReroll(false, lapis);
        assertInstanceOf(AltarRerollService.Result.Success.class, res);
        AltarRerollService.Result.Success success = (AltarRerollService.Result.Success) res;
        assertEquals(2, success.remainingLapis());
        verify(lapis).setAmount(2);
    }

    @Test
    void returnsZeroRemainingWhenSingleLapisConsumed() {
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(1);

        AltarRerollService.Result res = AltarRerollService.processReroll(false, lapis);
        assertInstanceOf(AltarRerollService.Result.Success.class, res);
        AltarRerollService.Result.Success success = (AltarRerollService.Result.Success) res;
        assertEquals(0, success.remainingLapis());
        verify(lapis).setAmount(0);
    }
}
