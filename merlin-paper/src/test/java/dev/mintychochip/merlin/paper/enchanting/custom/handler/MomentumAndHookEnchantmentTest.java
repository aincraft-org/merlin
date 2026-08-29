package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

final class MomentumAndHookEnchantmentTest {

    @Test
    void momentumBoostsGlidingPlayerVelocityPerLevel() {
        Player player = mock(Player.class);
        when(player.isGliding()).thenReturn(true);
        when(player.getVelocity()).thenReturn(new Vector(1, 0, 0));
        org.bukkit.inventory.ItemStack elytra = mock(org.bukkit.inventory.ItemStack.class);

        new MomentumHandler().onFireworkBoost(player, player, elytra, 2);

        verify(player).setVelocity(ArgumentMatchers.argThat(v -> Math.abs(v.getX() - 2.0) < 1e-9));
    }

    @Test
    void momentumIgnoresNonGlidingAndWrongShooter() {
        Player player = mock(Player.class);
        when(player.isGliding()).thenReturn(false);
        org.bukkit.inventory.ItemStack elytra = mock(org.bukkit.inventory.ItemStack.class);
        new MomentumHandler().onFireworkBoost(player, player, elytra, 1);
        verify(player, never()).setVelocity(ArgumentMatchers.any());

        Player other = mock(Player.class);
        new MomentumHandler().onFireworkBoost(player, other, elytra, 1);
        verify(player, never()).setVelocity(ArgumentMatchers.any());
    }

    @Test
    void fireHookSetsTargetOnFire() {
        Player player = mock(Player.class);
        LivingEntity hit = mock(LivingEntity.class);
        when(hit.isDead()).thenReturn(false);

        new FireHookHandler().onHookContact(player, hit, 2);
        verify(hit).setFireTicks(160);
    }

    @Test
    void fireHookIgnoresDeadAndNull() {
        LivingEntity dead = mock(LivingEntity.class);
        when(dead.isDead()).thenReturn(true);
        new FireHookHandler().onHookContact(mock(Player.class), dead, 1);
        verify(dead, never()).setFireTicks(ArgumentMatchers.anyInt());
    }

    @Test
    void poisonedHookAppliesPoisonForRankDuration() {
        Player player = mock(Player.class);
        LivingEntity hit = mock(LivingEntity.class);
        when(hit.isDead()).thenReturn(false);
        when(hit.getPotionEffect(PotionEffectType.POISON)).thenReturn(null);

        new PoisonedHookHandler().onHookContact(player, hit, 2);
        verify(hit).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 160, 1));
    }

    @Test
    void poisonedHookPreservesStrongerPoison() {
        LivingEntity hit = mock(LivingEntity.class);
        when(hit.isDead()).thenReturn(false);
        when(hit.getPotionEffect(PotionEffectType.POISON))
                .thenReturn(new PotionEffect(PotionEffectType.POISON, 200, 1));

        new PoisonedHookHandler().onHookContact(mock(Player.class), hit, 2);
        verify(hit, never()).addPotionEffect(ArgumentMatchers.any(PotionEffect.class));
    }
}