package dev.mintychochip.merlin.paper.enchanting.custom.passive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

final class PassiveEquipTest {

    private static ItemStack armorWith(Material material, String enchantKey, int level) {
        ItemStack item = mock(ItemStack.class);
        org.bukkit.inventory.meta.ItemMeta meta = mock(org.bukkit.inventory.meta.ItemMeta.class);
        PersistentDataContainer root = mock(PersistentDataContainer.class);
        PersistentDataContainer sub = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(root);
        org.bukkit.persistence.PersistentDataAdapterContext context =
                mock(org.bukkit.persistence.PersistentDataAdapterContext.class);
        when(root.getAdapterContext()).thenReturn(context);
        when(context.newPersistentDataContainer()).thenReturn(sub);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.isEmpty()).thenReturn(false);
        when(root.get(new org.bukkit.NamespacedKey("merlin", "overcap_enchantments"),
                org.bukkit.persistence.PersistentDataType.TAG_CONTAINER)).thenReturn(sub);
        when(sub.get(new org.bukkit.NamespacedKey("merlin", enchantKey),
                org.bukkit.persistence.PersistentDataType.INTEGER)).thenReturn(level);
        return item;
    }

    @Test
    void appliesSpeedAndRemovesOnUnequip() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);

        PassiveEffectApplier applier = new PassiveEffectApplier();

        applier.refresh(player, List.of(armorWith(Material.DIAMOND_BOOTS, "gears", 2)));

        verify(player).addPotionEffect(
                org.mockito.ArgumentMatchers.argThat(e ->
                        e.getType() == PotionEffectType.SPEED
                                && e.getAmplifier() == 1
                                && e.getDuration() == PotionEffect.INFINITE_DURATION),
                org.mockito.ArgumentMatchers.eq(true));

        applier.refresh(player, List.of(armorWith(Material.DIAMOND_BOOTS, "gears", 0)));
        verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    @Test
    void appliesMaxHealthModifierForOverload() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        when(maxHealth.getModifiers()).thenReturn(List.of());
        when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(maxHealth);
        PassiveEffectApplier applier = new PassiveEffectApplier();

        applier.refresh(player, List.of(armorWith(Material.DIAMOND_CHESTPLATE, "overload", 2)));

        verify(maxHealth).addModifier(org.mockito.ArgumentMatchers.argThat(m ->
                m.getAmount() == 4.0 && m.getOperation() == AttributeModifier.Operation.ADD_NUMBER));
    }

    @Test
    void armorSwapViaScheduledRefreshAppliesNewEffect() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack[] armor = {
                armorWith(Material.DIAMOND_BOOTS, "gears", 1), null, null, null};
        when(inventory.getArmorContents()).thenReturn(armor);
        ItemStack offhand = mock(ItemStack.class);
        when(offhand.isEmpty()).thenReturn(true);
        when(inventory.getItemInOffHand()).thenReturn(offhand);
        when(player.getInventory()).thenReturn(inventory);

        List<Runnable> scheduled = new ArrayList<>();
        PassiveEquipListener listener = new PassiveEquipListener(new PassiveEffectApplier(), scheduled::add);

        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player);
        when(click.getClickedInventory()).thenReturn(mock(org.bukkit.inventory.Inventory.class));
        when(click.getSlotType()).thenReturn(InventoryType.SlotType.ARMOR);
        when(click.getAction()).thenReturn(org.bukkit.event.inventory.InventoryAction.PLACE_ALL);

        listener.onInventoryClick(click);

        assertEquals(1, scheduled.size());
        scheduled.get(0).run();
        verify(player).addPotionEffect(org.mockito.ArgumentMatchers.any(PotionEffect.class),
                org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void implantsRestoresFoodWhileEquipped() {
        java.util.UUID id = java.util.UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        when(player.getFoodLevel()).thenReturn(10);
        when(player.getSaturation()).thenReturn(2.0f);
        PassiveEffectApplier applier = new PassiveEffectApplier(ignored -> player);

        applier.refresh(player, List.of(armorWith(Material.IRON_HELMET, "implants", 1)));
        verify(player).setFoodLevel(11);
        verify(player).setSaturation(2.5f);

        // Periodic tick restores more food.
        when(player.getFoodLevel()).thenReturn(12);
        applier.tickImplants();
        verify(player, org.mockito.Mockito.times(2)).setFoodLevel(org.mockito.ArgumentMatchers.anyInt());
        verify(player).setFoodLevel(13);

        // Unequip stops future ticks.
        applier.refresh(player, List.of(armorWith(Material.IRON_HELMET, "implants", 0)));
        applier.tickImplants();
        verify(player, org.mockito.Mockito.times(2)).setFoodLevel(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void wingsEnablesFlightAndRevertsOnUnequip() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        when(player.getAllowFlight()).thenReturn(false);
        PassiveEffectApplier applier = new PassiveEffectApplier();

        applier.refresh(player, List.of(armorWith(Material.ELYTRA, "wings", 1)));
        verify(player).setAllowFlight(true);

        applier.refresh(player, List.of(armorWith(Material.ELYTRA, "wings", 0)));
        verify(player).setAllowFlight(false);
    }
}