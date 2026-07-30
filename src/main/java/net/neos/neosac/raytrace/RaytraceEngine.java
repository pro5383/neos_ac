package net.neos.neosac.raytrace;

import net.neos.neosac.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RaytraceEngine {

    public static final double MAX_BLOCK_REACH = 4.5;
    public static final double MAX_ENTITY_REACH = 3.0;
    public static final double MAX_ENTITY_REACH_CREATIVE = 5.0;

    public static boolean canInteractWithBlock(@NotNull Player player, int targetX, int targetY, int targetZ, double maxReach) {
        Location eye = player.getEyeLocation();

        Location target = new Location(eye.getWorld(), targetX + 0.5, targetY + 0.5, targetZ + 0.5);
        double distance = eye.distance(target);
        if (distance > maxReach) {
            return false;
        }

        return VoxelRaytracer.hasLineOfSight(eye, targetX, targetY, targetZ, maxReach);
    }

    public static double getEntityReachDistance(@NotNull Player player, @NotNull AABB target) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();

        double[] hit = target.intersectRay(
                eye.getX(), eye.getY(), eye.getZ(),
                look.getX(), look.getY(), look.getZ(),
                MAX_ENTITY_REACH_CREATIVE * 2
        );

        if (hit == null) {
            double closestDist = closestDistanceToAABB(eye.toVector(), target);
            return closestDist;
        }

        return hit[3];
    }

    public static boolean canHitEntity(@NotNull Player player, @NotNull LivingEntity target) {
        Location eye = player.getEyeLocation();
        org.bukkit.util.BoundingBox bb = target.getBoundingBox();
        AABB aabb = AABB.fromBukkit(bb);

        double distance = getEntityReachDistance(player, aabb);
        double maxReach = player.getGameMode().toString().equals("CREATIVE") ? MAX_ENTITY_REACH_CREATIVE : MAX_ENTITY_REACH;

        if (distance > maxReach + 0.5) {
            return false;
        }

        Vector look = eye.getDirection();
        Vector toTarget = target.getLocation().toVector().subtract(eye.toVector());
        double angle = look.angle(toTarget);

        return angle < Math.PI / 2 + 0.2;
    }

    public static double closestDistanceToAABB(@NotNull Vector point, @NotNull AABB aabb) {
        double dx = Math.max(aabb.minX - point.getX(), Math.max(0, point.getX() - aabb.maxX));
        double dy = Math.max(aabb.minY - point.getY(), Math.max(0, point.getY() - aabb.maxY));
        double dz = Math.max(aabb.minZ - point.getZ(), Math.max(0, point.getZ() - aabb.maxZ));
        return MathUtil.distance3D(dx, dy, dz);
    }

    public static double getAngleToTarget(@NotNull Player player, @NotNull Location target) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        Vector toTarget = target.toVector().subtract(eye.toVector());
        if (toTarget.lengthSquared() < 1e-6) return 0;
        toTarget.normalize();

        return look.angle(toTarget);
    }

    public static float getRotationDelta(float yaw1, float pitch1, float yaw2, float pitch2) {
        float deltaYaw = Math.abs(yaw1 - yaw2) % 360;
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
        float deltaPitch = Math.abs(pitch1 - pitch2);
        return (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
    }

    public static boolean isWithinViewCone(@NotNull Player player, @NotNull Location target, double maxAngle) {
        return getAngleToTarget(player, target) <= maxAngle;
    }
}
