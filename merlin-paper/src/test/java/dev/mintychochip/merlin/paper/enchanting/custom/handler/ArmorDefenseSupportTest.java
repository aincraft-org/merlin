package dev.mintychochip.merlin.paper.enchanting.custom.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class ArmorDefenseSupportTest {
    @Test
    void detectsMainHandMaterialSuffix() {
        LivingEntity attacker = mock(LivingEntity.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack sword = mock(ItemStack.class);
        when(attacker.getEquipment()).thenReturn(equipment);
        when(equipment.getItemInMainHand()).thenReturn(sword);
        when(sword.isEmpty()).thenReturn(false);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);

        assertTrue(ArmorDefenseSupport.attackerHolds(attacker, "_SWORD"));
        assertFalse(ArmorDefenseSupport.attackerHolds(attacker, "_AXE"));
    }

    @Test
    void resolvesLivingProjectileShooter() {
        Projectile projectile = mock(Projectile.class);
        LivingEntity shooter = mock(LivingEntity.class);
        when(projectile.getShooter()).thenReturn(shooter);

        assertSame(shooter, ArmorDefenseSupport.livingAttacker(projectile));
        assertSame(shooter, ArmorDefenseSupport.livingAttacker(shooter));
    }

    @Test
    void returnsNullForNonLivingAttacker() {
        assertEquals(null, ArmorDefenseSupport.livingAttacker(mock(org.bukkit.entity.Entity.class)));
    }

    @Test
    void clampsReductionMultiplier() {
        assertEquals(0.8, ArmorDefenseSupport.reductionMultiplier(2, 0.1), 0.0001);
        assertEquals(0.0, ArmorDefenseSupport.reductionMultiplier(20, 0.1), 0.0001);
        assertEquals(1.0, ArmorDefenseSupport.reductionMultiplier(0, 0.1), 0.0001);
    }
}
