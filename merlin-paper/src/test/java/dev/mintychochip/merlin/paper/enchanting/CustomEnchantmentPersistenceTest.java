package dev.mintychochip.merlin.paper.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.IdentityHashMap;
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
    void mergesSequentialCustomApplicationsWithoutDroppingExistingEnchantments() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Merlin");
        when(plugin.namespace()).thenReturn("merlin");

        PersistentDataContainer root = mock(PersistentDataContainer.class);
        PersistentDataAdapterContext context = mock(PersistentDataAdapterContext.class);
        Map<PersistentDataContainer, Map<NamespacedKey, Integer>> valuesByContainer = new IdentityHashMap<>();
        AtomicReference<PersistentDataContainer> subRef = new AtomicReference<>();
        when(root.getAdapterContext()).thenReturn(context);
        when(root.get(any(NamespacedKey.class), eq(PersistentDataType.TAG_CONTAINER)))
                .thenAnswer(ignored -> subRef.get());
        when(context.newPersistentDataContainer()).thenAnswer(ignored -> {
            PersistentDataContainer created = mock(PersistentDataContainer.class);
            valuesByContainer.put(created, new HashMap<>());
            wireContainer(created, valuesByContainer);
            return created;
        });
        doAnswer(invocation -> {
            subRef.set(invocation.getArgument(2));
            return null;
        }).when(root).set(any(NamespacedKey.class), eq(PersistentDataType.TAG_CONTAINER),
                any(PersistentDataContainer.class));

        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(root);
        PersistentDataContainer existingSub = mock(PersistentDataContainer.class);
        valuesByContainer.put(existingSub, new HashMap<>());
        wireContainer(existingSub, valuesByContainer);
        NamespacedKey vanillaKey = new NamespacedKey("minecraft", "sharpness");
        valuesByContainer.get(existingSub).put(vanillaKey, 7);
        subRef.set(existingSub);
        AtomicReference<List<Component>> lore = new AtomicReference<>(List.of());
        when(meta.lore()).thenAnswer(ignored -> lore.get());
        doAnswer(invocation -> {
            lore.set(invocation.getArgument(0));
            return null;
        }).when(meta).lore(any());
        ItemStack item = new TestItemStack(meta);
        OvercapItemAdapter adapter = new OvercapItemAdapter(plugin, EnchantmentRegistry.defaultRegistry());
        NamespacedKey stickyGrip = new NamespacedKey("merlin", "sticky_grip");
        NamespacedKey equilibrium = new NamespacedKey("merlin", "equilibrium");

        assertTrue(adapter.applyEnchantments(item, Map.of(stickyGrip, 1)));
        assertEquals(Map.of(vanillaKey, 7, stickyGrip, 1), adapter.readOvercap(item));

        assertTrue(adapter.applyEnchantments(item, Map.of(equilibrium, 2)));
        assertEquals(Map.of(vanillaKey, 7, stickyGrip, 1, equilibrium, 2), adapter.readOvercap(item));

        assertTrue(adapter.applyEnchantments(item, Map.of(stickyGrip, 3)));
        assertEquals(Map.of(vanillaKey, 7, stickyGrip, 3, equilibrium, 2), adapter.readOvercap(item));

        String loreText = lore.get().stream()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        assertTrue(loreText.contains("Sticky Grip I"));
        assertTrue(loreText.contains("Equilibrium II"));
        assertTrue(loreText.contains("Sticky Grip III"));
    }

    private static void wireContainer(PersistentDataContainer container,
                                      Map<PersistentDataContainer, Map<NamespacedKey, Integer>> valuesByContainer) {
        when(container.getKeys()).thenAnswer(ignored -> Set.copyOf(valuesByContainer.get(container).keySet()));
        when(container.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenAnswer(invocation -> valuesByContainer.get(container).get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            valuesByContainer.get(container).put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(container).set(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), any(Integer.class));
        doAnswer(invocation -> {
            valuesByContainer.get(container).remove(invocation.getArgument(0));
            return null;
        }).when(container).remove(any(NamespacedKey.class));
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
