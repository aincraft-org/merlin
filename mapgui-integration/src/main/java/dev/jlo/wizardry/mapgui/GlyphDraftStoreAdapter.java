package dev.jlo.wizardry.mapgui;

import dev.jlo.wizardry.glyph.GlyphDraft;
import dev.jlo.wizardry.glyph.GlyphLimits;
import dev.jlo.wizardry.glyph.GlyphPoint;
import dev.jlo.wizardry.glyph.GlyphStroke;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class GlyphDraftStoreAdapter {
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey dataKey;
    public GlyphDraftStoreAdapter(Plugin plugin) { markerKey = new NamespacedKey(plugin, "glyph_item"); idKey = new NamespacedKey(plugin, "glyph_item_id"); dataKey = new NamespacedKey(plugin, "glyph_draft_v1"); }
    public ItemStack createGlyphItem() { var item = new ItemStack(Material.PAPER); var meta = item.getItemMeta(); var pdc = meta.getPersistentDataContainer(); pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1); pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString()); item.setItemMeta(meta); return item; }
    public UUID itemId(ItemStack item) { if (!isGlyphItem(item)) return null; try { return UUID.fromString(item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING)); } catch (RuntimeException e) { return null; } }
    public boolean save(ItemStack item, UUID expectedId, GlyphDraft draft) { if (itemId(item) == null || !itemId(item).equals(expectedId) || draft == null) return false; byte[] encoded = encode(draft); if (encoded.length > GlyphLimits.MAX_SERIALIZED_BYTES) return false; var meta = item.getItemMeta(); meta.getPersistentDataContainer().set(dataKey, PersistentDataType.BYTE_ARRAY, encoded); item.setItemMeta(meta); return true; }
    public Optional<GlyphDraft> load(ItemStack item) { return loadUnbound(item); }
    public Optional<GlyphDraft> load(ItemStack item, UUID expectedId) { return itemId(item) != null && itemId(item).equals(expectedId) ? loadUnbound(item) : Optional.empty(); }
    private Optional<GlyphDraft> loadUnbound(ItemStack item) { if (!isGlyphItem(item)) return Optional.empty(); byte[] bytes = item.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.BYTE_ARRAY); if (bytes == null || bytes.length > GlyphLimits.MAX_SERIALIZED_BYTES) return Optional.empty(); try { return Optional.of(decode(bytes)); } catch (RuntimeException ignored) { return Optional.empty(); } }
    private boolean isGlyphItem(ItemStack item) { return item != null && item.getType() == Material.PAPER && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE) && item.getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING); }
    private static byte[] encode(GlyphDraft draft) { var out = new StringBuilder("1;"); for (var stroke : draft.strokes()) { out.append(stroke.brushWidth()).append(',').append(stroke.startedAtMillis()).append(':'); for (var p : stroke.points()) out.append(p.x()).append(',').append(p.y()).append(';'); out.append('|'); } return Base64.getEncoder().encode(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static GlyphDraft decode(byte[] bytes) { var raw = new String(Base64.getDecoder().decode(bytes), java.nio.charset.StandardCharsets.UTF_8); if (!raw.startsWith("1;")) throw new IllegalArgumentException(); var strokes = new ArrayList<GlyphStroke>(); for (var encoded : raw.substring(2).split("\\|")) if (!encoded.isBlank()) { var parts = encoded.split(":", 2); var meta = parts[0].split(",", 2); var points = new ArrayList<GlyphPoint>(); for (var point : parts[1].split(";")) if (!point.isBlank()) { var xy = point.split(",", 2); points.add(new GlyphPoint(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]))); } strokes.add(new GlyphStroke(points, Double.parseDouble(meta[0]), Long.parseLong(meta[1]))); } return new GlyphDraft(strokes); }
}
