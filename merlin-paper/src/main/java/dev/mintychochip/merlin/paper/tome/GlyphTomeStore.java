package dev.mintychochip.merlin.paper.tome;

import dev.mintychochip.merlin.api.glyph.GlyphDraft;
import dev.mintychochip.merlin.api.glyph.GlyphToken;
import dev.mintychochip.merlin.api.glyph.ManaTable;
import dev.mintychochip.merlin.api.glyph.TomePages;
import dev.mintychochip.merlin.api.ml.Label;
import dev.mintychochip.merlin.common.glyph.GlyphCompilerImpl;
import dev.mintychochip.merlin.paper.mapgui.GlyphDraftStoreAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GlyphTomeStore {
    private static final int MAX_PAGES = 3;

    private final GlyphDraftStoreAdapter maps;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey pagesKey;
    private final NamespacedKey indexKey;
    private final NamespacedKey[] draftKeys;

    public GlyphTomeStore(Plugin plugin, GlyphDraftStoreAdapter maps) {
        this.maps = maps;
        markerKey = new NamespacedKey(plugin, "glyph_tome");
        idKey = new NamespacedKey(plugin, "glyph_tome_id");
        pagesKey = new NamespacedKey(plugin, "glyph_tome_pages");
        indexKey = new NamespacedKey(plugin, "glyph_tome_index");
        draftKeys = new NamespacedKey[] {
            new NamespacedKey(plugin, "glyph_tome_draft_0"),
            new NamespacedKey(plugin, "glyph_tome_draft_1"),
            new NamespacedKey(plugin, "glyph_tome_draft_2")
        };
    }

    public ItemStack createTome() {
        var item = new ItemStack(Material.BOOK);
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(pagesKey, PersistentDataType.STRING, "");
        pdc.set(indexKey, PersistentDataType.INTEGER, 0);
        applyDisplay(meta, TomePages.empty(), 0);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTome(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(markerKey, PersistentDataType.BYTE)
                && pdc.has(idKey, PersistentDataType.STRING);
    }

    public UUID tomeId(ItemStack item) {
        if (!isTome(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        try {
            return UUID.fromString(raw);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public TomePages pages(ItemStack item) {
        if (!isTome(item)) return TomePages.empty();
        String raw = item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(pagesKey, PersistentDataType.STRING, "");
        try {
            var pages = TomePages.empty();
            for (var token : decodePages(raw)) {
                var next = pages.insert(token, GlyphCompilerImpl.INSTANCE);
                if (next.isEmpty()) return TomePages.empty();
                pages = next.get();
            }
            return pages;
        } catch (RuntimeException invalid) {
            return TomePages.empty();
        }
    }

    public List<GlyphToken> tokens(ItemStack item) {
        return pages(item).tokens();
    }

    public int index(ItemStack item) {
        if (!isTome(item)) return 0;
        Integer stored = item.getItemMeta().getPersistentDataContainer()
                .get(indexKey, PersistentDataType.INTEGER);
        int n = pages(item).tokens().size();
        int idx = stored == null ? 0 : stored;
        if (n == 0) return 0;
        return Math.max(0, Math.min(idx, n - 1));
    }

    public Optional<TomePages> insert(ItemStack tome, ItemStack map, boolean sneak) {
        if (!isTome(tome)) return Optional.empty();
        var token = maps.loadToken(map);
        if (token.isEmpty()) return Optional.empty();
        var current = pages(tome);
        var next = current.insert(token.get(), GlyphCompilerImpl.INSTANCE);
        if (next.isEmpty()) return Optional.empty();
        var drafts = loadDrafts(tome, current.tokens().size());
        drafts.add(draftBytes(map));
        writePages(tome, next.get(), drafts, next.get().tokens().size() - 1);
        if (sneak) map.setAmount(Math.max(0, map.getAmount() - 1));
        return next;
    }

    public Optional<ItemStack> tear(ItemStack tome) {
        return tear(tome, null);
    }

    public Optional<ItemStack> tear(ItemStack tome, World world) {
        if (!isTome(tome)) return Optional.empty();
        var current = pages(tome);
        int idx = index(tome);
        if (current.tokens().isEmpty() || idx < 0 || idx >= current.tokens().size()) {
            return Optional.empty();
        }
        var torn = current.tear(idx);
        var drafts = loadDrafts(tome, current.tokens().size());
        byte[] draftBytes = drafts.remove(idx);
        int nextIndex = torn.pages().tokens().isEmpty() ? 0
                : Math.min(idx, torn.pages().tokens().size() - 1);
        writePages(tome, torn.pages(), drafts, nextIndex);
        return Optional.of(restoreMap(torn.torn(), draftBytes, world));
    }

    public boolean flip(ItemStack tome, int delta) {
        if (!isTome(tome)) return false;
        var current = pages(tome);
        int n = current.tokens().size();
        if (n == 0) return false;
        int next = Math.floorMod(index(tome) + delta, n);
        var meta = tome.getItemMeta();
        meta.getPersistentDataContainer().set(indexKey, PersistentDataType.INTEGER, next);
        applyDisplay(meta, current, next);
        tome.setItemMeta(meta);
        return true;
    }

    static String encodePages(List<GlyphToken> tokens) {
        var mana = ManaTable.v1();
        var out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) out.append('\n');
            var token = tokens.get(i);
            out.append(token.label().id()).append('|')
                    .append(token.pips()).append('|')
                    .append(mana.mana(token));
        }
        return out.toString();
    }

    static List<GlyphToken> decodePages(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        var out = new ArrayList<GlyphToken>();
        for (var line : raw.split("\n", -1)) {
            if (line.isBlank()) continue;
            var parts = line.split("\\|", 3);
            if (parts.length < 2) throw new IllegalArgumentException("page");
            out.add(new GlyphToken(Label.fromId(parts[0]), Integer.parseInt(parts[1])));
        }
        return List.copyOf(out);
    }

    private void writePages(ItemStack tome, TomePages pages, List<byte[]> drafts, int index) {
        var meta = tome.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(pagesKey, PersistentDataType.STRING, encodePages(pages.tokens()));
        pdc.set(indexKey, PersistentDataType.INTEGER, index);
        for (int i = 0; i < MAX_PAGES; i++) {
            if (i < drafts.size() && drafts.get(i) != null) {
                pdc.set(draftKeys[i], PersistentDataType.BYTE_ARRAY, drafts.get(i));
            } else {
                pdc.remove(draftKeys[i]);
            }
        }
        applyDisplay(meta, pages, index);
        tome.setItemMeta(meta);
    }

    private List<byte[]> loadDrafts(ItemStack tome, int count) {
        var pdc = tome.getItemMeta().getPersistentDataContainer();
        var drafts = new ArrayList<byte[]>(count);
        for (int i = 0; i < count; i++) {
            drafts.add(pdc.get(draftKeys[i], PersistentDataType.BYTE_ARRAY));
        }
        return drafts;
    }

    private byte[] draftBytes(ItemStack map) {
        return maps.load(map)
                .map(GlyphDraftStoreAdapter::encode)
                .orElseGet(() -> GlyphDraftStoreAdapter.encode(GlyphDraft.empty()));
    }

    private ItemStack restoreMap(GlyphToken token, byte[] draftBytes, World world) {
        var item = maps.createGlyphItem();
        var id = maps.itemId(item);
        var draft = decodeDraft(draftBytes);
        if (world != null) {
            var prepared = maps.prepareMapSave(item, id, draft);
            if (prepared.isPresent()) {
                var committed = maps.commitMapSave(prepared.get(), world);
                if (committed.isPresent()) item = committed.get();
            }
        } else {
            maps.save(item, id, draft);
        }
        maps.saveToken(item, id, token);
        return item;
    }

    private static GlyphDraft decodeDraft(byte[] draftBytes) {
        if (draftBytes == null) return GlyphDraft.empty();
        try {
            return GlyphDraftStoreAdapter.decode(draftBytes);
        } catch (RuntimeException invalid) {
            return GlyphDraft.empty();
        }
    }

    private static void applyDisplay(ItemMeta meta, TomePages pages, int index) {
        meta.displayName(Component.text("Glyph Tome"));
        var lore = new ArrayList<Component>();
        var tokens = pages.tokens();
        if (tokens.isEmpty()) {
            lore.add(Component.text("empty"));
        } else {
            lore.add(Component.text("page " + (index + 1) + "/" + tokens.size()));
            int mana = 0;
            var table = ManaTable.v1();
            for (var token : tokens) {
                lore.add(Component.text(GlyphDraftStoreAdapter.tokenTitle(token)));
                mana += table.mana(token);
            }
            lore.add(Component.text("mana " + mana));
        }
        meta.lore(lore);
    }
}
