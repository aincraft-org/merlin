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
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist <= 1.0) return true;

        int steps = (int) Math.ceil(dist * 3.0); // sample every ~0.33 blocks along ray
        int lastBx = x1;
        int lastBy = y1;
        int lastBz = z1;

        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int bx = (int) Math.floor(x1 + 0.5 + dx * t);
            int by = (int) Math.floor(y1 + 0.5 + dy * t);
            int bz = (int) Math.floor(z1 + 0.5 + dz * t);

            // Skip endpoints
            if ((bx == x1 && by == y1 && bz == z1) || (bx == x2 && by == y2 && bz == z2)) {
                continue;
            }
            if (bx == lastBx && by == lastBy && bz == lastBz) {
                continue;
            }
            lastBx = bx;
            lastBy = by;
            lastBz = bz;

            Block block = world.getBlockAt(bx, by, bz);
            if (!block.isEmpty() && block.getType().isSolid()) {
                return false;
            }
        }
        return true;
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
