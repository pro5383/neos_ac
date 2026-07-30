package net.neos.neosac.physics;

/**
 * Ванильные константы физики движения Minecraft (1.8 - 1.21).
 * Используются движком предсказания {@link net.neos.neosac.checks.simulation.SimulationCheck}
 * для построения диапазона допустимых значений скорости на каждый тик.
 */
public final class PhysicsConstants {

    private PhysicsConstants() {}

    // --- Вертикаль ---
    public static final double GRAVITY = 0.08D;
    public static final double SLOW_FALLING_GRAVITY = 0.01D;
    public static final double AIR_DRAG_Y = 0.9800000190734863D;
    public static final double TERMINAL_VELOCITY = -3.92D;
    public static final double JUMP_IMPULSE = 0.42D;
    public static final double JUMP_BOOST_PER_LEVEL = 0.1D;

    // Свободное падение из состояния покоя за один тик: (0 - g) * drag
    public static final double FREEFALL_FROM_ZERO = -GRAVITY * AIR_DRAG_Y;
    public static final double SLOW_FALL_MIN_Y = -SLOW_FALLING_GRAVITY * AIR_DRAG_Y;

    // --- Горизонталь ---
    public static final double AIR_DRAG_XZ = 0.9100000262260437D;
    public static final double GROUND_DRAG = 0.546D;      // slipperiness * AIR_DRAG_XZ при обычном блоке
    public static final float DEFAULT_SLIPPERINESS = 0.6F;
    public static final double MIN_MOTION = 0.003D;

    public static final double BASE_WALK_ACCEL = 0.1D;
    public static final double SPRINT_MULTIPLIER = 1.3D;
    public static final double SNEAK_MULTIPLIER = 0.3D;
    public static final double AIR_ACCEL_BASE = 0.02D;
    public static final double AIR_ACCEL_SPRINT = 0.026D;
    public static final double SPRINT_JUMP_BOOST = 0.2D;

    // Базовые ванильные потолки наземной скорости (accel / (1 - friction) при DEFAULT_SLIPPERINESS)
    public static final double GROUND_WALK_CAP = 0.221D;
    public static final double GROUND_SPRINT_CAP = 0.287D;

    // --- Блочные коэффициенты трения ---
    public static final float FRICTION_ICE = 0.98F;
    public static final float FRICTION_BLUE_ICE = 0.989F;
    public static final float FRICTION_SLIME = 0.8F;
    public static final float FRICTION_SOUL_SAND = 0.4F;

    // Ключевое масштабирование ввода клавиш перед нормализацией
    public static final double KEY_SCALE = 0.98D;
}
