package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class CustomEnchantmentPersistenceTest {
    @Test
    void persistsCustomRankOneInPdcAndLore() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");

        PersistentDataContainer root = mock(PersistentDataContainer.class);
        PersistentDataContainer sub = mock(PersistentDataContainer.class);
        PersistentDataAdapterContext context = mock(PersistentDataAdapterContext.class);
        Map<NamespacedKey, Integer> values = new HashMap<>();
        when(root.getAdapterContext()).thenReturn(context);
        when(context.newPersistentDataContainer()).thenReturn(sub);
        when(root.get(any(NamespacedKey.class), eq(PersistentDataType.TAG_CONTAINER))).thenReturn(sub);
        when(sub.getKeys()).thenAnswer(ignored -> Set.copyOf(values.keySet()));
        when(sub.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(sub).set(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), any(Integer.class));

        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(root);
        AtomicReference<List<Component>> lore = new AtomicReference<>(List.of());
        when(meta.lore()).thenAnswer(ignored -> lore.get());
        doAnswer(invocation -> {
            lore.set(invocation.getArgument(0));
            return null;
        }).when(meta).lore(any());
        ItemStack item = new TestItemStack(meta);
        OvercapItemAdapter adapter = new OvercapItemAdapter(plugin, EnchantmentRegistry.defaultRegistry());
        NamespacedKey stickyGrip = new NamespacedKey("merlin", "sticky_grip");

        assertTrue(adapter.applyEnchantments(item, Map.of(stickyGrip, 1)));

        assertEquals(1, adapter.readOvercap(item).get(stickyGrip));
        String loreText = lore.get().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        assertTrue(loreText.contains("Sticky Grip I"));
    }

    private static final class TestItemStack extends ItemStack {
        private ItemMeta meta;

        private TestItemStack(ItemMeta meta) {
            super();
            this.meta = meta;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean hasItemMeta() {
            return true;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public boolean setItemMeta(ItemMeta itemMeta) {
            meta = itemMeta;
            return true;
        }
    }
}
