package dev.mintychochip.merlin.paper.enchanting.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.AltarProfile;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OfferConfig;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.QuantaRollEngine;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class AltarGuiRerollTest {
    private AltarGuiSession createSession(Player player, Inventory inv) {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        OfferConfig offerConfig = OfferConfig.defaultConfig();
        QuantaRollEngine engine = new QuantaRollEngine(registry, offerConfig);
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        AltarProfile profile = new AltarProfile(30.0, 0.5, Map.of());
        Location loc = mock(Location.class);

        return new AltarGuiSession(player, loc, profile, registry, engine, adapter, inv);
    }

    @Test
    void rejectsRerollWhenLapisMissing() {
        Player player = mock(Player.class);
        Inventory inv = mock(Inventory.class);
        when(inv.getItem(AltarGuiSession.SLOT_LAPIS)).thenReturn(null);

        AltarGuiSession session = createSession(player, inv);
        boolean result = session.handleRerollClick();

        assertFalse(result);
        verify(player).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void rejectsRerollWhenLapisWrongMaterial() {
        Player player = mock(Player.class);
        Inventory inv = mock(Inventory.class);
        ItemStack wrongItem = mock(ItemStack.class);
        when(wrongItem.getType()).thenReturn(Material.DIRT);
        when(wrongItem.getAmount()).thenReturn(5);
        when(inv.getItem(AltarGuiSession.SLOT_LAPIS)).thenReturn(wrongItem);

        AltarGuiSession session = createSession(player, inv);
        boolean result = session.handleRerollClick();

        assertFalse(result);
        verify(player).sendMessage(org.mockito.ArgumentMatchers.any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void decrementsLapisAndRefreshesOffersOnValidReroll() {
        Player player = mock(Player.class);
        Inventory inv = mock(Inventory.class);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(3);
        when(inv.getItem(AltarGuiSession.SLOT_LAPIS)).thenReturn(lapis);

        final int[] amount = new int[]{3};
        org.mockito.Mockito.doAnswer(invocation -> {
            amount[0] = invocation.getArgument(0);
            return null;
        }).when(lapis).setAmount(org.mockito.ArgumentMatchers.anyInt());

        AltarGuiSession session = createSession(player, inv);
        boolean result = session.handleRerollClick();

        assertTrue(result);
        verify(lapis).setAmount(2);
        assertEquals(2, amount[0]);
    }

    @Test
    void clearsLapisSlotWhenLastLapisConsumed() {
        Player player = mock(Player.class);
        Inventory inv = mock(Inventory.class);
        ItemStack lapis = mock(ItemStack.class);
        when(lapis.getType()).thenReturn(Material.LAPIS_LAZULI);
        when(lapis.getAmount()).thenReturn(1);
        when(inv.getItem(AltarGuiSession.SLOT_LAPIS)).thenReturn(lapis);

        org.mockito.Mockito.doAnswer(invocation -> {
            when(lapis.getAmount()).thenReturn((Integer) invocation.getArgument(0));
            return null;
        }).when(lapis).setAmount(org.mockito.ArgumentMatchers.anyInt());

        AltarGuiSession session = createSession(player, inv);
        boolean result = session.handleRerollClick();

        assertTrue(result);
        verify(lapis).setAmount(0);
        verify(inv).setItem(AltarGuiSession.SLOT_LAPIS, null);
    }
}
