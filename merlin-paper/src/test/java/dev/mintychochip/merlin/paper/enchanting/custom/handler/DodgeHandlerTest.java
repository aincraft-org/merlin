package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import java.util.Random;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.junit.jupiter.api.Test;

final class DodgeHandlerTest {
    @Test
    void lowRollCancelsLivingEntityDamage() {
        Random random = mock(Random.class);
        when(random.nextInt(100)).thenReturn(9);
        MutableDamage damage = new MutableDamage(10.0);

        new DodgeHandler(random).onArmorDefense(mock(Player.class), mock(LivingEntity.class), damage, 1);

        assertTrue(damage.isCancelled());
        verify(random).nextInt(100);
    }

    @Test
    void highRollLeavesDamageUnchanged() {
        Random random = mock(Random.class);
        when(random.nextInt(100)).thenReturn(10);
        MutableDamage damage = new MutableDamage(10.0);

        new DodgeHandler(random).onArmorDefense(mock(Player.class), mock(LivingEntity.class), damage, 1);

        assertFalse(damage.isCancelled());
    }

    @Test
    void ignoresProjectileAttacks() {
        Random random = mock(Random.class);
        MutableDamage damage = new MutableDamage(10.0);

        new DodgeHandler(random).onArmorDefense(mock(Player.class), mock(Projectile.class), damage, 3);

        assertFalse(damage.isCancelled());
        verifyNoInteractions(random);
    }

    @Test
    void ignoresNonPositiveLevels() {
        Random random = mock(Random.class);
        MutableDamage damage = new MutableDamage(10.0);

        new DodgeHandler(random).onArmorDefense(mock(Player.class), mock(LivingEntity.class), damage, 0);

        assertFalse(damage.isCancelled());
        verifyNoInteractions(random);
    }
}
