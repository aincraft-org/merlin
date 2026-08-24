package dev.mintychochip.merlin.paper.book;

import dev.mintychochip.merlin.api.dsl.CompilerConstants;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class ScribeBookStore {
    public static final String STARTER_SOURCE = "look ahead 8\nburn target\nrest 3 seconds";
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey sourceKey;

    public ScribeBookStore(Plugin plugin) {
        markerKey = new NamespacedKey(plugin, "scribe_book");
        idKey = new NamespacedKey(plugin, "scribe_book_id");
        sourceKey = new NamespacedKey(plugin, "scribe_source");
    }

    public ItemStack createBook() {
        var item = new ItemStack(org.bukkit.Material.WRITABLE_BOOK);
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(sourceKey, PersistentDataType.STRING, STARTER_SOURCE);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isScribeBook(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.WRITABLE_BOOK || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)
                && item.getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    public UUID bookId(ItemStack item) {
        if (!isScribeBook(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ex) { return null; }
    }

    public String source(ItemStack item) {
        if (!isScribeBook(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(sourceKey, PersistentDataType.STRING, STARTER_SOURCE);
    }

    public boolean save(ItemStack held, UUID expectedBookId, String source) {
        if (!isScribeBook(held) || !expectedBookId.equals(bookId(held)) || !withinLimits(source)) return false;
        var meta = held.getItemMeta();
        meta.getPersistentDataContainer().set(sourceKey, PersistentDataType.STRING, source);
        held.setItemMeta(meta);
        return true;
    }

    private boolean withinLimits(String source) {
        return source.codePointCount(0, source.length()) <= CompilerConstants.MAX_SOURCE_SCALARS
                && source.getBytes(StandardCharsets.UTF_8).length <= CompilerConstants.MAX_SOURCE_UTF8_BYTES;
    }
}
