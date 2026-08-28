package dev.mintychochip.merlin.paper.enchanting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class AltarScanner {
    private static final Set<Material> PASSABLE_BLOCKS = Set.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.TORCH, Material.WALL_TORCH,
            Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH
    );

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

    /**
     * Exact 3D Digital Differential Analyzer (DDA) voxel raycast from start block to end block.
     * Every voxel pierced along the 3D ray segment is checked for solid block obstruction.
     */
    public static boolean hasLineOfSight(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        double startX = x1 + 0.5;
        double startY = y1 + 0.5;
        double startZ = z1 + 0.5;
        double endX = x2 + 0.5;
        double endY = y2 + 0.5;
        double endZ = z2 + 0.5;

        double dirX = endX - startX;
        double dirY = endY - startY;
        double dirZ = endZ - startZ;
        double dist = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dist <= 1.0) return true;

        dirX /= dist;
        dirY /= dist;
        dirZ /= dist;

        int currentX = x1;
        int currentY = y1;
        int currentZ = z1;

        int stepX = dirX > 0 ? 1 : (dirX < 0 ? -1 : 0);
        int stepY = dirY > 0 ? 1 : (dirY < 0 ? -1 : 0);
        int stepZ = dirZ > 0 ? 1 : (dirZ < 0 ? -1 : 0);

        double tDeltaX = (stepX != 0) ? Math.abs(1.0 / dirX) : Double.MAX_VALUE;
        double tDeltaY = (stepY != 0) ? Math.abs(1.0 / dirY) : Double.MAX_VALUE;
        double tDeltaZ = (stepZ != 0) ? Math.abs(1.0 / dirZ) : Double.MAX_VALUE;

        double tMaxX = (stepX > 0) ? ((currentX + 1.0 - startX) * tDeltaX) : ((startX - currentX) * tDeltaX);
        double tMaxY = (stepY > 0) ? ((currentY + 1.0 - startY) * tDeltaY) : ((startY - currentY) * tDeltaY);
        double tMaxZ = (stepZ > 0) ? ((currentZ + 1.0 - startZ) * tDeltaZ) : ((startZ - currentZ) * tDeltaZ);

        while (true) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentX += stepX;
                    if (tMaxX > dist) break;
                    tMaxX += tDeltaX;
                } else {
                    currentZ += stepZ;
                    if (tMaxZ > dist) break;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentY += stepY;
                    if (tMaxY > dist) break;
                    tMaxY += tDeltaY;
                } else {
                    currentZ += stepZ;
                    if (tMaxZ > dist) break;
                    tMaxZ += tDeltaZ;
                }
            }

            if (currentX == x2 && currentY == y2 && currentZ == z2) {
                break;
            }

            Block block = world.getBlockAt(currentX, currentY, currentZ);
            if (isObstructing(block)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isObstructing(Block block) {
        if (block == null || block.isEmpty()) return false;
        Material mat = block.getType();
        if (mat == null || PASSABLE_BLOCKS.contains(mat)) return false;
        return !mat.name().endsWith("_CARPET");
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
