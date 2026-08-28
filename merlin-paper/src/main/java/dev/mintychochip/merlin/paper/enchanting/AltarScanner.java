package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class AltarScanner {
    private final AltarConfig config;

    public AltarScanner(AltarConfig config) {
        this.config = config;
    }

    public AltarProfile scan(Location tableLoc) {
        World world = tableLoc.getWorld();
        if (world == null) return new AltarProfile(0.0, 0.0, Map.of());
        int tx = tableLoc.getBlockX();
        int ty = tableLoc.getBlockY();
        int tz = tableLoc.getBlockZ();

        Map<Material, Integer> counts = new HashMap<>();
        for (int x = -config.radiusHorizontal(); x <= config.radiusHorizontal(); x++) {
            for (int z = -config.radiusHorizontal(); z <= config.radiusHorizontal(); z++) {
                for (int y = -config.radiusVerticalDown(); y <= config.radiusVerticalUp(); y++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block b = world.getBlockAt(tx + x, ty + y, tz + z);
                    Material mat = b.getType();
                    if (config.blockStats().containsKey(mat)) {
                        if (hasLineOfSight(world, tx, ty, tz, tx + x, ty + y, tz + z)) {
                            counts.merge(mat, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return calculateProfile(config, counts);
    }

    public static boolean hasLineOfSight(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        if (dx <= 1 && dy <= 1 && dz <= 1) return true;
        int midX = x1 + (x2 - x1) / 2;
        int midY = y1 + (y2 - y1) / 2;
        int midZ = z1 + (z2 - z1) / 2;
        if (midX == x1 && midY == y1 && midZ == z1) return true;
        Block midBlock = world.getBlockAt(midX, midY, midZ);
        return midBlock.isEmpty() || !midBlock.getType().isSolid();
    }

    public static AltarProfile calculateProfile(AltarConfig config, Map<Material, Integer> counts) {
        double totalEterna = 0.0;
        double totalQuanta = 0.0;
        for (var entry : counts.entrySet()) {
            AltarBlockStats stats = config.blockStats().get(entry.getKey());
            if (stats == null) continue;
            int count = entry.getValue();
            double eternaContribution = Math.min(stats.maxEternaCap(), count * stats.eterna());
            double quantaContribution = count * stats.quanta();
            if (stats.maxQuantaCap() >= 0) {
                quantaContribution = Math.min(stats.maxQuantaCap(), quantaContribution);
            } else {
                quantaContribution = Math.max(stats.maxQuantaCap(), quantaContribution);
            }
            totalEterna += eternaContribution;
            totalQuanta += quantaContribution;
        }
        return new AltarProfile(Math.max(0.0, totalEterna), totalQuanta, counts);
    }
}
