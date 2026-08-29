package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.custom.MutableDamage;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class WeaponDefenseHandlerTest {
    @Test
    void armoredReducesSwordDamageByTenPercentPerLevel() {
        LivingEntity attacker = attackerHolding(Material.DIAMOND_SWORD);
        MutableDamage damage = new MutableDamage(10.0);

        new ArmoredHandler().onArmorDefense(mock(Player.class), attacker, damage, 2);

        assertEquals(0.8, damage.getMultiplier(), 0.0001);
        assertEquals(8.0, damage.getFinalDamage(), 0.0001);
    }

    @Test
    void tankReducesAxeDamageByTenPercentPerLevel() {
        LivingEntity attacker = attackerHolding(Material.NETHERITE_AXE);
        MutableDamage damage = new MutableDamage(10.0);

        new TankHandler().onArmorDefense(mock(Player.class), attacker, damage, 2);

        assertEquals(0.8, damage.getMultiplier(), 0.0001);
    }

    @Test
    void wrongWeaponLeavesDamageUnchanged() {
        LivingEntity attacker = attackerHolding(Material.DIAMOND_AXE);
        MutableDamage armoredDamage = new MutableDamage(10.0);
        MutableDamage tankDamage = new MutableDamage(10.0);

        new ArmoredHandler().onArmorDefense(mock(Player.class), attacker, armoredDamage, 2);
        new TankHandler().onArmorDefense(mock(Player.class), attackerHolding(Material.DIAMOND_SWORD), tankDamage, 2);

        assertEquals(1.0, armoredDamage.getMultiplier(), 0.0001);
        assertEquals(1.0, tankDamage.getMultiplier(), 0.0001);
    }

    @Test
    void reductionsClampAtZero() {
        LivingEntity attacker = attackerHolding(Material.DIAMOND_SWORD);
        MutableDamage damage = new MutableDamage(10.0);

        new ArmoredHandler().onArmorDefense(mock(Player.class), attacker, damage, 20);

        assertEquals(0.0, damage.getFinalDamage(), 0.0001);
    }

    @Test
    void nonLivingAttackersAreIgnored() {
        MutableDamage damage = new MutableDamage(10.0);

        new ArmoredHandler().onArmorDefense(mock(Player.class), mock(Entity.class), damage, 2);

        assertEquals(1.0, damage.getMultiplier(), 0.0001);
    }

    private static LivingEntity attackerHolding(Material material) {
        LivingEntity attacker = mock(LivingEntity.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack item = mock(ItemStack.class);
        when(attacker.getEquipment()).thenReturn(equipment);
        when(equipment.getItemInMainHand()).thenReturn(item);
        when(item.isEmpty()).thenReturn(false);
        when(item.getType()).thenReturn(material);
        return attacker;
    }
}
