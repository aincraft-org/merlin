package dev.mintychochip.merlin.paper.enchanting.custom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.mintychochip.merlin.paper.enchanting.EnchantmentDefinition;
import dev.mintychochip.merlin.paper.enchanting.EnchantmentRegistry;
import dev.mintychochip.merlin.paper.enchanting.OvercapEffectHandler;
import dev.mintychochip.merlin.paper.enchanting.OvercapItemAdapter;
import dev.mintychochip.merlin.paper.enchanting.custom.trigger.ActivateTrigger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

final class ActivationTriggerTest {
    interface TestActivateHandler extends OvercapEffectHandler, ActivateTrigger {}

    @Test
    void admitsActivationOnceUntilCooldownExpires() {
        NamespacedKey key = new NamespacedKey("merlin", "test-activation");
        TestActivateHandler handler = mock(TestActivateHandler.class);
        when(handler.key()).thenReturn(key);
        when(handler.activationCooldown()).thenReturn(Duration.ofSeconds(5));
        when(handler.onActivate(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Activation", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_SWORD), Optional.of(handler)));

        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack item = mock(ItemStack.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 2));

        Clock clock = Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC);
        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapter, registry, new ActivationCooldowns(clock));
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);

        assertTrue(dispatcher.dispatchActivate(player, item));
        assertFalse(dispatcher.dispatchActivate(player, item));
        verify(handler).onActivate(2, player, item);
    }

    @Test
    void doesNotConsumeCooldownWhenActivationDeclines() {
        NamespacedKey key = new NamespacedKey("merlin", "test-declined-activation");
        TestActivateHandler handler = mock(TestActivateHandler.class);
        when(handler.key()).thenReturn(key);
        when(handler.activationCooldown()).thenReturn(Duration.ofSeconds(5));
        when(handler.onActivate(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false, true);

        EnchantmentRegistry registry = new EnchantmentRegistry();
        registry.register(new EnchantmentDefinition(
                key, "Test Declined Activation", 0, 3, 10, 5, 10,
                Set.of(Material.DIAMOND_SWORD), Optional.of(handler)));
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        ItemStack item = mock(ItemStack.class);
        when(adapter.readOvercap(item)).thenReturn(Map.of(key, 1));

        CustomEnchantmentDispatcher dispatcher = new CustomEnchantmentDispatcher(
                adapter, registry, new ActivationCooldowns());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertFalse(dispatcher.dispatchActivate(player, item));
        assertTrue(dispatcher.dispatchActivate(player, item));
    }

    @Test
    void clearsCooldownsForPlayerLifecycleCleanup() {
        ActivationCooldowns cooldowns = new ActivationCooldowns();
        UUID playerId = UUID.randomUUID();
        NamespacedKey key = new NamespacedKey("merlin", "lifecycle");

        assertTrue(cooldowns.tryAcquire(playerId, key, Duration.ofMinutes(1)));
        cooldowns.clearPlayer(playerId);

        assertTrue(cooldowns.tryAcquire(playerId, key, Duration.ofMinutes(1)));
    }
}
