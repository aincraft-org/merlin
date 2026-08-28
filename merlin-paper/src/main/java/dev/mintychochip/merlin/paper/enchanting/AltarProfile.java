package dev.mintychochip.merlin.paper.enchanting;

import java.util.Map;
import org.bukkit.Material;

public record AltarProfile(
        double totalEterna,
        double totalQuanta,
        Map<Material, Integer> blockCounts
) {}
