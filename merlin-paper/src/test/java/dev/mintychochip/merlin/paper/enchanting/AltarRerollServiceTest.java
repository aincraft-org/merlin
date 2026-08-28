package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class AltarRerollServiceTest {
    @Test
    void rejectsWhenSessionClosed() {
        AltarRerollService.Result res = AltarRerollService.processReroll(true, Material.LAPIS_LAZULI, 5);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("closed"));
    }

    @Test
    void rejectsWhenMaterialNull() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, null, 0);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void rejectsWhenWrongMaterial() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, Material.DIAMOND, 5);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void rejectsWhenAmountZero() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, Material.LAPIS_LAZULI, 0);
        assertInstanceOf(AltarRerollService.Result.Failure.class, res);
        assertTrue(((AltarRerollService.Result.Failure) res).reason().contains("Lapis Lazuli"));
    }

    @Test
    void decrementsLapisAndReturnsSuccessWhenLapisAvailable() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, Material.LAPIS_LAZULI, 3);
        assertInstanceOf(AltarRerollService.Result.Success.class, res);
        AltarRerollService.Result.Success success = (AltarRerollService.Result.Success) res;
        assertEquals(2, success.newAmount());
    }

    @Test
    void returnsZeroRemainingWhenSingleLapisConsumed() {
        AltarRerollService.Result res = AltarRerollService.processReroll(false, Material.LAPIS_LAZULI, 1);
        assertInstanceOf(AltarRerollService.Result.Success.class, res);
        AltarRerollService.Result.Success success = (AltarRerollService.Result.Success) res;
        assertEquals(0, success.newAmount());
    }
}
