package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.CustomEnchantmentDispatcher;
import dev.mintychochip.merlin.paper.enchanting.custom.CustomEnchantmentListener;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
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
        new OverflowingHandler().onBucketEmpty(player, null, null, bucket, EquipmentSlot.HAND, 1);
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any(ItemStack.class));
    }

    @Test
    void bucketListenersResolvePreActionItemsAndRestoreTheEventHandAfterVanillaCompletion() {
        OvercapItemAdapter adapter = org.mockito.Mockito.mock(OvercapItemAdapter.class);
        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                CustomEnchantmentSupport.customKey("overflowing"), "Overflowing", 0, 3, 0, 1, 1,
                java.util.Set.of(Material.WATER_BUCKET), java.util.Optional.of(new OverflowingHandler())));
        registry.register(new EnchantmentDefinition(
                CustomEnchantmentSupport.customKey("vacuum"), "Vacuum", 0, 3, 0, 1, 1,
                java.util.Set.of(Material.BUCKET), java.util.Optional.of(new VacuumHandler())));
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        ArrayList<Runnable> scheduled = new ArrayList<>();
        CustomEnchantmentListener listener = new CustomEnchantmentListener(dispatcher, scheduled::add);

        Player player = org.mockito.Mockito.mock(Player.class);
        PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        java.util.concurrent.atomic.AtomicReference<ItemStack> offhand =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(inventory.getItem(EquipmentSlot.OFF_HAND)).thenAnswer(invocation -> offhand.get());
        org.mockito.Mockito.doAnswer(invocation -> {
                    offhand.set(invocation.getArgument(1));
                    return null;
                })
                .when(inventory)
                .setItem(eq(EquipmentSlot.OFF_HAND), any(ItemStack.class));

        ItemStack emptyInput = org.mockito.Mockito.mock(ItemStack.class);
        ItemStack emptyMain = emptyInput;
        ItemStack emptyOutput = org.mockito.Mockito.mock(ItemStack.class);
        when(emptyInput.getType()).thenReturn(Material.WATER_BUCKET);
        when(emptyOutput.getType()).thenReturn(Material.BUCKET);
        offhand.set(emptyInput);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(emptyMain);
        when(adapter.readOvercap(emptyInput))
                .thenReturn(Map.of(CustomEnchantmentSupport.customKey("overflowing"), 1));
        PlayerBucketEmptyEvent emptyEvent = org.mockito.Mockito.mock(PlayerBucketEmptyEvent.class);
        when(emptyEvent.getPlayer()).thenReturn(player);
        when(emptyEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(emptyEvent.getBlockFace()).thenReturn(org.bukkit.block.BlockFace.UP);
        when(emptyEvent.getItemStack()).thenReturn(emptyOutput);
        listener.onBucketEmpty(emptyEvent);
        verify(adapter).readOvercap(emptyInput);
        verify(emptyEvent, never()).getItemStack();
        offhand.set(emptyOutput);
        listener.onBucketEmptyFinal(emptyEvent);
        scheduled.remove(0).run();
        assertEquals(Material.WATER_BUCKET, offhand.get().getType());
        org.junit.jupiter.api.Assertions.assertSame(emptyInput, offhand.get());
        verify(inventory, never()).setItem(eq(EquipmentSlot.HAND), any(ItemStack.class));
        assertEquals(emptyInput, emptyMain);

        ItemStack fillInput = org.mockito.Mockito.mock(ItemStack.class);
        ItemStack fillMain = fillInput;
        ItemStack fillOutput = org.mockito.Mockito.mock(ItemStack.class);
        when(fillInput.getType()).thenReturn(Material.BUCKET);
        when(fillOutput.getType()).thenReturn(Material.WATER_BUCKET);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(fillMain);
        offhand.set(fillInput);
        when(adapter.readOvercap(fillInput))
                .thenReturn(Map.of(CustomEnchantmentSupport.customKey("vacuum"), 1));
        PlayerBucketFillEvent fillEvent = org.mockito.Mockito.mock(PlayerBucketFillEvent.class);
        when(fillEvent.getPlayer()).thenReturn(player);
        when(fillEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);
        when(fillEvent.getBlockFace()).thenReturn(org.bukkit.block.BlockFace.UP);
        when(fillEvent.getItemStack()).thenReturn(fillOutput);
        listener.onBucketFill(fillEvent);
        verify(adapter).readOvercap(fillInput);
        verify(fillEvent, never()).getItemStack();
        offhand.set(fillOutput);
        listener.onBucketFillFinal(fillEvent);
        scheduled.remove(0).run();
        assertEquals(Material.BUCKET, offhand.get().getType());
        org.junit.jupiter.api.Assertions.assertSame(fillInput, offhand.get());
        verify(inventory, never()).setItem(eq(EquipmentSlot.HAND), any(ItemStack.class));
        assertEquals(fillInput, fillMain);
    }

    private static void assertBucketRestored(EquipmentSlot hand, Object handler, Material source) {
        Player player = org.mockito.Mockito.mock(Player.class);
        PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        ItemStack bucket = org.mockito.Mockito.mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);
        when(bucket.getType()).thenReturn(source);

        if (handler instanceof OverflowingHandler overflowing) {
            overflowing.onBucketEmpty(player, null, null, bucket, hand, 1);
        } else {
            ((VacuumHandler) handler).onBucketFill(player, null, null, bucket, hand, 1);
        }

        Material expected = handler instanceof OverflowingHandler ? Material.WATER_BUCKET : Material.BUCKET;
        verify(bucket).setType(expected);
        verify(inventory).setItem(hand, bucket);
    }
}
