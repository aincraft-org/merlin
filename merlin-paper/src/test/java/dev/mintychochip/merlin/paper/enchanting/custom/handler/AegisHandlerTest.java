package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

final class AegisHandlerTest {
    @Test
    void grantsSpeedAfterFallDamage() {
        Player player = mock(Player.class);

        new AegisHandler().onEnvironmentalDamage(player, DamageCause.FALL, new MutableDamage(4.0), 2);

        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
    }

    @Test
    void ignoresOtherDamageCauses() {
        Player player = mock(Player.class);

        new AegisHandler().onEnvironmentalDamage(player, DamageCause.FIRE, new MutableDamage(4.0), 2);

        verify(player, never()).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player player = mock(Player.class);

        new AegisHandler().onEnvironmentalDamage(player, DamageCause.FALL, new MutableDamage(4.0), 0);

        verify(player, never()).addPotionEffect(any(PotionEffect.class));
    }
    @Test
    void ignoresMissingOrZeroDamage() {
        Player player = mock(Player.class);
        AegisHandler handler = new AegisHandler();

        handler.onEnvironmentalDamage(player, DamageCause.FALL, null, 1);
        handler.onEnvironmentalDamage(player, DamageCause.FALL, new MutableDamage(0.0), 1);

        verify(player, never()).addPotionEffect(any(PotionEffect.class));
    }
}
