package net.neos.neosac.util;

import org.jetbrains.annotations.NotNull;

public final class MathUtil {

    private MathUtil() {}

    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.98;
    public static final double GROUND_DRAG = 0.546;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double SPRINT_MULTIPLIER = 1.3;
    public static final double SNEAK_MULTIPLIER = 0.3;
    public static final double WALK_SPEED = 0.2;
    public static final double WATER_SLOWDOWN = 0.2;
    public static final double LAVA_SLOWDOWN = 0.5;
    public static final double STEP_HEIGHT = 0.6;

    public static double sqrt(double v) {
        return v <= 0 ? 0 : Math.sqrt(v);
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double distance2D(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double distance3D(double dx, double dy, double dz) {
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double distanceSquared2D(double dx, double dz) {
        return dx * dx + dz * dz;
    }

    public static double distanceSquared3D(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    public static float angleDifference(float a, float b) {
        float diff = Math.abs(a - b) % 360.0f;
        if (diff > 180.0f) {
            diff = 360.0f - diff;
        }
        return diff;
    }

    public static float yawDifference(float a, float b) {
        float diff = (a - b) % 360.0f;
        if (diff < -180.0f) diff += 360.0f;
        if (diff > 180.0f) diff -= 360.0f;
        return Math.abs(diff);
    }

    public static double round(double v, int decimals) {
        double mul = Math.pow(10, decimals);
        return Math.round(v * mul) / mul;
    }

    public static boolean approximatelyZero(double v, double epsilon) {
        return Math.abs(v) < epsilon;
    }

    public static long gcd(long a, long b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    public static int floor(double v) {
        int i = (int) v;
        return v < (double) i ? i - 1 : i;
    }

    public static int ceil(double v) {
        int i = (int) v;
        return v > (double) i ? i + 1 : i;
    }

    public static double average(@NotNull double[] arr) {
        if (arr.length == 0) return 0;
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    public static double stdDev(@NotNull double[] arr) {
        if (arr.length < 2) return 0;
        double avg = average(arr);
        double sum = 0;
        for (double v : arr) {
            double d = v - avg;
            sum += d * d;
        }
        return Math.sqrt(sum / arr.length);
    }
}
