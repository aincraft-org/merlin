package dev.mintychochip.merlin.paper.ritual;

import dev.mintychochip.merlin.api.glyph.GlyphElement;
import dev.mintychochip.merlin.api.ml.Label;
import org.bukkit.Material;

public final class RitualRecipe {
    private final Label word;
    private final Material material;
    private final GlyphElement school;
    private final Material catalyst;
    private final Material output;
    private final int baseYield;
    private final int maxYield;

    public RitualRecipe(Label word, Material material, GlyphElement school, Material catalyst,
                        Material output, int baseYield, int maxYield) {
        this.word = word;
        this.material = material;
        this.school = school;
        this.catalyst = catalyst;
        this.output = output;
        this.baseYield = baseYield;
        this.maxYield = maxYield;
    }

    public Label word() { return word; }
    public Material material() { return material; }
    public GlyphElement school() { return school; }
    public Material catalyst() { return catalyst; }
    public Material output() { return output; }
    public int baseYield() { return baseYield; }
    public int maxYield() { return maxYield; }
}
