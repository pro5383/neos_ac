package net.neos.neosac.raytrace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoxelRaytracer {

    public static class RaytraceResult {
        public final Block hitBlock;
        public final double distance;
        public final boolean hasClearLineOfSight;

        public RaytraceResult(Block hitBlock, double distance, boolean hasClearLineOfSight) {
            this.hitBlock = hitBlock;
            this.distance = distance;
            this.hasClearLineOfSight = hasClearLineOfSight;
        }
    }

    @Nullable
    public static RaytraceResult raytraceBlocks(@NotNull Location start, @NotNull Location target, double maxDist) {
        if (start.getWorld() == null) return null;

        World world = start.getWorld();
        Vector origin = start.toVector();
        Vector direction = target.toVector().subtract(origin);

        double totalDist = direction.length();
        if (totalDist <= 0 || totalDist > maxDist) {
            return new RaytraceResult(null, totalDist, false);
        }

        direction.normalize();

        int x = start.getBlockX();
        int y = start.getBlockY();
        int z = start.getBlockZ();

        int stepX = direction.getX() > 0 ? 1 : (direction.getX() < 0 ? -1 : 0);
        int stepY = direction.getY() > 0 ? 1 : (direction.getY() < 0 ? -1 : 0);
        int stepZ = direction.getZ() > 0 ? 1 : (direction.getZ() < 0 ? -1 : 0);

        double tMaxX = tMaxToBoundary(origin.getX(), direction.getX(), stepX);
        double tMaxY = tMaxToBoundary(origin.getY(), direction.getY(), stepY);
        double tMaxZ = tMaxToBoundary(origin.getZ(), direction.getZ(), stepZ);

        double tDeltaX = tDeltaForDirection(direction.getX());
        double tDeltaY = tDeltaForDirection(direction.getY());
        double tDeltaZ = tDeltaForDirection(direction.getZ());

        Block targetBlock = target.getBlock();
        double currentDist = 0;
        int iterations = 0;
        int maxIterations = 200;

        while (currentDist <= totalDist && iterations < maxIterations) {
            iterations++;

            Block block = world.getBlockAt(x, y, z);

            if (x == targetBlock.getX() && y == targetBlock.getY() && z == targetBlock.getZ()) {
                return new RaytraceResult(block, currentDist, true);
            }

            if (isOpaque(block)) {
                return new RaytraceResult(block, currentDist, false);
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    currentDist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    currentDist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    currentDist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    currentDist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }

            if (currentDist > maxDist) break;
        }

        return new RaytraceResult(targetBlock, currentDist, true);
    }

    public static boolean hasLineOfSight(@NotNull Location eye, int targetBlockX, int targetBlockY, int targetBlockZ, double maxReach) {
        if (eye.getWorld() == null) return true;

        Location targetCenter = new Location(eye.getWorld(),
                targetBlockX + 0.5, targetBlockY + 0.5, targetBlockZ + 0.5);

        RaytraceResult result = raytraceBlocks(eye, targetCenter, maxReach);
        return result != null && result.hasClearLineOfSight;
    }

    private static double tMaxToBoundary(double origin, double direction, int step) {
        if (step == 0) return Double.POSITIVE_INFINITY;

        double fractional = origin - Math.floor(origin);
        double nextBoundary;
        if (step > 0) {
            nextBoundary = 1.0 - fractional;
        } else {
            nextBoundary = fractional;
        }

        double absDir = Math.abs(direction);
        if (absDir < 1e-10) return Double.POSITIVE_INFINITY;

        return nextBoundary / absDir;
    }

    private static double tDeltaForDirection(double direction) {
        double absDir = Math.abs(direction);
        if (absDir < 1e-10) return Double.POSITIVE_INFINITY;
        return 1.0 / absDir;
    }

    private static boolean isOpaque(@NotNull Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type == Material.AIR || type.isAir()) return false;
        if (type == Material.WATER || type == Material.LAVA) return false;
        String name = type.name();
        if (name.contains("SAPLING") || name.contains("GRASS") || name.contains("FERN")
                || name.contains("FLOWER") || name.contains("MUSHROOM") || name.contains("VINE")
                || name.contains("TORCH") || name.contains("BUTTON") || name.contains("LEVER")
                || name.contains("SIGN") || name.contains("BANNER") || name.contains("CARPET")
                || name.contains("RAIL") || name.contains("LADDER") || name.contains("REPEATER")
                || name.contains("COMPARATOR") || name.contains("PRESSURE_PLATE")
                || name.contains("TRIPWIRE") || name.contains("BAMBOO") || name.contains("CORAL")
                || name.contains("KELP") || name.contains("SEAGRASS") || name.contains("FERN")) {
            return false;
        }
        try {
            return type.isOccluding();
        } catch (Throwable t) {
            return type.isSolid();
        }
    }
}
