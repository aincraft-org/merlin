package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

final class OvercapEnchantmentListenerTest {
    @Test
    void dispatchesDamageBonusOnOvercapSharpness() {
        EnchantmentRegistry registry = EnchantmentRegistry.defaultRegistry();
        OvercapItemAdapter adapter = mock(OvercapItemAdapter.class);
        OvercapEnchantmentListener listener = new OvercapEnchantmentListener(adapter, registry);

        Player player = mock(Player.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack sword = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getItemInMainHand()).thenReturn(sword);

        // Level 7 Sharpness stored in overcap (+3.0 bonus damage over vanilla 5)
        when(adapter.readOvercap(sword)).thenReturn(Map.of(NamespacedKey.minecraft("sharpness"), 7));

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getDamage()).thenReturn(10.0);

        final double[] modifiedDamage = new double[]{10.0};
        org.mockito.Mockito.doAnswer(invocation -> {
            modifiedDamage[0] = invocation.getArgument(0);
            return null;
        }).when(event).setDamage(org.mockito.ArgumentMatchers.anyDouble());

        listener.onAttack(event);
        assertEquals(13.0, modifiedDamage[0], 0.001);
    }
}
