package net.neos.neosac.physics;

/**
 * Движок предсказания полёта на элитрах (fall flying).
 * <p>
 * Из скорости прошлого тика и направления взгляда строит предсказанную скорость текущего
 * тика по ванильной модели {@code LivingEntity#travel} (ветка fall-flying): гравитация с
 * подъёмной силой от косинуса тангажа, перевод падения в горизонталь, набор высоты при
 * задранном носе, аэродинамическая доводка вектора к взгляду и drag. Дополнительно, если
 * активен буст фейерверка, моделируется ванильная тяга ракеты — тогда предсказание остаётся
 * корректным и во время буста, а не глушится грацией.
 * <p>
 * Экземпляр переиспользуется: {@link #predict} перезаписывает результат, читаемый геттерами.
 */
public final class ElytraPredictor {

    // Ванильная тяга фейерверка: motion += look*0.1 + (look*1.5 - motion)*0.5.
    private static final double FIREWORK_BASE  = 0.1D;
    private static final double FIREWORK_TARGET = 1.5D;
    private static final double FIREWORK_PULL  = 0.5D;

    private double predictedDeltaX;
    private double predictedDeltaY;
    private double predictedDeltaZ;
    private double predictedHorizontal;

    /**
     * @param lastX,lastY,lastZ скорость прошлого тика
     * @param yaw,pitch         угол обзора текущего тика (градусы)
     * @param fireworkActive    активен ли буст фейерверка на этом тике
     */
    public void predict(double lastX, double lastY, double lastZ,
                        float yaw, float pitch, boolean fireworkActive) {

        double pitchRad = Math.toRadians(pitch);
        double yawRad   = Math.toRadians(-yaw);

        // Вектор взгляда (Entity#getViewVector): lookX = -sin(yaw)cos(pitch).
        double lookX = Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

        double horizLook  = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double lookLen    = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double horizSpeed = Math.sqrt(lastX * lastX + lastZ * lastZ);

        double mX = lastX, mY = lastY, mZ = lastZ;

        // Гравитация с подъёмной силой: g*(-1 + d2*0.75), где d2 = cos²(pitch)*min(1, |look|/0.4).
        double d2 = Math.cos(pitchRad);
        d2 = d2 * d2 * Math.min(1.0D, lookLen / PhysicsConstants.ELYTRA_LOOK_NORM);
        mY += PhysicsConstants.ELYTRA_GRAVITY * (-1.0D + d2 * 0.75D);

        // Падение переходит в горизонтальную скорость по взгляду.
        if (mY < 0.0D && horizLook > 0.0D) {
            double lift = mY * -0.1D * d2;
            mY += lift;
            mX += lookX * lift / horizLook;
            mZ += lookZ * lift / horizLook;
        }

        // Задранный нос (pitch < 0): набор высоты за счёт горизонтальной скорости.
        if (pitchRad < 0.0D && horizLook > 0.0D) {
            double push = horizSpeed * (-Math.sin(pitchRad)) * 0.04D;
            mY += push * 3.2D;
            mX -= lookX * push / horizLook;
            mZ -= lookZ * push / horizLook;
        }

        // Аэродинамическая доводка вектора скорости к направлению взгляда.
        if (horizLook > 0.0D) {
            mX += (lookX / horizLook * horizSpeed - mX) * 0.1D;
            mZ += (lookZ / horizLook * horizSpeed - mZ) * 0.1D;
        }

        // Тяга фейерверка — направленный импульс к взгляду.
        if (fireworkActive) {
            mX += lookX * FIREWORK_BASE + (lookX * FIREWORK_TARGET - mX) * FIREWORK_PULL;
            mY += lookY * FIREWORK_BASE + (lookY * FIREWORK_TARGET - mY) * FIREWORK_PULL;
            mZ += lookZ * FIREWORK_BASE + (lookZ * FIREWORK_TARGET - mZ) * FIREWORK_PULL;
        }

        mX *= PhysicsConstants.ELYTRA_DRAG_XZ;
        mY *= PhysicsConstants.ELYTRA_DRAG_Y;
        mZ *= PhysicsConstants.ELYTRA_DRAG_XZ;

        this.predictedDeltaX     = mX;
        this.predictedDeltaY     = mY;
        this.predictedDeltaZ     = mZ;
        this.predictedHorizontal = Math.sqrt(mX * mX + mZ * mZ);
    }

    public double getPredictedDeltaX()     { return predictedDeltaX; }
    public double getPredictedDeltaY()     { return predictedDeltaY; }
    public double getPredictedDeltaZ()     { return predictedDeltaZ; }
    public double getPredictedHorizontal() { return predictedHorizontal; }
}
