package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

final class ArmorEnchantmentIntegrationTest {
    @Test
    void dispatcherReachesAegisEnvironmentalHandler() {
        NamespacedKey key = key("aegis");
        ItemStack armorPiece = mock(ItemStack.class);
        CustomEnchantmentDispatcher dispatcher = dispatcherFor(armorPiece, key);
        Player player = mock(Player.class);

        dispatcher.dispatchEnvironmentalDamage(
                player, DamageCause.FALL, new MutableDamage(4.0), new ItemStack[]{armorPiece});

        verify(player).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0));
    }

    @Test
    void dispatcherReachesArmoredDefenseHandler() {
        NamespacedKey key = key("armored");
        ItemStack armorPiece = mock(ItemStack.class);
        CustomEnchantmentDispatcher dispatcher = dispatcherFor(armorPiece, key);
        Player defender = mock(Player.class);
        LivingEntity attacker = mock(LivingEntity.class);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack sword = mock(ItemStack.class);
        when(attacker.getEquipment()).thenReturn(equipment);
        when(equipment.getItemInMainHand()).thenReturn(sword);
        when(sword.isEmpty()).thenReturn(false);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        MutableDamage damage = new MutableDamage(10.0);

        dispatcher.dispatchArmorDefense(defender, attacker, damage, new ItemStack[]{armorPiece});

        assertEquals(0.9, damage.getMultiplier(), 0.0001);
    }

    private static CustomEnchantmentDispatcher dispatcherFor(ItemStack item, NamespacedKey key) {
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 1));
        return new CustomEnchantmentDispatcher(adapter, EnchantmentRegistry.defaultRegistry());
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("merlin", name);
    }
}
