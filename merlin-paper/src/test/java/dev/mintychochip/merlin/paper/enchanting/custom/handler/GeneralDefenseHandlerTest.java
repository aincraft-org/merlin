package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class GeneralDefenseHandlerTest {
    @Test
    void chunkyReducesDamageByFivePercentPerLevel() {
        MutableDamage damage = new MutableDamage(10.0);

        new ChunkyHandler().onArmorDefense(mock(Player.class), mock(Entity.class), damage, 2);

        assertEquals(0.9, damage.getMultiplier(), 0.0001);
    }

    @Test
    void heavyReducesArrowDamageByTenPercentPerLevel() {
        MutableDamage damage = new MutableDamage(10.0);

        new HeavyHandler().onArmorDefense(mock(Player.class), mock(AbstractArrow.class), damage, 2);

        assertEquals(0.8, damage.getMultiplier(), 0.0001);
    }

    @Test
    void heavyIgnoresNonArrowAttackers() {
        MutableDamage damage = new MutableDamage(10.0);

        new HeavyHandler().onArmorDefense(mock(Player.class), mock(Entity.class), damage, 2);

        assertEquals(1.0, damage.getMultiplier(), 0.0001);
    }

    @Test
    void nonPositiveLevelLeavesDamageUnchanged() {
        MutableDamage damage = new MutableDamage(10.0);

        new ChunkyHandler().onArmorDefense(mock(Player.class), mock(Entity.class), damage, 0);
        new HeavyHandler().onArmorDefense(mock(Player.class), mock(AbstractArrow.class), damage, 0);

        assertEquals(1.0, damage.getMultiplier(), 0.0001);
    }
}
