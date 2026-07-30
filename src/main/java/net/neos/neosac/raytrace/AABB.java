package net.neos.neosac.raytrace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AABB {

    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    @Nullable
    public double[] intersectRay(double originX, double originY, double originZ,
                                  double dirX, double dirY, double dirZ,
                                  double maxDist) {
        double invDirX = dirX == 0 ? Double.POSITIVE_INFINITY : 1.0 / dirX;
        double invDirY = dirY == 0 ? Double.POSITIVE_INFINITY : 1.0 / dirY;
        double invDirZ = dirZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / dirZ;

        double tx1 = (minX - originX) * invDirX;
        double tx2 = (maxX - originX) * invDirX;
        double tmin = Math.min(tx1, tx2);
        double tmax = Math.max(tx1, tx2);

        double ty1 = (minY - originY) * invDirY;
        double ty2 = (maxY - originY) * invDirY;
        tmin = Math.max(tmin, Math.min(ty1, ty2));
        tmax = Math.min(tmax, Math.max(ty1, ty2));

        double tz1 = (minZ - originZ) * invDirZ;
        double tz2 = (maxZ - originZ) * invDirZ;
        tmin = Math.max(tmin, Math.min(tz1, tz2));
        tmax = Math.min(tmax, Math.max(tz1, tz2));

        if (tmax < 0 || tmin > tmax || tmin > maxDist) {
            return null;
        }
        double t = tmin < 0 ? tmax : tmin;
        if (t < 0 || t > maxDist) return null;

        return new double[] {
                originX + dirX * t,
                originY + dirY * t,
                originZ + dirZ * t,
                t
        };
    }

    public double distanceToCenter(double x, double y, double z) {
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @NotNull
    public AABB expand(double amount) {
        return new AABB(
                minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount
        );
    }

    @NotNull
    public AABB offset(double dx, double dy, double dz) {
        return new AABB(
                minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz
        );
    }

    @Override
    public String toString() {
        return String.format("AABB[%.3f,%.3f,%.3f -> %.3f,%.3f,%.3f]",
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    @NotNull
    public static AABB forBlock(int blockX, int blockY, int blockZ) {
        return new AABB(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
    }

    @NotNull
    public static AABB forBlockPartial(int blockX, int blockY, int blockZ, double height) {
        return new AABB(blockX, blockY, blockZ, blockX + 1, blockY + height, blockZ + 1);
    }

    @NotNull
    public static AABB fromBukkit(org.bukkit.util.BoundingBox bb) {
        return new AABB(bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ());
    }
}
