package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BowShootTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitByEntityTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityKillTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityMoveTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityItemDamageTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentDispatcherTest {
    interface TestHitHandler extends OvercapEffectHandler, EntityHitTrigger {}
    interface TestBreakHandler extends OvercapEffectHandler, BlockBreakTrigger {}
    interface TestEntityMoveHandler extends OvercapEffectHandler, EntityMoveTrigger {}
    interface TestHitByEntityHandler extends OvercapEffectHandler, EntityHitByEntityTrigger {}
    interface TestEntityItemDamageHandler extends OvercapEffectHandler, EntityItemDamageTrigger {}
    interface TestBowHandler extends OvercapEffectHandler, BowShootTrigger {}
    interface TestKillHandler extends OvercapEffectHandler, EntityKillTrigger {}

    @Test
    void dispatchesEntityHitToRegisteredTriggers() {
        NamespacedKey key = new NamespacedKey("merlin", "test_vampirism");
        TestHitHandler handler = mock(TestHitHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Vampirism", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack sword = mock(ItemStack.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage dmg = new MutableDamage(10.0);

        dispatcher.dispatchEntityHit(attacker, victim, dmg, sword);
        verify(handler).onEntityHit(attacker, victim, dmg, 2);
    }

    @Test
    void dispatchesEntityHitByEntityToVictimEquipment() {
        NamespacedKey key = new NamespacedKey("merlin", "test-defense");
        TestHitByEntityHandler handler = mock(TestHitByEntityHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Defense", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_HELMET), Optional.of(handler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack helmet = mock(ItemStack.class);
        when(adapter.readOvercap(helmet)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        LivingEntity victim = mock(LivingEntity.class);
        Entity attacker = mock(Entity.class);
        MutableDamage damage = new MutableDamage(8.0);

        dispatcher.dispatchEntityHitByEntity(victim, attacker, damage, new ItemStack[]{helmet});

        verify(handler).onEntityHitByEntity(victim, attacker, damage, 2);
    }

    @Test
    void dispatchesEntityItemDamageAndReturnsModifiedAmount() {
        NamespacedKey key = new NamespacedKey("merlin", "test-entity-item-damage");
        TestEntityItemDamageHandler handler = mock(TestEntityItemDamageHandler.class);
        when(handler.key()).thenReturn(key);
        when(handler.onEntityItemDamage(any(Entity.class), any(ItemStack.class), eq(4), eq(2))).thenReturn(7);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Entity Item Damage", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_HELMET), Optional.of(handler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack item = mock(ItemStack.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        Entity entity = mock(Entity.class);

        assertEquals(7, dispatcher.dispatchEntityItemDamage(entity, item, 4));

        verify(handler).onEntityItemDamage(entity, item, 4, 2);
    }
    @Test
    void dispatchesBowShootForAnyLivingShooter() {
        NamespacedKey key = new NamespacedKey("merlin", "test-bow");
        TestBowHandler handler = mock(TestBowHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Bow", 0, 3, 10, 5, 10,
                Set.of(Material.BOW), Optional.of(handler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack bow = mock(ItemStack.class);
        when(adapter.readOvercap(bow)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        LivingEntity shooter = mock(LivingEntity.class);
        Entity projectile = mock(Entity.class);

        dispatcher.dispatchBowShoot(shooter, projectile, bow, 1.0f);

        verify(handler).onBowShoot(shooter, projectile, bow, 1.0f, 2);
    }

    @Test
    void dispatchesEntityKillForAnyLivingKiller() {
        NamespacedKey key = new NamespacedKey("merlin", "test-kill");
        TestKillHandler handler = mock(TestKillHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Kill", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_SWORD), Optional.of(handler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack weapon = mock(ItemStack.class);
        when(adapter.readOvercap(weapon)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        LivingEntity killer = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        List<ItemStack> drops = new ArrayList<>();
        MutableExperience experience = new MutableExperience(5);
        dispatcher.dispatchEntityKill(killer, victim, drops, experience, weapon);

        verify(handler).onEntityKill(killer, victim, drops, experience, 2);
    }

    @Test
    void dispatchesEntityMoveForLivingEntity() {
        NamespacedKey key = new NamespacedKey("merlin", "test-move");
        TestEntityMoveHandler handler = mock(TestEntityMoveHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Move", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack item = mock(ItemStack.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 1));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);
        LivingEntity entity = mock(LivingEntity.class);
        Location from = mock(Location.class);
        Location to = mock(Location.class);

        dispatcher.dispatchEntityMove(entity, from, to, new ItemStack[]{item});

        verify(handler).onEntityMove(entity, from, to, 1);
    }

    @Test
    void dispatchesBlockBreakToRegisteredTriggers() {
        NamespacedKey key = new NamespacedKey("merlin", "test_vein_miner");
        TestBreakHandler handler = mock(TestBreakHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Vein Miner", 0, 3, 20, 8, 5, Set.of(Material.DIAMOND_PICKAXE), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack pickaxe = mock(ItemStack.class);
        when(adapter.readOvercap(pickaxe)).thenReturn(Map.of(key, 3));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        Player player = mock(Player.class);
        Block block = mock(Block.class);
        CascadeScope scope = mock(CascadeScope.class);

        dispatcher.dispatchBlockBreak(player, block, pickaxe, scope);
        verify(handler).onBlockBreak(player, block, 3, scope);
    }

    @Test
    void executesTriggersInDescendingPriorityOrder() {
        NamespacedKey lowKey = new NamespacedKey("merlin", "low_priority");
        NamespacedKey highKey = new NamespacedKey("merlin", "high_priority");

        List<String> executionOrder = new ArrayList<>();

        TestHitHandler lowHandler = mock(TestHitHandler.class);
        when(lowHandler.key()).thenReturn(lowKey);
        when(lowHandler.priority()).thenReturn(10);
        org.mockito.Mockito.doAnswer(inv -> {
            executionOrder.add("low");
            return null;
        }).when(lowHandler).onEntityHit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());

        TestHitHandler highHandler = mock(TestHitHandler.class);
        when(highHandler.key()).thenReturn(highKey);
        when(highHandler.priority()).thenReturn(100);
        org.mockito.Mockito.doAnswer(inv -> {
            executionOrder.add("high");
            return null;
        }).when(highHandler).onEntityHit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(lowKey, "Low", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(lowHandler)));
        registry.register(new EnchantmentDefinition(highKey, "High", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(highHandler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack sword = mock(ItemStack.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(lowKey, 1, highKey, 1));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        Player attacker = mock(Player.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage dmg = new MutableDamage(10.0);

        dispatcher.dispatchEntityHit(attacker, victim, dmg, sword);
        assertEquals(List.of("high", "low"), executionOrder);
    }
}
