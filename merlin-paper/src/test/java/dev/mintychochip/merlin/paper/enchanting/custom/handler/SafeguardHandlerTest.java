package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

final class SafeguardHandlerTest {
    @Test
    void grantsResistanceAfterDefendingAgainstDamage() {
        Player defender = mock(Player.class);

        new SafeguardHandler().onArmorDefense(defender, mock(Entity.class), new MutableDamage(4.0), 2);

        verify(defender).addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
    }

    @Test
    void ignoresCancelledDamage() {
        Player defender = mock(Player.class);
        MutableDamage damage = new MutableDamage(4.0);
        damage.setCancelled(true);

        new SafeguardHandler().onArmorDefense(defender, mock(Entity.class), damage, 1);

        verify(defender, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player defender = mock(Player.class);

        new SafeguardHandler().onArmorDefense(defender, mock(Entity.class), new MutableDamage(4.0), 0);

        verify(defender, never()).addPotionEffect(any(PotionEffect.class));
    }
}
