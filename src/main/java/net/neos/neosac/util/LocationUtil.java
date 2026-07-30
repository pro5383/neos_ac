package net.neos.neosac.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public final class LocationUtil {

    private LocationUtil() {}

    public static Location findSafeLocation(@NotNull Location loc) {
        if (loc == null || loc.getWorld() == null) return loc;

        Location current = loc.clone();
        if (isSafe(current)) {
            return current;
        }

        for (int dy = 0; dy <= 2; dy++) {
            Location up = current.clone().add(0, dy, 0);
            if (isSafe(up)) return up;
            Location down = current.clone().subtract(0, dy, 0);
            if (isSafe(down)) return down;
        }

        return current;
    }

    public static boolean isSafe(@NotNull Location loc) {
        if (loc.getWorld() == null) return true;
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();
        return isPassable(feet.getType()) && isPassable(head.getType()) && ground.getType().isSolid();
    }

    public static boolean isPassable(@NotNull Material material) {
        if (material == Material.AIR) return true;
        if (material.isAir()) return true;
        if (material == Material.WATER || material == Material.LAVA) return true;
        String name = material.name();
        if (name.contains("SAPLING") || name.contains("GRASS") || name.contains("FERN")
                || name.contains("FLOWER") || name.contains("MUSHROOM") || name.contains("VINE")
                || name.contains("LILY") || name.contains("TORCH") || name.contains("BUTTON")
                || name.contains("LEVER") || name.contains("SIGN") || name.contains("BANNER")
                || name.contains("CARPET") || name.contains("SNOW") && !name.equals("SNOW_BLOCK")
                || name.contains("RAIL") || name.contains("LADDER") || name.contains("REPEATER")
                || name.contains("COMPARATOR")) {
            return true;
        }
        return !material.isSolid();
    }

    public static double distance3D(@NotNull Location a, @NotNull Location b) {
        if (a.getWorld() != b.getWorld()) return Double.MAX_VALUE;
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double distance2D(@NotNull Location a, @NotNull Location b) {
        if (a.getWorld() != b.getWorld()) return Double.MAX_VALUE;
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double distanceY(@NotNull Location a, @NotNull Location b) {
        return Math.abs(a.getY() - b.getY());
    }

    public static boolean isOnGround(@NotNull Location loc) {
        if (loc.getWorld() == null) return true;
        Block below = loc.clone().subtract(0, 0.05, 0).getBlock();
        return below.getType().isSolid();
    }

    public static Block getBlockBelow(@NotNull Location loc) {
        return loc.clone().subtract(0, 1, 0).getBlock();
    }

    public static boolean isInLiquid(@NotNull Location loc) {
        if (loc.getWorld() == null) return false;
        Material feet = loc.getBlock().getType();
        Material head = loc.clone().add(0, 1, 0).getBlock().getType();
        return feet == Material.WATER || feet == Material.LAVA
                || head == Material.WATER || head == Material.LAVA;
    }

    public static boolean isOnClimbable(@NotNull Location loc) {
        if (loc.getWorld() == null) return false;
        Block block = loc.getBlock();
        Material type = block.getType();
        return type == Material.LADDER || type == Material.VINE
                || type == Material.SCAFFOLDING || type == Material.WEEPING_VINES
                || type == Material.TWISTING_VINES || type == Material.CAVE_VINES
                || type == Material.CAVE_VINES_PLANT || type == Material.WEEPING_VINES_PLANT
                || type == Material.TWISTING_VINES_PLANT;
    }

    public static Location clone(@NotNull Location loc) {
        return loc == null ? null : loc.clone();
    }
}
