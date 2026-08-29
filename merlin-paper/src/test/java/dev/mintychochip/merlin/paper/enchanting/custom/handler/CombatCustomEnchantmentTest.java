package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import dev.mintychochip.merlin.paper.enchanting.custom.MutableExperience;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

final class CombatCustomEnchantmentTest {
    @Test
    void sharedSupportProvidesKeysRollsNetherFilterHeadsAndSafeHealing() {
        assertEquals(new NamespacedKey("merlin", "equilibrium"),
                CustomEnchantmentSupport.customKey("equilibrium"));
        assertEquals(11, CustomEnchantmentSupport.randomPerRank(new SequenceRandom(1, 0, 1), 3, 4, 3));

        LivingEntity blaze = mock(LivingEntity.class);
        when(blaze.getType()).thenReturn(EntityType.BLAZE);
        LivingEntity zombie = mock(LivingEntity.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        assertTrue(CustomEnchantmentSupport.isNativeNetherMob(blaze));
        assertFalse(CustomEnchantmentSupport.isNativeNetherMob(zombie));

        assertEquals(Material.CREEPER_HEAD, CustomEnchantmentSupport.mobHeadMaterial(EntityType.CREEPER));
        assertEquals(Material.ZOMBIE_HEAD, CustomEnchantmentSupport.mobHeadMaterial(EntityType.ZOMBIE));
        assertEquals(Material.SKELETON_SKULL, CustomEnchantmentSupport.mobHeadMaterial(EntityType.SKELETON));
        assertEquals(Material.WITHER_SKELETON_SKULL,
                CustomEnchantmentSupport.mobHeadMaterial(EntityType.WITHER_SKELETON));
        assertNull(CustomEnchantmentSupport.mobHeadMaterial(EntityType.BLAZE));

        AttributeInstance maxHealth = mock(AttributeInstance.class);
        LivingEntity player = mock(LivingEntity.class);
        when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(maxHealth);
        when(maxHealth.getValue()).thenReturn(20.0);
        when(player.getHealth()).thenReturn(19.5);
        CustomEnchantmentSupport.healToMax(player, 1.0);
        verify(player).setHealth(20.0);
    }

    @Test
    void equilibriumAddsDamageAndGuardedHalfSelfDamage() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(5.0);
        EquilibriumHandler handler = new EquilibriumHandler(new SequenceRandom(0));
        doAnswer(invocation -> {
            handler.onEntityHit(attacker, victim, damage, 1);
            return null;
        }).when(attacker).damage(0.5);

        handler.onEntityHit(attacker, victim, damage, 1);

        assertEquals(1.0, damage.getBonusDamage());
        assertEquals(6.0, damage.getFinalDamage());
        verify(attacker).damage(0.5);
    }

    @Test
    void equilibriumIgnoresNullContextsAndNonPositiveLevels() {
        EquilibriumHandler handler = new EquilibriumHandler(new SequenceRandom(0));
        MutableDamage damage = new MutableDamage(5.0);
        LivingEntity victim = mock(LivingEntity.class);

        handler.onEntityHit(null, victim, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, null, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, damage, 0);

        assertEquals(0.0, damage.getBonusDamage());
    }

    @Test
    void netherScourgeAddsDamageOnlyForNativeNetherHostiles() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity blaze = mock(LivingEntity.class);
        when(blaze.getType()).thenReturn(EntityType.BLAZE);
        LivingEntity zombie = mock(LivingEntity.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        MutableDamage damage = new MutableDamage(5.0);
        NetherScourgeHandler handler = new NetherScourgeHandler(new SequenceRandom(2, 0));

        handler.onEntityHit(attacker, blaze, damage, 2);
        assertEquals(4.0, damage.getBonusDamage());

        handler.onEntityHit(attacker, zombie, damage, 2);
        assertEquals(4.0, damage.getBonusDamage());
    }

    @Test
    void netherScourgeIgnoresNullContextsAndNonPositiveLevels() {
        NetherScourgeHandler handler = new NetherScourgeHandler(new SequenceRandom(0));
        MutableDamage damage = new MutableDamage(5.0);
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.getType()).thenReturn(EntityType.BLAZE);

        handler.onEntityHit(null, victim, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), null, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, damage, 0);

        assertEquals(0.0, damage.getBonusDamage());
    }

    @Test
    void coldAspectRaisesFreezeTicksWithoutReducingStrongerDuration() {
        LivingEntity victim = mock(LivingEntity.class);
        ColdAspectHandler handler = new ColdAspectHandler();
        when(victim.getFreezeTicks()).thenReturn(5, 100);

        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);
        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);

        verify(victim).setFreezeTicks(40);
        verify(victim, never()).setFreezeTicks(100);
    }

    @Test
    void coldAspectIgnoresNullContextsAndNonPositiveLevels() {
        ColdAspectHandler handler = new ColdAspectHandler();
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(1.0);

        handler.onEntityHit(null, victim, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), null, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, null, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, damage, 0);

        verify(victim, never()).setFreezeTicks(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void confusingAspectAppliesNauseaForRankDurationAndPreservesLongerEffect() {
        LivingEntity victim = mock(LivingEntity.class);
        ConfusingAspectHandler handler = new ConfusingAspectHandler();
        when(victim.getPotionEffect(PotionEffectType.NAUSEA))
                .thenReturn(null, new PotionEffect(PotionEffectType.NAUSEA, 400, 0));

        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);
        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);

        verify(victim).addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0));
    }

    @Test
    void confusingAspectIgnoresNullContextsAndNonPositiveLevels() {
        ConfusingAspectHandler handler = new ConfusingAspectHandler();
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(1.0);

        handler.onEntityHit(null, victim, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, damage, 0);

        verify(victim, never()).addPotionEffect(org.mockito.ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void toxinAspectAppliesPoisonForRankDurationAndPreservesLongerEffect() {
        LivingEntity victim = mock(LivingEntity.class);
        ToxinAspectHandler handler = new ToxinAspectHandler();
        when(victim.getPotionEffect(PotionEffectType.POISON))
                .thenReturn(null, new PotionEffect(PotionEffectType.POISON, 400, 0));

        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);
        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);

        verify(victim).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 160, 0));
    }

    @Test
    void toxinAspectIgnoresNullContextsAndNonPositiveLevels() {
        ToxinAspectHandler handler = new ToxinAspectHandler();
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(1.0);

        handler.onEntityHit(null, victim, damage, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, damage, 0);

        verify(victim, never()).addPotionEffect(org.mockito.ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void vampirismHealsExactlyOneHealthPointAndCapsAtMaxHealth() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        when(attacker.getAttribute(Attribute.MAX_HEALTH)).thenReturn(maxHealth);
        when(maxHealth.getValue()).thenReturn(20.0);
        when(attacker.getHealth()).thenReturn(19.5);

        VampirismHandler handler = new VampirismHandler(new SequenceRandom(0.0));
        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);

        verify(attacker).setHealth(20.0);
    }

    @Test
    void vampirismIgnoresFailedChanceNullContextsAndNonPositiveLevels() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        VampirismHandler handler = new VampirismHandler(new SequenceRandom(0.10));

        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        handler.onEntityHit(null, victim, new MutableDamage(1.0), 1);
        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 0);

        verify(attacker, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void knowledgeAddsOneBoundedRollPerRankToDeathExperience() {
        LivingEntity killer = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        List<ItemStack> drops = List.of(drop(Material.ROTTEN_FLESH));
        MutableExperience experience = new MutableExperience(5);
        KnowledgeHandler handler = new KnowledgeHandler(new SequenceRandom(1, 0, 1));

        handler.onEntityKill(killer, victim, drops, experience, 3);

        assertEquals(16, experience.getAmount());
    }

    @Test
    void knowledgeIgnoresNullContextsEmptyDropsAndNonPositiveLevels() {
        KnowledgeHandler handler = new KnowledgeHandler(new SequenceRandom(0));
        MutableExperience experience = new MutableExperience(5);
        LivingEntity victim = mock(LivingEntity.class);

        handler.onEntityKill(null, victim, List.of(drop(Material.ROTTEN_FLESH)), experience, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, List.of(), experience, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, null, experience, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, List.of(drop(Material.ROTTEN_FLESH)), experience, 0);

        assertEquals(5, experience.getAmount());
    }

    @Test
    void vorpalAddsMatchingHeadOnlyWhenChanceSucceeds() {
        LivingEntity killer = mock(LivingEntity.class);
        LivingEntity creeper = mock(LivingEntity.class);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        List<ItemStack> drops = new ArrayList<>(List.of(drop(Material.ROTTEN_FLESH)));
        VorpalHandler handler = new VorpalHandler(new SequenceRandom(0.0));

        try (MockedConstruction<ItemStack> ignored = mockConstruction(ItemStack.class,
                (item, context) -> when(item.getType()).thenReturn((Material) context.arguments().get(0)))) {
            handler.onEntityKill(killer, creeper, drops, new MutableExperience(1), 1);
            assertEquals(2, drops.size());
            assertEquals(Material.CREEPER_HEAD, drops.get(1).getType());
        }
    }

    @Test
    void vorpalMapsAllSupportedHeadsAndIgnoresUnsupportedOrFailedChance() {
        LivingEntity killer = mock(LivingEntity.class);
        try (MockedConstruction<ItemStack> ignored = mockConstruction(ItemStack.class,
                (item, context) -> when(item.getType()).thenReturn((Material) context.arguments().get(0)))) {
            for (var pair : List.of(
                    List.of(EntityType.ZOMBIE, Material.ZOMBIE_HEAD),
                    List.of(EntityType.SKELETON, Material.SKELETON_SKULL),
                    List.of(EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL))) {
                LivingEntity victim = mock(LivingEntity.class);
                when(victim.getType()).thenReturn((EntityType) pair.get(0));
                List<ItemStack> drops = new ArrayList<>(List.of(drop(Material.ROTTEN_FLESH)));
                new VorpalHandler(new SequenceRandom(0.0)).onEntityKill(
                        killer, victim, drops, new MutableExperience(1), 1);
                assertEquals((Material) pair.get(1), drops.get(1).getType());
            }

            LivingEntity unsupported = mock(LivingEntity.class);
            when(unsupported.getType()).thenReturn(EntityType.BLAZE);
            List<ItemStack> unsupportedDrops = new ArrayList<>(List.of(drop(Material.ROTTEN_FLESH)));
            new VorpalHandler(new SequenceRandom(0.0)).onEntityKill(
                    killer, unsupported, unsupportedDrops, new MutableExperience(1), 1);
            assertEquals(1, unsupportedDrops.size());

            LivingEntity zombie = mock(LivingEntity.class);
            when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
            List<ItemStack> failedDrops = new ArrayList<>(List.of(drop(Material.ROTTEN_FLESH)));
            new VorpalHandler(new SequenceRandom(0.005)).onEntityKill(
                    killer, zombie, failedDrops, new MutableExperience(1), 1);
            assertEquals(1, failedDrops.size());
        }
    }

    @Test
    void vorpalIgnoresNullContextsEmptyDropsAndNonPositiveLevels() {
        VorpalHandler handler = new VorpalHandler(new SequenceRandom(0.0));
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.getType()).thenReturn(EntityType.ZOMBIE);
        List<ItemStack> drops = new ArrayList<>(List.of(drop(Material.ROTTEN_FLESH)));

        handler.onEntityKill(null, victim, drops, new MutableExperience(1), 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, List.of(), new MutableExperience(1), 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, drops, new MutableExperience(1), 0);

        assertEquals(1, drops.size());
    }

    @Test
    void plunderAppendsClonedCopiesForEveryRank() {
        LivingEntity killer = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        ItemStack first = drop(Material.ROTTEN_FLESH);
        ItemStack second = drop(Material.BONE);
        ItemStack firstCopy = drop(Material.ROTTEN_FLESH);
        ItemStack secondCopy = drop(Material.BONE);
        when(first.clone()).thenReturn(firstCopy);
        when(second.clone()).thenReturn(secondCopy);
        List<ItemStack> drops = new ArrayList<>(List.of(first, second));

        new PlunderHandler().onEntityKill(killer, victim, drops, new MutableExperience(0), 2);

        assertEquals(6, drops.size());
        assertSame(first, drops.get(0));
        assertSame(second, drops.get(1));
        assertNotSame(first, drops.get(2));
        assertNotSame(second, drops.get(3));
        assertSame(firstCopy, drops.get(2));
        assertSame(secondCopy, drops.get(3));
        assertSame(firstCopy, drops.get(4));
        assertSame(secondCopy, drops.get(5));
    }

    @Test
    void plunderIgnoresNullContextsEmptyDropsAndNonPositiveLevels() {
        PlunderHandler handler = new PlunderHandler();
        LivingEntity victim = mock(LivingEntity.class);
        List<ItemStack> drops = new ArrayList<>(List.of(drop(Material.BONE)));

        handler.onEntityKill(null, victim, drops, new MutableExperience(0), 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, List.of(), new MutableExperience(0), 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, drops, null, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, drops, new MutableExperience(0), 0);

        assertEquals(1, drops.size());
    }

    @Test
    void wisdomAddsOriginalExperienceOncePerRankAndLeavesZeroXpAlone() {
        LivingEntity killer = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        List<ItemStack> drops = List.of(drop(Material.BONE));
        WisdomHandler handler = new WisdomHandler();
        MutableExperience experience = new MutableExperience(4);

        handler.onEntityKill(killer, victim, drops, experience, 3);
        assertEquals(16, experience.getAmount());

        MutableExperience zero = new MutableExperience(0);
        handler.onEntityKill(killer, victim, drops, zero, 3);
        assertEquals(0, zero.getAmount());
    }

    @Test
    void wisdomIgnoresNullContextsEmptyDropsAndNonPositiveLevels() {
        WisdomHandler handler = new WisdomHandler();
        LivingEntity victim = mock(LivingEntity.class);
        MutableExperience experience = new MutableExperience(5);
        List<ItemStack> drops = new ArrayList<>(List.of(drop(Material.BONE)));

        handler.onEntityKill(null, victim, drops, experience, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, List.of(), experience, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, drops, null, 1);
        handler.onEntityKill(mock(LivingEntity.class), victim, drops, experience, 0);

        assertEquals(5, experience.getAmount());
    }

    private static ItemStack drop(Material material) {
        ItemStack drop = mock(ItemStack.class);
        when(drop.getType()).thenReturn(material);
        when(drop.isEmpty()).thenReturn(false);
        return drop;
    }

    private static final class SequenceRandom extends Random {
        private final int[] ints;
        private final double[] doubles;
        private int intIndex;
        private int doubleIndex;

        private SequenceRandom(int... ints) {
            this.ints = ints;
            this.doubles = new double[0];
        }

        private SequenceRandom(double... doubles) {
            this.ints = new int[0];
            this.doubles = doubles;
        }

        @Override
        public int nextInt(int bound) {
            return ints[Math.min(intIndex++, ints.length - 1)];
        }

        @Override
        public double nextDouble() {
            return doubles[Math.min(doubleIndex++, doubles.length - 1)];
        }
    }
}
