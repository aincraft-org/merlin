package dev.mintychochip.merlin.paper.runtime;

import dev.mintychochip.merlin.api.glyph.CharmBind;
import org.bukkit.Material;

public final class CharmBinder {
    public static int level(CharmBind bind) { return bind.rank(); }

    public static boolean canHost(Material material) {
        return material != null && material.name().endsWith("_SWORD");
    }

    private CharmBinder() {}
}
