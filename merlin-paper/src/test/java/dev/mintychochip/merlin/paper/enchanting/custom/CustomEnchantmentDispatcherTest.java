package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.BlockBreakTrigger;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.EntityHitTrigger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentDispatcherTest {
    interface TestHitHandler extends OvercapEffectHandler, EntityHitTrigger {}
    interface TestBreakHandler extends OvercapEffectHandler, BlockBreakTrigger {}

    @Test
    void dispatchesEntityHitToRegisteredTriggers() {
        NamespacedKey key = new NamespacedKey("merlin", "test_vampirism");
        TestHitHandler handler = mock(TestHitHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Vampirism", 0, 3, 10, 5, 10, Set.of(Material.DIAMOND_SWORD), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack sword = mock(ItemStack.class);
        when(adapter.readOvercap(sword)).thenReturn(Map.of(key, 2));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        Player attacker = mock(Player.class);
        LivingEntity victim = mock(LivingEntity.class);
        MutableDamage dmg = new MutableDamage(10.0);

        dispatcher.dispatchEntityHit(attacker, victim, dmg, sword);
        verify(handler).onEntityHit(attacker, victim, dmg, 2);
    }

    @Test
    void dispatchesBlockBreakToRegisteredTriggers() {
        NamespacedKey key = new NamespacedKey("merlin", "test_vein_miner");
        TestBreakHandler handler = mock(TestBreakHandler.class);
        when(handler.key()).thenReturn(key);

        EnchantmentDefinition def = new EnchantmentDefinition(
                key, "Test Vein Miner", 0, 3, 20, 8, 5, Set.of(Material.DIAMOND_PICKAXE), Optional.of(handler));

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(def);

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack pickaxe = mock(ItemStack.class);
        when(adapter.readOvercap(pickaxe)).thenReturn(Map.of(key, 3));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(adapter, registry);

        Player player = mock(Player.class);
        Block block = mock(Block.class);
        CascadeScope scope = mock(CascadeScope.class);

        dispatcher.dispatchBlockBreak(player, block, pickaxe, scope);
        verify(handler).onBlockBreak(player, block, 3, scope);
    }
}
