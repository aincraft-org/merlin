package dev.mintychochip.merlin.paper.ink;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class FlowerGrind {
    private static final Map<Material, GlyphElement> MAPPINGS = Map.ofEntries(
            Map.entry(Material.TORCHFLOWER, GlyphElement.FLAME),
            Map.entry(Material.POPPY, GlyphElement.FLAME),
            Map.entry(Material.BLUE_ORCHID, GlyphElement.FROST),
            Map.entry(Material.CORNFLOWER, GlyphElement.FROST),
            Map.entry(Material.ALLIUM, GlyphElement.ARCANE),
            Map.entry(Material.WITHER_ROSE, GlyphElement.ARCANE),
            Map.entry(Material.LILAC, GlyphElement.ARCANE),
            Map.entry(Material.OXEYE_DAISY, GlyphElement.PHYSICAL),
            Map.entry(Material.DANDELION, GlyphElement.PHYSICAL),
            Map.entry(Material.AZURE_BLUET, GlyphElement.PHYSICAL),
            Map.entry(Material.SUNFLOWER, GlyphElement.PHYSICAL));

    private FlowerGrind() {}

    public static Optional<GlyphElement> elementFor(Material flower) {
        if (flower == null) return Optional.empty();
        return Optional.ofNullable(MAPPINGS.get(flower));
    }

    public static Map<Material, GlyphElement> mappings() {
        return MAPPINGS;
    }
}
