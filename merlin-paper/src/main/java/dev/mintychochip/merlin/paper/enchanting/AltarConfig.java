package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public record AltarConfig(
        int radiusHorizontal,
        int radiusVerticalDown,
        int radiusVerticalUp,
        Map<Material, AltarBlockStats> blockStats
) {
    public static AltarConfig fromSection(ConfigurationSection section) {
        if (section == null) {
            return new AltarConfig(2, 1, 1, Map.of());
        }
        int radH = section.getInt("scan_radius_horizontal", 2);
        int radVDown = section.getInt("scan_radius_vertical_down", 1);
        int radVUp = section.getInt("scan_radius_vertical_up", 1);
        Map<Material, AltarBlockStats> stats = new HashMap<>();
        ConfigurationSection blocksSec = section.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String key : blocksSec.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat == null) continue;
                ConfigurationSection b = blocksSec.getConfigurationSection(key);
                if (b == null) continue;
                stats.put(mat, new AltarBlockStats(
                        b.getDouble("eterna", 0.0),
                        b.getDouble("quanta", 0.0),
                        b.getDouble("max_eterna_cap", 100.0),
                        b.getDouble("max_quanta_cap", 1.0)
                ));
            }
        }
        return new AltarConfig(radH, radVDown, radVUp, stats);
    }
}
