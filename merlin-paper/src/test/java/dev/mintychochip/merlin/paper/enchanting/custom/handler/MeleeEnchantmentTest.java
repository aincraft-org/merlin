package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

final class MeleeEnchantmentTest {

    private static LivingEntity context(PotionEffectType type, PotionEffect existing) {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.getPotionEffect(type)).thenReturn(existing);
        return victim;
    }

    private static void assertApplied(LivingEntity entity, PotionEffectType type, int ticks, int amplifier) {
        verify(entity).addPotionEffect(new PotionEffect(type, ticks, amplifier));
    }

    @Test
    void bleedAppliesWitherForRankDuration() {
        LivingEntity victim = context(PotionEffectType.WITHER, null);
        new BleedHandler().onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 2);

        assertApplied(victim, PotionEffectType.WITHER, 160, 1);
    }

    @Test
    void bleedPreservesStrongerWitherAndInfinite() {
        LivingEntity stronger = context(PotionEffectType.WITHER, new PotionEffect(PotionEffectType.WITHER, 200, 1));
        new BleedHandler().onEntityHit(mock(LivingEntity.class), stronger, new MutableDamage(1.0), 2);
        verify(stronger, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));

        LivingEntity infinite = context(PotionEffectType.WITHER,
                new PotionEffect(PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION, 0));
        new BleedHandler().onEntityHit(mock(LivingEntity.class), infinite, new MutableDamage(1.0), 2);
        verify(infinite, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void bleedIgnoresNullAndCancelledAndNonPositiveLevels() {
        LivingEntity victim = context(PotionEffectType.WITHER, null);
        BleedHandler handler = new BleedHandler();

        handler.onEntityHit(null, victim, new MutableDamage(1.0), 1);
        MutableDamage cancelled = new MutableDamage(1.0);
        cancelled.setCancelled(true);
        handler.onEntityHit(mock(LivingEntity.class), victim, cancelled, 1);
        handler.onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 0);

        verify(victim, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void blindAppliesBlindnessForRankDuration() {
        LivingEntity victim = context(PotionEffectType.BLINDNESS, null);
        new BlindHandler().onEntityHit(mock(LivingEntity.class), victim, new MutableDamage(1.0), 3);

        assertApplied(victim, PotionEffectType.BLINDNESS, 300, 2);
    }

    @Test
    void blindPreservesStrongerEffectAndIgnoresCancelled() {
        LivingEntity stronger = context(PotionEffectType.BLINDNESS, new PotionEffect(PotionEffectType.BLINDNESS, 400, 3));
        new BlindHandler().onEntityHit(mock(LivingEntity.class), stronger, new MutableDamage(1.0), 1);
        verify(stronger, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));

        LivingEntity victim = context(PotionEffectType.BLINDNESS, null);
        MutableDamage cancelled = new MutableDamage(1.0);
        cancelled.setCancelled(true);
        new BlindHandler().onEntityHit(mock(LivingEntity.class), victim, cancelled, 1);
        verify(victim, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void blockNegatesDamageWhenChanceSucceeds() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(10.0);

        new BlockHandler(new ChancedRandom(0.0)).onEntityHit(attacker, victim, damage, 2);

        assertEquals(0.0, damage.getFinalDamage());
        assertTrue(damage.getFinalDamage() == 0.0);
    }

    @Test
    void blockLeavesDamageWhenChanceMissesAndIgnoresCancelled() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage damage = new MutableDamage(10.0);

        new BlockHandler(new ChancedRandom(0.5)).onEntityHit(attacker, victim, damage, 2);
        assertEquals(10.0, damage.getFinalDamage());

        MutableDamage cancelled = new MutableDamage(10.0);
        cancelled.setCancelled(true);
        new BlockHandler(new ChancedRandom(0.0)).onEntityHit(attacker, victim, cancelled, 2);
        assertTrue(cancelled.isCancelled());
    }

    @Test
    void berserkAppliesStrengthAndMiningFatigue() {
        LivingEntity attacker = mock(LivingEntity.class);
        new BerserkHandler().onEntityHit(attacker, mock(LivingEntity.class), new MutableDamage(1.0), 2);

        assertApplied(attacker, PotionEffectType.STRENGTH, 120, 1);
        assertApplied(attacker, PotionEffectType.MINING_FATIGUE, 120, 1);
    }

    @Test
    void berserkClampsAmplifierAndIgnoresNull() {
        LivingEntity attacker = mock(LivingEntity.class);
        new BerserkHandler().onEntityHit(attacker, mock(LivingEntity.class), new MutableDamage(1.0), 10);
        assertApplied(attacker, PotionEffectType.STRENGTH, 600, 4);

        LivingEntity fresh = mock(LivingEntity.class);
        new BerserkHandler().onEntityHit(null, fresh, new MutableDamage(1.0), 1);
        verify(fresh, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void criticalMultipliesOnlyWhenAttackerIsAirborne() {
        LivingEntity grounded = mock(LivingEntity.class);
        when(grounded.isOnGround()).thenReturn(true);
        MutableDamage groundDamage = new MutableDamage(10.0);
        new CriticalHandler().onEntityHit(grounded, mock(LivingEntity.class), groundDamage, 2);
        assertEquals(10.0, groundDamage.getFinalDamage());

        LivingEntity airborne = mock(LivingEntity.class);
        when(airborne.isOnGround()).thenReturn(false);
        MutableDamage airDamage = new MutableDamage(10.0);
        new CriticalHandler().onEntityHit(airborne, mock(LivingEntity.class), airDamage, 2);
        assertEquals(12.0, airDamage.getFinalDamage());
    }

    @Test
    void criticalIgnoresNullAndCancelled() {
        MutableDamage cancelled = new MutableDamage(10.0);
        cancelled.setCancelled(true);
        LivingEntity airborne = mock(LivingEntity.class);
        when(airborne.isOnGround()).thenReturn(false);
        new CriticalHandler().onEntityHit(airborne, mock(LivingEntity.class), cancelled, 2);
        assertTrue(cancelled.isCancelled());

        new CriticalHandler().onEntityHit(null, mock(LivingEntity.class), new MutableDamage(1.0), 1);
    }

    @Test
    void doubleStrikeDealsInitialDamageTimesLevel() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.isDead()).thenReturn(false);

        new DoubleStrikeHandler().onEntityHit(attacker, victim, new MutableDamage(4.0), 3);

        verify(victim).damage(12.0, attacker);
    }

    @Test
    void doubleStrikeSkipsCancelledDeadAndNonPositiveLevels() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.isDead()).thenReturn(true);

        new DoubleStrikeHandler().onEntityHit(attacker, victim, new MutableDamage(4.0), 1);
        verify(victim, never()).damage(ArgumentMatchers.anyDouble(), (org.bukkit.entity.Entity) ArgumentMatchers.any());

        MutableDamage cancelled = new MutableDamage(4.0);
        cancelled.setCancelled(true);
        LivingEntity alive = mock(LivingEntity.class);
        when(alive.isDead()).thenReturn(false);
        new DoubleStrikeHandler().onEntityHit(attacker, alive, cancelled, 1);
        verify(alive, never()).damage(ArgumentMatchers.anyDouble(), (org.bukkit.entity.Entity) ArgumentMatchers.any());

        new DoubleStrikeHandler().onEntityHit(attacker, alive, new MutableDamage(4.0), 0);
        verify(alive, never()).damage(ArgumentMatchers.anyDouble(), (org.bukkit.entity.Entity) ArgumentMatchers.any());
    }

    @Test
    void thunderlordStrikesLightningOnEveryThirdHit() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        when(victim.getWorld()).thenReturn(world);
        when(victim.getLocation()).thenReturn(location);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        ThunderlordHandler handler = new ThunderlordHandler();

        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world, never()).strikeLightning(ArgumentMatchers.any(Location.class));

        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world, never()).strikeLightning(ArgumentMatchers.any(Location.class));

        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world).strikeLightning(location);
    }

    @Test
    void thunderlordResetsCounterAfterStrikeAndIgnoresNullContexts() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        when(victim.getWorld()).thenReturn(world);
        when(victim.getLocation()).thenReturn(location);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        ThunderlordHandler handler = new ThunderlordHandler();

        for (int i = 0; i < 3; i++) handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world, org.mockito.Mockito.times(1)).strikeLightning(location);

        for (int i = 0; i < 2; i++) handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world, org.mockito.Mockito.times(1)).strikeLightning(location);

        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 1);
        verify(world, org.mockito.Mockito.times(2)).strikeLightning(location);
    }

    @Test
    void thunderlordIgnoresCancelledAndNonPositiveLevels() {
        LivingEntity attacker = mock(LivingEntity.class);
        LivingEntity victim = mock(LivingEntity.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        when(victim.getWorld()).thenReturn(world);
        when(victim.getLocation()).thenReturn(location);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        ThunderlordHandler handler = new ThunderlordHandler();

        MutableDamage cancelled = new MutableDamage(1.0);
        cancelled.setCancelled(true);
        handler.onEntityHit(attacker, victim, cancelled, 1);
        handler.onEntityHit(attacker, victim, new MutableDamage(1.0), 0);
        verify(world, never()).strikeLightning(ArgumentMatchers.any(Location.class));
    }

    private static final class ChancedRandom extends java.util.Random {
        private final double value;
        private ChancedRandom(double value) { this.value = value; }
        @Override
        public double nextDouble() { return value; }
    }
}