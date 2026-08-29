package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class AngelicHandlerTest {
    @Test
    void healsDefenderAfterPositiveDamage() {
        Player defender = mock(Player.class);
        AttributeInstance maxHealth = mock(AttributeInstance.class);
        when(defender.getAttribute(Attribute.MAX_HEALTH)).thenReturn(maxHealth);
        when(maxHealth.getValue()).thenReturn(20.0);
        when(defender.getHealth()).thenReturn(10.0);

        new AngelicHandler().onArmorDefensePost(defender, mock(Entity.class), new MutableDamage(4.0), 2);

        verify(defender).setHealth(12.0);
    }

    @Test
    void defersHealingUntilPostDamagePhase() {
        Player defender = mock(Player.class);

        new AngelicHandler().onArmorDefense(defender, mock(Entity.class), new MutableDamage(4.0), 1);

        verify(defender, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }
    @Test
    void doesNotHealCancelledDamage() {
        Player defender = mock(Player.class);
        MutableDamage damage = new MutableDamage(4.0);
        damage.setCancelled(true);

        new AngelicHandler().onArmorDefense(defender, mock(Entity.class), damage, 1);

        verify(defender, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }

    @Test
    void ignoresNonPositiveLevels() {
        Player defender = mock(Player.class);

        new AngelicHandler().onArmorDefense(defender, mock(Entity.class), new MutableDamage(4.0), 0);

        verify(defender, never()).setHealth(org.mockito.ArgumentMatchers.anyDouble());
    }
}
