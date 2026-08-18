package dev.mintychochip.wizardry.paper.mapgui;

import dev.mintychochip.wizardry.api.glyph.GlyphElement;
import dev.mintychochip.wizardry.api.glyph.GlyphDraft;
import dev.mintychochip.wizardry.api.glyph.GlyphLimits;
import dev.mintychochip.wizardry.api.glyph.GlyphPoint;
import dev.mintychochip.wizardry.api.glyph.GlyphRoles;
import dev.mintychochip.wizardry.api.glyph.GlyphToken;
import dev.mintychochip.wizardry.api.glyph.ManaTable;
import dev.mintychochip.wizardry.api.ml.Label;
import dev.mintychochip.wizardry.common.glyph.GlyphRasterizer;
import dev.mintychochip.wizardry.api.glyph.GlyphStroke;
import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GlyphDraftStoreAdapter {
    private final Server server;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey dataKey;
    private final NamespacedKey labelKey;
    private final NamespacedKey pipsKey;
    private final NamespacedKey manaKey;

    public GlyphDraftStoreAdapter(Plugin plugin) {
        server = plugin.getServer();
        markerKey = new NamespacedKey(plugin, "glyph_item");
        idKey = new NamespacedKey(plugin, "glyph_item_id");
        dataKey = new NamespacedKey(plugin, "glyph_draft_v1");
        labelKey = new NamespacedKey(plugin, "glyph_label");
        pipsKey = new NamespacedKey(plugin, "glyph_pips");
        manaKey = new NamespacedKey(plugin, "glyph_mana");
    }

    public ItemStack createGlyphItem() {
        var item = new ItemStack(Material.PAPER);
        setIdentity(item, UUID.randomUUID());
        return item;
    }

    public UUID itemId(ItemStack item) {
        if (!isGlyphItem(item)) return null;
        try {
            return UUID.fromString(item.getItemMeta().getPersistentDataContainer()
                    .get(idKey, PersistentDataType.STRING));
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public boolean save(ItemStack item, UUID expectedId, GlyphDraft draft) {
        if (!matches(item, expectedId) || draft == null) return false;
        byte[] encoded = encode(draft);
        if (encoded.length > GlyphLimits.MAX_SERIALIZED_BYTES) return false;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.BYTE_ARRAY, encoded);
        item.setItemMeta(meta);
        return true;
    }

    public Optional<PreparedMapSave> prepareMapSave(ItemStack source, UUID expectedId, GlyphDraft draft) {
        if (!matches(source, expectedId) || draft == null) return Optional.empty();
        byte[] encoded = encode(draft);
        if (encoded.length > GlyphLimits.MAX_SERIALIZED_BYTES) return Optional.empty();
        if (source.getType() == Material.FILLED_MAP) {
            var meta = source.getItemMeta();
            if (!(meta instanceof MapMeta mapMeta) || !mapMeta.hasMapView() || mapMeta.getMapView() == null) {
                return Optional.empty();
            }
        }
        return Optional.of(new PreparedMapSave(source.clone(), expectedId, draft, encoded));
    }

    public Optional<ItemStack> commitMapSave(PreparedMapSave prepared, World world) {
        if (prepared == null || world == null) return Optional.empty();
        try {
            var replacement = prepared.source().clone();
            org.bukkit.map.MapView view;
            if (replacement.getType() == Material.PAPER) {
                view = server.createMap(world);
                replacement.setType(Material.FILLED_MAP);
            } else {
                var sourceMeta = replacement.getItemMeta();
                if (!(sourceMeta instanceof MapMeta sourceMapMeta)) return Optional.empty();
                view = sourceMapMeta.getMapView();
                if (view == null) return Optional.empty();
            }
            var replacementMeta = replacement.getItemMeta();
            if (!(replacementMeta instanceof MapMeta mapMeta)) return Optional.empty();
            mapMeta.setMapView(view);
            var pdc = mapMeta.getPersistentDataContainer();
            pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(idKey, PersistentDataType.STRING, prepared.itemId().toString());
            pdc.set(dataKey, PersistentDataType.BYTE_ARRAY, prepared.encoded());
            replacement.setItemMeta(mapMeta);
            installRenderer(view, prepared.draft());
            return Optional.of(replacement);
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    public boolean saveToken(ItemStack item, UUID expectedId, GlyphToken token) {
        if (!matches(item, expectedId) || token == null) return false;
        var encoded = encodeToken(token, ManaTable.v1());
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(labelKey, PersistentDataType.STRING, encoded.label());
        pdc.set(pipsKey, PersistentDataType.INTEGER, encoded.pips());
        pdc.set(manaKey, PersistentDataType.INTEGER, encoded.mana());
        String title = tokenTitle(token);
        meta.displayName(Component.text(title));
        meta.lore(List.of(Component.text(title), Component.text("mana " + encoded.mana())));
        item.setItemMeta(meta);
        return true;
    }

    public Optional<GlyphToken> loadToken(ItemStack item) {
        if (!isGlyphItem(item)) return Optional.empty();
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String label = pdc.get(labelKey, PersistentDataType.STRING);
        Integer pips = pdc.get(pipsKey, PersistentDataType.INTEGER);
        Integer mana = pdc.get(manaKey, PersistentDataType.INTEGER);
        if (label == null || pips == null || mana == null) return Optional.empty();
        try {
            return Optional.of(decodeToken(new FrozenToken(label, pips, mana)));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public Optional<GlyphDraft> load(ItemStack item) {
        return loadUnbound(item);
    }
    public boolean restoreRenderer(ItemStack item) {
        if (itemId(item) == null || item.getType() != Material.FILLED_MAP) return false;
        var meta = item.getItemMeta();
        if (!(meta instanceof MapMeta mapMeta) || !mapMeta.hasMapView()) return false;
        var view = mapMeta.getMapView();
        var draft = load(item);
        if (view == null || draft.isEmpty()) return false;
        try {
            installRenderer(view, draft.get());
            return true;
        } catch (RuntimeException failure) {
            return false;
        }
    }


    public Optional<GlyphDraft> load(ItemStack item, UUID expectedId) {
        return matches(item, expectedId) ? loadUnbound(item) : Optional.empty();
    }

    private Optional<GlyphDraft> loadUnbound(ItemStack item) {
        if (!isGlyphItem(item)) return Optional.empty();
        byte[] bytes = item.getItemMeta().getPersistentDataContainer()
                .get(dataKey, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length > GlyphLimits.MAX_SERIALIZED_BYTES) return Optional.empty();
        try {
            return Optional.of(decode(bytes));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private boolean matches(ItemStack item, UUID expectedId) {
        UUID actualId = itemId(item);
        return actualId != null && actualId.equals(expectedId);
    }

    private boolean isGlyphItem(ItemStack item) {
        return item != null
                && isGlyphCanvasMaterial(item.getType())
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)
                && item.getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    static boolean isGlyphCanvasMaterial(Material material) {
        return material == Material.PAPER || material == Material.FILLED_MAP;
    }

    private void setIdentity(ItemStack item, UUID id) {
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(idKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
    }

    public record MapSaveRequest(UUID itemId, GlyphDraft draft) {
        public MapSaveRequest {
            if (itemId == null || draft == null) throw new IllegalArgumentException("glyph save request is incomplete");
        }
    }
    public record PreparedMapSave(ItemStack source, UUID itemId, GlyphDraft draft, byte[] encoded) {
        public PreparedMapSave {
            source = source.clone();
            encoded = encoded.clone();
        }

        @Override public ItemStack source() { return source.clone(); }
        @Override public byte[] encoded() { return encoded.clone(); }
    }

    private static void installRenderer(org.bukkit.map.MapView view, GlyphDraft draft) {
        view.setLocked(true);
        for (var renderer : java.util.List.copyOf(view.getRenderers())) view.removeRenderer(renderer);
        view.addRenderer(new GlyphMapRenderer(GlyphRasterizer.renderFull(draft)));
    }


    record FrozenToken(String label, int pips, int mana) {}

    static FrozenToken encodeToken(GlyphToken token, ManaTable mana) {
        return new FrozenToken(token.label().id(), token.pips(), mana.mana(token));
    }

    static GlyphToken decodeToken(FrozenToken encoded) {
        return new GlyphToken(Label.fromId(encoded.label()), encoded.pips());
    }

    public static String tokenTitle(GlyphToken token) {
        if (!GlyphRoles.hasPips(token.role())) return token.label().id();
        return token.label().id() + " " + "●".repeat(token.pips());
    }

    public static byte[] encode(GlyphDraft draft) {
        var out = new StringBuilder("3;");
        for (var stroke : draft.strokes()) {
            out.append(stroke.brushWidth()).append(',').append(stroke.startedAtMillis())
                    .append(',').append(stroke.element().name()).append(':');
            for (var point : stroke.points()) out.append(point.x()).append(',').append(point.y()).append(';');
            out.append(':');
            for (double width : stroke.segmentWidths()) out.append(width).append(',');
            out.append('|');
        }
        return Base64.getEncoder().encode(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static GlyphDraft decode(byte[] bytes) {
        var raw = new String(Base64.getDecoder().decode(bytes), java.nio.charset.StandardCharsets.UTF_8);
        boolean hasWidths;
        boolean hasElement;
        if (raw.startsWith("3;")) {
            hasWidths = true;
            hasElement = true;
        } else if (raw.startsWith("2;")) {
            hasWidths = true;
            hasElement = false;
        } else if (raw.startsWith("1;")) {
            hasWidths = false;
            hasElement = false;
        } else {
            throw new IllegalArgumentException();
        }
        var strokes = new ArrayList<GlyphStroke>();
        for (var encoded : raw.substring(2).split("\\|")) {
            if (encoded.isBlank()) continue;
            var parts = encoded.split(":", hasWidths ? 3 : 2);
            var meta = parts[0].split(",", hasElement ? 3 : 2);
            var points = new ArrayList<GlyphPoint>();
            for (var point : parts[1].split(";")) {
                if (point.isBlank()) continue;
                var xy = point.split(",", 2);
                points.add(new GlyphPoint(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
            }
            double brushWidth = Double.parseDouble(meta[0]);
            long startedAt = Long.parseLong(meta[1]);
            var element = hasElement ? GlyphElement.valueOf(meta[2]) : GlyphElement.PHYSICAL;
            if (!hasWidths) {
                strokes.add(new GlyphStroke(points, brushWidth, startedAt));
                continue;
            }
            var widths = new ArrayList<Double>();
            for (var width : parts[2].split(",")) {
                if (!width.isBlank()) widths.add(Double.parseDouble(width));
            }
            strokes.add(new GlyphStroke(points, brushWidth, startedAt, widths, element));
        }
        return new GlyphDraft(strokes);
    }
}
