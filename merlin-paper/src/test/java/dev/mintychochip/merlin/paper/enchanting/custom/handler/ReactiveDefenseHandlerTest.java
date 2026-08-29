package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.junit.jupiter.api.Test;

final class ReactiveDefenseHandlerTest {
    @Test
    void moltenSetsLivingAttackerOnFireForAtLeastLevelDuration() {
        LivingEntity attacker = mock(LivingEntity.class);
        when(attacker.getFireTicks()).thenReturn(10);

        new MoltenHandler().onArmorDefense(mock(Player.class), attacker, new MutableDamage(4.0), 2);

        verify(attacker).setFireTicks(120);
    }

    @Test
    void moltenPreservesLongerExistingFire() {
        LivingEntity attacker = mock(LivingEntity.class);
        when(attacker.getFireTicks()).thenReturn(200);

        new MoltenHandler().onArmorDefense(mock(Player.class), attacker, new MutableDamage(4.0), 1);

        verify(attacker).setFireTicks(200);
    }

    @Test
    void reflectDealsTenPercentOfInitialDamagePerLevel() {
        Player defender = mock(Player.class);
        LivingEntity attacker = mock(LivingEntity.class);

        new ReflectHandler().onArmorDefense(defender, attacker, new MutableDamage(20.0), 2);

        verify(attacker).damage(4.0, defender);
    }

    @Test
    void reflectResolvesProjectileShooter() {
        Player defender = mock(Player.class);
        Projectile projectile = mock(Projectile.class);
        LivingEntity shooter = mock(LivingEntity.class);
        when(projectile.getShooter()).thenReturn(shooter);

        new ReflectHandler().onArmorDefense(defender, projectile, new MutableDamage(10.0), 1);

        verify(shooter).damage(1.0, defender);
    }

    @Test
    void reactiveHandlersIgnoreCancelledDamageAndNonLivingAttackers() {
        Player defender = mock(Player.class);
        Entity attacker = mock(Entity.class);
        MutableDamage damage = new MutableDamage(4.0);
        damage.setCancelled(true);

        new MoltenHandler().onArmorDefense(defender, attacker, damage, 1);
        new ReflectHandler().onArmorDefense(defender, attacker, damage, 1);

        verifyNoInteractions(attacker);
        verify(defender, never()).damage(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any(Entity.class));
    }
}
