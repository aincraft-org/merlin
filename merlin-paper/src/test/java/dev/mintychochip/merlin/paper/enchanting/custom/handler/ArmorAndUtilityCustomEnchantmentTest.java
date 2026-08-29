package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.Test;

final class ArmorAndUtilityCustomEnchantmentTest {
    @Test
    void stickyGripCancelsEveryPositiveLevelDrop() {
        Player player = org.mockito.Mockito.mock(Player.class);
        ItemStack item = org.mockito.Mockito.mock(ItemStack.class);

        assertTrue(new StickyGripHandler().shouldCancelDrop(player, item, 1));
        assertFalse(new StickyGripHandler().shouldCancelDrop(player, item, 0));
    }

    @Test
    void quenchingRestoresOnlyHungerLossAndCapsAtTwenty() {
        QuenchingHandler handler = new QuenchingHandler();
        Player player = org.mockito.Mockito.mock(Player.class);

        assertEquals(12, handler.onFoodLevelChange(player, 16, 10, 2));
        assertEquals(20, handler.onFoodLevelChange(player, 20, 1, 30));
        assertEquals(16, handler.onFoodLevelChange(player, 10, 16, 4));
        assertEquals(10, handler.onFoodLevelChange(player, 10, 10, 4));
        assertEquals(10, handler.onFoodLevelChange(player, 10, 10, 0));
    }

    @Test
    void coloramaMutatesLeatherArmorMetaOnly() {
        Player player = org.mockito.Mockito.mock(Player.class);
        ItemStack leather = org.mockito.Mockito.mock(ItemStack.class);
        LeatherArmorMeta leatherMeta = org.mockito.Mockito.mock(LeatherArmorMeta.class);
        when(leather.getItemMeta()).thenReturn(leatherMeta);

        assertEquals(3, new ColoramaHandler(new Random(7)).onItemDamage(player, leather, 3, 1));
        verify(leatherMeta).setColor(any(Color.class));
        verify(leather).setItemMeta(leatherMeta);

        ItemStack other = org.mockito.Mockito.mock(ItemStack.class);
        ItemMeta otherMeta = org.mockito.Mockito.mock(ItemMeta.class);
        when(other.getItemMeta()).thenReturn(otherMeta);
        assertEquals(3, new ColoramaHandler(new Random(7)).onItemDamage(player, other, 3, 1));
        verify(other, never()).setItemMeta(any());
    }

    @Test
    void leapingAddsPowerPerRankButRespectsApiMaximum() {
        AbstractHorse horse = org.mockito.Mockito.mock(AbstractHorse.class);
        LeapingHandler handler = new LeapingHandler();

        assertEquals(0.7f, handler.onHorseJump(horse, 0.5f, 2), 0.0001f);
        assertEquals(1.0f, handler.onHorseJump(horse, 0.95f, 2), 0.0001f);
        assertEquals(0.5f, handler.onHorseJump(horse, 0.5f, 0), 0.0001f);
    }

    @Test
    void featherHoovesCancelsFallDamageForHorsesOnly() {
        AbstractHorse horse = org.mockito.Mockito.mock(AbstractHorse.class);
        LivingEntity other = org.mockito.Mockito.mock(LivingEntity.class);
        MutableDamage fall = new MutableDamage(5.0);
        MutableDamage fire = new MutableDamage(5.0);
        MutableDamage otherFall = new MutableDamage(5.0);

        FeatherHoovesHandler handler = new FeatherHoovesHandler();
        handler.onEnvironmentalDamage(horse, DamageCause.FALL, fall, 1);
        handler.onEnvironmentalDamage(horse, DamageCause.FIRE, fire, 1);
        handler.onEnvironmentalDamage(other, DamageCause.FALL, otherFall, 1);

        assertTrue(fall.isCancelled());
        assertFalse(fire.isCancelled());
        assertFalse(otherFall.isCancelled());
    }

    @Test
    void prismaticRandomizesSheepOnly() {
        Player player = org.mockito.Mockito.mock(Player.class);
        ItemStack shears = org.mockito.Mockito.mock(ItemStack.class);
        Sheep sheep = org.mockito.Mockito.mock(Sheep.class);
        Entity other = org.mockito.Mockito.mock(Entity.class);
        when(shears.getType()).thenReturn(Material.SHEARS);

        PrismaticHandler handler = new PrismaticHandler(new Random(3));
        handler.onShearEntity(player, sheep, shears, EquipmentSlot.HAND, 1);
        handler.onShearEntity(player, other, shears, EquipmentSlot.HAND, 1);

        verify(sheep).setColor(any(DyeColor.class));
        verify(other, never()).getType();
    }

    @Test
    void overflowingRestoresWaterBucketInEitherHand() {
        assertBucketRestored(EquipmentSlot.HAND, new OverflowingHandler(), Material.WATER_BUCKET);
        assertBucketRestored(EquipmentSlot.OFF_HAND, new OverflowingHandler(), Material.WATER_BUCKET);
    }

    @Test
    void vacuumRestoresEmptyBucketInEitherHand() {
        assertBucketRestored(EquipmentSlot.HAND, new VacuumHandler(), Material.BUCKET);
        assertBucketRestored(EquipmentSlot.OFF_HAND, new VacuumHandler(), Material.BUCKET);
    }

    @Test
    void incompatibleContextsAreNoOps() {
        Player player = org.mockito.Mockito.mock(Player.class);
        ItemStack item = org.mockito.Mockito.mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STICK);
        when(item.getItemMeta()).thenReturn(org.mockito.Mockito.mock(ItemMeta.class));
        assertEquals(4, new ColoramaHandler().onItemDamage(player, item, 4, 1));

        LivingEntity other = org.mockito.Mockito.mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(4);
        new FeatherHoovesHandler().onEnvironmentalDamage(other, DamageCause.FALL, damage, 1);
        assertFalse(damage.isCancelled());

        ItemStack bucket = org.mockito.Mockito.mock(ItemStack.class);
        when(bucket.getType()).thenReturn(Material.LAVA_BUCKET);
        PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(bucket);
        new OverflowingHandler().onBucketEmpty(player, null, null, bucket, 1);
        verify(inventory, never()).setItemInMainHand(any(ItemStack.class));
        verify(inventory, never()).setItemInOffHand(any(ItemStack.class));
    }

    private static void assertBucketRestored(EquipmentSlot hand, Object handler, Material source) {
        Player player = org.mockito.Mockito.mock(Player.class);
        PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        ItemStack bucket = org.mockito.Mockito.mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(bucket.getType()).thenReturn(source);
        if (hand == EquipmentSlot.HAND) {
            when(inventory.getItemInMainHand()).thenReturn(bucket);
        } else {
            when(inventory.getItemInOffHand()).thenReturn(bucket);
        }

        if (handler instanceof OverflowingHandler overflowing) {
            overflowing.onBucketEmpty(player, null, null, bucket, 1);
        } else {
            ((VacuumHandler) handler).onBucketFill(player, null, null, bucket, 1);
        }

        Material expected = handler instanceof OverflowingHandler ? Material.WATER_BUCKET : Material.BUCKET;
        verify(bucket).setType(expected);
        if (hand == EquipmentSlot.HAND) {
            verify(inventory).setItemInMainHand(bucket);
        } else {
            verify(inventory).setItemInOffHand(bucket);
        }
    }
}
