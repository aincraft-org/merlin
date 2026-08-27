package dev.mintychochip.merlin.paper.ink;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.glyph.MagicalInk;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class InkStore {
    private final NamespacedKey markerKey;
    private final NamespacedKey elementKey;
    private final NamespacedKey remainingKey;
    private final NamespacedKey maxKey;

    public InkStore(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        markerKey = new NamespacedKey(plugin, "magical_ink");
        elementKey = new NamespacedKey(plugin, "ink_element");
        remainingKey = new NamespacedKey(plugin, "ink_remaining");
        maxKey = new NamespacedKey(plugin, "ink_max");
    }

    public ItemStack create(GlyphElement element) {
        var item = new ItemStack(Material.POTION);
        write(item, MagicalInk.full(element));
        return item;
    }

    public boolean isInk(ItemStack item) {
        if (item == null || !isBottleType(item.getType()) || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(markerKey, PersistentDataType.BYTE)
                && pdc.has(elementKey, PersistentDataType.STRING);
    }

    public Optional<MagicalInk> read(ItemStack item) {
        if (!isInk(item)) return Optional.empty();
        var pdc = item.getItemMeta().getPersistentDataContainer();
        GlyphElement element;
        try {
            element = GlyphElement.parse(pdc.get(elementKey, PersistentDataType.STRING));
        } catch (RuntimeException invalid) {
            return Optional.empty();
        }
        Integer remaining = pdc.get(remainingKey, PersistentDataType.INTEGER);
        Integer max = pdc.get(maxKey, PersistentDataType.INTEGER);
        if (remaining == null || max == null) return Optional.empty();
        try {
            return Optional.of(new MagicalInk(element, remaining, max));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public List<GlyphElement> available(Inventory inventory) {
        return MagicalInk.filledElements(fills(inventory));
    }

    public Optional<ItemStack> find(Inventory inventory, GlyphElement element) {
        if (element == null) return Optional.empty();
        for (var item : bottles(inventory)) {
            var ink = read(item);
            if (ink.isPresent() && ink.get().element() == element && !ink.get().empty()) return Optional.of(item);
        }
        return Optional.empty();
    }

    public List<MagicalInk> fills(Inventory inventory) {
        var inks = new ArrayList<MagicalInk>();
        for (var item : bottles(inventory)) read(item).ifPresent(inks::add);
        return inks;
    }

    public List<ItemStack> bottles(Inventory inventory) {
        var out = new ArrayList<ItemStack>();
        if (inventory == null) return out;
        if (inventory instanceof PlayerInventory playerInv) {
            var offhand = playerInv.getItemInOffHand();
            if (isInk(offhand)) out.add(offhand);
            ItemStack[] storage = playerInv.getStorageContents();
            if (storage != null) {
                for (var item : storage) {
                    if (item == null || item == offhand) continue;
                    if (isInk(item)) out.add(item);
                }
            }
            return out;
        }
        ItemStack[] contents = inventory.getContents();
        if (contents == null) return out;
        for (var item : contents) if (isInk(item)) out.add(item);
        return out;
    }

    public boolean write(ItemStack item, MagicalInk spent) {
        if (item == null || spent == null || !isBottleType(item.getType())) return false;
        var current = read(item);
        if (current.isPresent() && current.get().element() != spent.element()) return false;
        if (current.isEmpty() && isInk(item)) return false;
        if (item.getType() != Material.POTION) item.setType(Material.POTION);
        var meta = item.getItemMeta();
        if (meta == null) return false;
        apply(meta, spent);
        item.setItemMeta(meta);
        style(item, spent);
        return true;
    }

    public static Optional<GlyphElement> parseElement(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "physical" -> Optional.of(GlyphElement.PHYSICAL);
            case "flame", "fire" -> Optional.of(GlyphElement.FLAME);
            case "frost" -> Optional.of(GlyphElement.FROST);
            case "arcane" -> Optional.of(GlyphElement.ARCANE);
            default -> Optional.empty();
        };
    }

    public static String displayName(GlyphElement element) {
        return switch (element) {
            case PHYSICAL -> "Physical Ink";
            case FLAME -> "Flame Ink";
            case FROST -> "Frost Ink";
            case ARCANE -> "Arcane Ink";
        };
    }

    public static Color tint(GlyphElement element) {
        return Color.fromRGB(
                Math.round(element.r() * 255),
                Math.round(element.g() * 255),
                Math.round(element.b() * 255));
    }

    private static boolean isBottleType(Material type) {
        return type == Material.POTION || type == Material.GLASS_BOTTLE;
    }

    private void apply(ItemMeta meta, MagicalInk ink) {
        var pdc = meta.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(elementKey, PersistentDataType.STRING, ink.element().name());
        pdc.set(remainingKey, PersistentDataType.INTEGER, ink.remaining());
        pdc.set(maxKey, PersistentDataType.INTEGER, ink.max());
        if (meta instanceof Damageable damageable) {
            damageable.setMaxDamage(ink.max());
            damageable.setDamage(ink.max() - ink.remaining());
        }
    }

    private static void style(ItemStack item, MagicalInk ink) {
        Color color = tint(ink.element());
        var name = Component.text(displayName(ink.element()), TextColor.color(color.asRGB()))
                .decoration(TextDecoration.ITALIC, false);
        var remaining = Component.text(ink.remaining() + "/" + ink.max())
                .color(TextColor.color(0x9aa0a6))
                .decoration(TextDecoration.ITALIC, false);
        item.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().customColor(color));
        item.setData(DataComponentTypes.ITEM_NAME, name);
        item.unsetData(DataComponentTypes.CUSTOM_NAME);
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(remaining)));
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.POTION_CONTENTS));
        item.unsetData(DataComponentTypes.CONSUMABLE);
    }
}
