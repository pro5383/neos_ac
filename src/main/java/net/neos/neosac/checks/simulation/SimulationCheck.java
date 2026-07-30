package net.neos.neosac.checks.simulation;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.physics.PhysicsConstants;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Полноценный симуляционный движок анти-чита (area/range-based prediction).
 * <p>
 * Вместо жёсткого сравнения «реальная скорость == одно предсказанное значение» строится
 * <b>диапазон</b> допустимых значений вертикали и горизонтали на каждый тик с учётом всех
 * ванильных факторов (прыжок, гравитация, drag, трение блоков, вода, лианы, паутина, слизь,
 * мёд, эффекты зелий, удар головой о блок, шаг, приземление). Отклонение накапливается в
 * буфер; флаг ставится только когда буфер стабильно превышает порог — это исключает
 * ложные срабатывания на одиночном сетевом шуме.
 * <p>
 * Заменяет вертикальные (Fly / Hover / AirJump / Glide) и горизонтальные
 * (Speed / Bhop / Strafe / NoSlow) читы одним связным движком.
 */
public class SimulationCheck extends Check implements PacketAware {

    // ─── Пороги флагов ─────────────────────────────────────────────────────────
    private static final float VERTICAL_FLAG_THRESHOLD   = 0.75F;
    private static final float HORIZONTAL_FLAG_THRESHOLD  = 1.0F;

    // ─── Горизонталь ───────────────────────────────────────────────────────────
    private static final double SPRINT_JUMP_BOOST = PhysicsConstants.SPRINT_JUMP_BOOST;
    private static final double MIN_XZ_MOTION     = PhysicsConstants.MIN_MOTION;
    private static final double H_NOISE_FLOOR     = 0.002D;
    private static final double GROUND_CAP_LENIENCY          = 0.008D;
    private static final double GROUND_ACCEL_LENIENCY        = 0.015D;
    private static final double GROUND_VECTOR_ACCEL_LENIENCY = 0.020D;
    private static final double AIR_ACCEL_BASE   = PhysicsConstants.AIR_ACCEL_BASE;
    private static final double AIR_ACCEL_SPRINT = PhysicsConstants.AIR_ACCEL_SPRINT;
    private static final double KEY_SCALE        = PhysicsConstants.KEY_SCALE;

    // Импульс sprint-jump живёт ещё несколько тиков после отрыва от земли.
    private static final int SPRINT_JUMP_GRACE_TICKS = 8;
    // Грация после сетбэка/телепорта/спавна в миллисекундах.
    private static final long TELEPORT_GRACE_MS = 1000L;

    private enum Tag {
        JUMP, JUMP_START, JUMPING, IN_AIR,
        LANDING, LANDING_GRACE, HEAD_HIT,
        STEP_Y, STEP_DOWN,
        LADDER, LIQUID, WEB,
        TELEPORT_GRACE,
        SLIME_BLOCK, HONEY, ICE, SOUL_SAND,
        LEVITATION, SLOW_FALLING,
        GRAVITY_INVALID, FLY_DIP_RESET,
        SPRINT_JUMP, SNEAKING, USING_ITEM, BLOCK_PLACE,
        AIRBORNE_XZ, WEB_XZ, GROUND_XZ, WALL_TOUCH
    }

    private static final class State {
        // Вертикаль
        float   verticalBuffer;
        int     hoverTicks;
        int     landingTicks;
        double  slimeFallVelocity;
        boolean lastWasOnGround = true;
        int     lastAirTicks;
        // Горизонталь
        float  horizontalBuffer;
        int    overSpeedTicks;
        int    groundSpeedTicks;
        int    groundAccelTicks;
        double driftAccumulator;
        int    driftViolationTicks;
        int    lastForward;
        int    lastStrafe;
        int    sprintJumpTick = -999;
        int    tickCounter;
    }

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public SimulationCheck(NeosAC plugin) {
        super(plugin, "Simulation", CheckType.SIMULATION,
                "Полноценная симуляция физики движения (area-based prediction: Fly/Speed/Strafe/NoSlow)");
    }

    public void clear(UUID uuid) {
        states.remove(uuid);
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasPositionChanged()) return;

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        if (current == null || last == null || current.getWorld() == null || last.getWorld() == null) return;
        if (!current.getWorld().equals(last.getWorld())) return;

        // Ситуации, которые ванильный движок предсказания не покрывает — не проверяем.
        if (player.isGliding() || player.isInsideVehicle() || player.isRiptiding() || player.isFlying()) {
            resetState(data.getUuid());
            return;
        }
        if (data.getTicksExisted() < 20) return;

        final State st = states.computeIfAbsent(data.getUuid(), k -> new State());
        st.tickCounter++;

        final double deltaY      = data.getDeltaY();
        final double lastDeltaY  = data.getLastDeltaY();
        final double deltaXZ     = data.getDeltaXZ();
        final double lastDeltaXZ = data.getLastDeltaXZ();

        final boolean onGround = flying.isOnGround();
        final int airTicks     = data.getAirTicks();
        final int lastAirTicks = st.lastAirTicks;

        final boolean wasRecentlyOnGround = onGround || airTicks <= 1 || lastAirTicks == 0;
        final boolean wasRecentlyOnGroundForJump = onGround || airTicks <= 2 || lastAirTicks == 0;

        final int levitation  = data.getPotionLevel(PotionEffectType.LEVITATION);
        final int slowFalling = data.getPotionLevel(PotionEffectType.SLOW_FALLING);
        final int jumpBoost   = data.getPotionLevel(PotionEffectType.JUMP_BOOST);
        final int speedLevel  = data.getPotionLevel(PotionEffectType.SPEED);
        final double jumpImpulse = PhysicsConstants.JUMP_IMPULSE + jumpBoost * PhysicsConstants.JUMP_BOOST_PER_LEVEL;

        // Эффект зелья Levitation/SlowFalling делает вертикальный предикт бессмысленным для
        // некоторых веток — но мы всё равно строим диапазон, поэтому оставляем.
        if (deltaY < -0.05D) st.slimeFallVelocity = Math.min(st.slimeFallVelocity, deltaY);
        if (deltaY > 0.0D)   st.slimeFallVelocity = 0.0D;

        final boolean teleportGrace =
                (System.currentTimeMillis() - data.getLastSetbackTime()) < TELEPORT_GRACE_MS;

        final EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);

        // ── Окружение ────────────────────────────────────────────────────────
        if (!st.lastWasOnGround && onGround) { tags.add(Tag.LANDING); st.landingTicks = 3; }
        if (st.landingTicks > 0) { tags.add(Tag.LANDING_GRACE); st.landingTicks--; }

        if (data.isInLiquid())    tags.add(Tag.LIQUID);
        if (data.isInWeb())       tags.add(Tag.WEB);
        if (data.isOnClimbable()) tags.add(Tag.LADDER);
        if (data.isUnderBlock())  tags.add(Tag.HEAD_HIT);
        if (isNearHorizontalCollision(current)) tags.add(Tag.WALL_TOUCH);

        Material below = blockTypeBelow(current);
        if (below == Material.SLIME_BLOCK)  tags.add(Tag.SLIME_BLOCK);
        if (below == Material.HONEY_BLOCK)  tags.add(Tag.HONEY);
        if (isIce(below))                   tags.add(Tag.ICE);
        if (below == Material.SOUL_SAND || below == Material.SOUL_SOIL) tags.add(Tag.SOUL_SAND);

        if (levitation > 0)  tags.add(Tag.LEVITATION);
        if (slowFalling > 0) tags.add(Tag.SLOW_FALLING);
        if (teleportGrace)   tags.add(Tag.TELEPORT_GRACE);

        boolean inAir = !onGround && !tags.contains(Tag.LADDER) && !tags.contains(Tag.LIQUID);
        if (inAir) tags.add(Tag.IN_AIR);

        if (player.isHandRaised()) tags.add(Tag.USING_ITEM);
        if ((System.currentTimeMillis() - data.getLastBlockPlace()) < 150L) tags.add(Tag.BLOCK_PLACE);

        boolean isJumpMotion = wasRecentlyOnGroundForJump
                && deltaY > (jumpImpulse - 0.002D) && deltaY < (jumpImpulse + 0.002D);
        boolean isLikelyJump = wasRecentlyOnGroundForJump && deltaY > 0.3D;

        if ((airTicks <= 1 && wasRecentlyOnGround) || isJumpMotion || isLikelyJump) tags.add(Tag.JUMP);
        if (wasRecentlyOnGround && airTicks == 0 && deltaY > 0.0D) tags.add(Tag.JUMP_START);
        if (tags.contains(Tag.SLIME_BLOCK) && deltaY > 0.0D) tags.add(Tag.JUMP_START);
        if (isJumpMotion || isLikelyJump) tags.add(Tag.JUMP_START);
        if (!onGround && airTicks < 25 && deltaY > 0.0D) tags.add(Tag.JUMPING);

        if (onGround && deltaY > 0.0D && deltaY <= 0.601D) tags.add(Tag.STEP_Y);
        if (onGround && deltaY < 0.0D) tags.add(Tag.STEP_DOWN);
        if (st.lastWasOnGround && !onGround && deltaY < 0.0D) tags.add(Tag.STEP_DOWN);

        // Hover / fly-dip детекция
        if (tags.contains(Tag.IN_AIR) && Math.abs(deltaY) < 1.0E-7D
                && !tags.contains(Tag.LADDER) && !tags.contains(Tag.LIQUID)
                && !tags.contains(Tag.HEAD_HIT) && !tags.contains(Tag.TELEPORT_GRACE)) {
            st.hoverTicks++;
            if (st.hoverTicks > 3) { tags.add(Tag.GRAVITY_INVALID); st.verticalBuffer += 0.5F; }
        } else {
            st.hoverTicks = 0;
        }

        if (tags.contains(Tag.IN_AIR) && Math.abs(deltaY + 0.04D) < 1.0E-4D
                && !tags.contains(Tag.HEAD_HIT) && !tags.contains(Tag.TELEPORT_GRACE)) {
            tags.add(Tag.FLY_DIP_RESET);
        }

        // ── Симуляция ────────────────────────────────────────────────────────
        processVertical(data, st, tags, deltaY, lastDeltaY, onGround,
                wasRecentlyOnGround, airTicks, levitation, slowFalling, jumpImpulse);

        processHorizontal(data, st, tags, deltaXZ, lastDeltaXZ, onGround,
                wasRecentlyOnGround, airTicks, speedLevel);

        // ── Хвост тика ───────────────────────────────────────────────────────
        st.lastWasOnGround = onGround;
        st.lastAirTicks = airTicks;
    }

    // ─── Вертикаль ───────────────────────────────────────────────────────────
    private void processVertical(PlayerData data, State st, EnumSet<Tag> tags,
                                 double deltaY, double lastDeltaY, boolean onGround,
                                 boolean wasRecentlyOnGround, int airTicks,
                                 int levitation, int slowFalling, double jumpImpulse) {

        double predFallY;
        if (levitation > 0) {
            double target = 0.05D * levitation;
            predFallY = (lastDeltaY + (target - lastDeltaY) * 0.2D) * PhysicsConstants.AIR_DRAG_Y;
        } else {
            predFallY = (lastDeltaY - PhysicsConstants.GRAVITY) * PhysicsConstants.AIR_DRAG_Y;
        }
        if (slowFalling > 0) predFallY = Math.max(predFallY, PhysicsConstants.SLOW_FALL_MIN_Y);
        if (tags.contains(Tag.WEB)) predFallY *= 0.05D;
        if (predFallY < PhysicsConstants.TERMINAL_VELOCITY) predFallY = PhysicsConstants.TERMINAL_VELOCITY;

        double vDiff = Math.abs(deltaY - predFallY);

        // Множество допустимых альтернатив — берём минимальное отклонение.
        if (onGround || tags.contains(Tag.HEAD_HIT) || tags.contains(Tag.LANDING)) {
            vDiff = Math.min(vDiff, Math.abs(deltaY));
            vDiff = Math.min(vDiff, Math.abs(deltaY - PhysicsConstants.FREEFALL_FROM_ZERO));
        }
        if (tags.contains(Tag.JUMP) || tags.contains(Tag.JUMP_START) || wasRecentlyOnGround)
            vDiff = Math.min(vDiff, Math.abs(deltaY - jumpImpulse));
        if (tags.contains(Tag.JUMPING))
            vDiff = Math.min(vDiff, Math.abs(deltaY - PhysicsConstants.FREEFALL_FROM_ZERO));
        if (tags.contains(Tag.HEAD_HIT)) {
            if (deltaY > 0.0D && deltaY < jumpImpulse + 0.01D) vDiff = 0.0D;
            if (lastDeltaY > 0.0D && deltaY <= 0.0D) vDiff = 0.0D;
        }
        if (tags.contains(Tag.LADDER)) {
            vDiff = Math.min(vDiff, Math.abs(deltaY - 0.2D));
            vDiff = Math.min(vDiff, Math.abs(deltaY - 0.1176D));
            vDiff = Math.min(vDiff, Math.abs(deltaY));
            vDiff = Math.min(vDiff, Math.abs(deltaY + 0.15D));
        }
        if (tags.contains(Tag.LIQUID)) {
            double waterJump = (lastDeltaY + 0.04D) * 0.8D;
            double waterFall = (lastDeltaY - 0.04D) * 0.8D;
            vDiff = Math.min(vDiff, Math.abs(deltaY - waterJump));
            vDiff = Math.min(vDiff, Math.abs(deltaY - waterFall));
            vDiff = Math.min(vDiff, Math.abs(deltaY - 0.04D));
            vDiff = Math.min(vDiff, Math.abs(deltaY + 0.5D));
            vDiff = Math.min(vDiff, Math.abs(deltaY - 0.7D));
            vDiff = Math.min(vDiff, Math.abs(deltaY - 0.42D));
        }
        if (tags.contains(Tag.SLIME_BLOCK)) {
            double maxBounce = Math.abs(st.slimeFallVelocity) + 0.05D;
            if (tags.contains(Tag.JUMP_START) && deltaY > 0.0D && deltaY <= jumpImpulse + 0.02D) {
                vDiff = 0.0D;
            } else if (deltaY >= 0.0D && deltaY <= maxBounce) {
                vDiff = 0.0D;
            } else {
                vDiff = Math.min(vDiff, Math.abs(deltaY - Math.abs(st.slimeFallVelocity)));
                vDiff = Math.min(vDiff, Math.abs(deltaY));
            }
        }
        if (tags.contains(Tag.BLOCK_PLACE)) {
            if (deltaY <= 0.0D && deltaY >= -0.1D) vDiff = Math.min(vDiff, Math.abs(deltaY));
        }

        boolean validVerticalStep = onGround && deltaY <= 0.601D;
        if (tags.contains(Tag.GRAVITY_INVALID) || tags.contains(Tag.FLY_DIP_RESET)) validVerticalStep = false;

        if (validVerticalStep || tags.contains(Tag.STEP_DOWN)) {
            if (!tags.contains(Tag.GRAVITY_INVALID) && !tags.contains(Tag.FLY_DIP_RESET)) vDiff = 0.0D;
        }
        if (tags.contains(Tag.HEAD_HIT) && vDiff < 0.12D) {
            vDiff = 0.0D;
            st.verticalBuffer = Math.max(0, st.verticalBuffer - 0.4F);
        }
        if (tags.contains(Tag.TELEPORT_GRACE)) { vDiff = 0.0D; st.verticalBuffer = 0.0F; }

        if (vDiff > 0.01D) {
            float mult;
            if (tags.contains(Tag.GRAVITY_INVALID) || tags.contains(Tag.FLY_DIP_RESET)) mult = 6.0F;
            else if (tags.contains(Tag.IN_AIR) && Math.abs(deltaY) < 1.0E-5D)           mult = 25.0F;
            else if (tags.contains(Tag.IN_AIR) && vDiff > 0.05D)                         mult = 15.0F;
            else                                                                          mult = 5.0F;
            st.verticalBuffer += (float) (vDiff * mult);
        } else {
            float decay = tags.contains(Tag.IN_AIR) ? 0.025F : 0.1F;
            if (!tags.contains(Tag.GRAVITY_INVALID) && !tags.contains(Tag.FLY_DIP_RESET))
                st.verticalBuffer = Math.max(0, st.verticalBuffer - decay);
        }

        if (st.verticalBuffer >= VERTICAL_FLAG_THRESHOLD && vDiff > 0.01D) {
            String sub = tags.contains(Tag.GRAVITY_INVALID) ? "AirWalk"
                    : tags.contains(Tag.FLY_DIP_RESET)      ? "FlyDip"
                    : tags.contains(Tag.IN_AIR)             ? "Air"
                    : "Vertical";
            fail(data, "Симуляция (V/%s): buf=%.3f vDiff=%.4f dy=%.4f lastDy=%.4f air=%d",
                    sub, st.verticalBuffer, vDiff, deltaY, lastDeltaY, airTicks);
            st.verticalBuffer = Math.min(st.verticalBuffer, 1.5F);
        }
    }

    // ─── Горизонталь ─────────────────────────────────────────────────────────
    private void processHorizontal(PlayerData data, State st, EnumSet<Tag> tags,
                                   double deltaXZ, double lastDeltaXZ, boolean onGround,
                                   boolean wasRecentlyOnGround, int airTicks, int speedLevel) {

        if (deltaXZ < MIN_XZ_MOTION && lastDeltaXZ < MIN_XZ_MOTION) {
            st.horizontalBuffer = Math.max(0F, st.horizontalBuffer - 0.1F);
            st.driftAccumulator *= 0.85D;
            return;
        }

        if (tags.contains(Tag.TELEPORT_GRACE)) {
            st.horizontalBuffer = Math.max(0F, st.horizontalBuffer - 0.3F);
            st.overSpeedTicks = 0;
            st.driftAccumulator *= 0.7D;
            return;
        }

        if (tags.contains(Tag.WALL_TOUCH)) {
            st.horizontalBuffer = Math.max(0F, st.horizontalBuffer - 0.12F);
            st.overSpeedTicks = 0;
            st.driftAccumulator *= 0.65D;
        }

        boolean isSprinting = data.isSprinting();

        // Sprint-jump грация: тег держится SPRINT_JUMP_GRACE_TICKS тиков после отрыва.
        if (isSprinting && (tags.contains(Tag.JUMP_START)
                || (airTicks > 0 && airTicks <= 1 && wasRecentlyOnGround))) {
            st.sprintJumpTick = st.tickCounter;
        }
        if ((st.tickCounter - st.sprintJumpTick) <= SPRINT_JUMP_GRACE_TICKS) tags.add(Tag.SPRINT_JUMP);

        if (data.isSneaking()) tags.add(Tag.SNEAKING);
        if (!onGround)         tags.add(Tag.AIRBORNE_XZ);
        if (data.isInWeb())    tags.add(Tag.WEB_XZ);

        double maxAllowed = computeMaxHorizontalSpeed(data, speedLevel);
        if (speedLevel > 0) maxAllowed += speedLevel * 0.03D;
        if (tags.contains(Tag.SPRINT_JUMP)) maxAllowed += SPRINT_JUMP_BOOST + 0.05D;
        if (tags.contains(Tag.USING_ITEM))  maxAllowed *= 0.35D;
        if (tags.contains(Tag.SOUL_SAND))   maxAllowed = Math.min(maxAllowed, 0.13D);
        if (tags.contains(Tag.HONEY))       maxAllowed = Math.min(maxAllowed, 0.12D);
        if (tags.contains(Tag.WEB_XZ))      maxAllowed = Math.min(maxAllowed, 0.05D);
        if (tags.contains(Tag.LADDER))      maxAllowed = Math.min(maxAllowed, 0.15D);
        if (tags.contains(Tag.SNEAKING))    maxAllowed = Math.min(maxAllowed, 0.13D);
        if (tags.contains(Tag.SLIME_BLOCK)) maxAllowed += 0.15D;

        // Сохранение инерции: скорость не может внезапно превысить (прошлая * трение + импульс).
        double friction = tags.contains(Tag.AIRBORNE_XZ)
                ? PhysicsConstants.AIR_DRAG_XZ : computeGroundFriction(data);
        double momentum = (lastDeltaXZ * friction) + 0.05D;
        if (tags.contains(Tag.SPRINT_JUMP)) momentum += SPRINT_JUMP_BOOST + 0.05D;
        maxAllowed = Math.max(maxAllowed, momentum);

        if (tags.contains(Tag.LANDING) || tags.contains(Tag.LANDING_GRACE)) maxAllowed += 0.2D;
        if (tags.contains(Tag.STEP_Y)) maxAllowed += 0.1D;
        if (tags.contains(Tag.ICE))    maxAllowed += 0.15D;

        // ── Воздушный strafe + воздушный максимум ────────────────────────────
        if (tags.contains(Tag.AIRBORNE_XZ) && !tags.contains(Tag.SLIME_BLOCK)) {
            double airAccel = isSprinting ? AIR_ACCEL_SPRINT : AIR_ACCEL_BASE;
            if (speedLevel > 0) airAccel += speedLevel * 0.006D;
            double maxAirSpeed = (lastDeltaXZ * PhysicsConstants.AIR_DRAG_XZ) + airAccel + 0.08D;
            if (tags.contains(Tag.SPRINT_JUMP)) maxAirSpeed += SPRINT_JUMP_BOOST + 0.05D;

            double[] inferred = inferMoveRelativeAccel(data, airAccel, st);
            double predictedVX = data.getLastDeltaX() * PhysicsConstants.AIR_DRAG_XZ + inferred[0];
            double predictedVZ = data.getLastDeltaZ() * PhysicsConstants.AIR_DRAG_XZ + inferred[1];

            double accelX = data.getDeltaX() - data.getLastDeltaX() * PhysicsConstants.AIR_DRAG_XZ;
            double accelZ = data.getDeltaZ() - data.getLastDeltaZ() * PhysicsConstants.AIR_DRAG_XZ;
            double airVectorAccel = Math.hypot(accelX, accelZ);

            double vecDev = Math.hypot(data.getDeltaX() - predictedVX, data.getDeltaZ() - predictedVZ);

            double driftTolerance = 0.055D;
            if (tags.contains(Tag.SPRINT_JUMP) || tags.contains(Tag.JUMP) || tags.contains(Tag.JUMP_START))
                driftTolerance += 0.12D;
            if (tags.contains(Tag.WALL_TOUCH)) driftTolerance += 0.12D;

            double excessDrift = Math.max(0.0D, vecDev - driftTolerance);
            st.driftAccumulator = st.driftAccumulator * 0.88D + excessDrift;
            if (st.driftAccumulator > 0.18D) st.driftViolationTicks++;
            else st.driftViolationTicks = Math.max(0, st.driftViolationTicks - 1);

            double maxAirVectorAccel = airAccel + 0.12D;
            if (tags.contains(Tag.SPRINT_JUMP) || tags.contains(Tag.JUMP) || tags.contains(Tag.JUMP_START))
                maxAirVectorAccel += SPRINT_JUMP_BOOST + 0.08D;

            if (airVectorAccel > maxAirVectorAccel && !tags.contains(Tag.WEB_XZ)
                    && !tags.contains(Tag.LADDER) && !tags.contains(Tag.WALL_TOUCH)) {
                st.horizontalBuffer += (float) ((airVectorAccel - maxAirVectorAccel) * 15.0D);
                if (st.horizontalBuffer >= HORIZONTAL_FLAG_THRESHOLD && airTicks > 1) {
                    fail(data, "Симуляция (AirStrafe): vAccel=%.4f > %.4f air=%d",
                            airVectorAccel, maxAirVectorAccel, airTicks);
                    st.horizontalBuffer = Math.min(st.horizontalBuffer, 2.0F);
                }
            }

            if (st.driftAccumulator > 0.35D && st.driftViolationTicks >= 4
                    && !tags.contains(Tag.WEB_XZ) && !tags.contains(Tag.LADDER)
                    && !tags.contains(Tag.WALL_TOUCH)) {
                fail(data, "Симуляция (AirDrift): drift=%.4f dTicks=%d pred=(%.3f,%.3f) got=(%.3f,%.3f)",
                        st.driftAccumulator, st.driftViolationTicks, predictedVX, predictedVZ,
                        data.getDeltaX(), data.getDeltaZ());
                st.driftAccumulator = 0.12D;
                st.driftViolationTicks = 0;
            }

            if (maxAirSpeed < maxAllowed && !tags.contains(Tag.WEB_XZ) && !tags.contains(Tag.LADDER))
                maxAllowed = maxAirSpeed;
        } else {
            st.driftAccumulator *= 0.75D;
            st.driftViolationTicks = Math.max(0, st.driftViolationTicks - 1);
        }

        // ── Наземная скорость (Speed / Bhop) ─────────────────────────────────
        GroundSpeedResult g = checkGroundSpeed(data, tags, deltaXZ, lastDeltaXZ, onGround, speedLevel);
        if (g.applicable) {
            if (g.speedDiff > H_NOISE_FLOOR) { tags.add(Tag.GROUND_XZ); st.groundSpeedTicks++; }
            else st.groundSpeedTicks = Math.max(0, st.groundSpeedTicks - 1);

            if (g.accelDiff > H_NOISE_FLOOR) st.groundAccelTicks++;
            else st.groundAccelTicks = Math.max(0, st.groundAccelTicks - 1);

            if ((st.groundSpeedTicks >= 2 && st.groundAccelTicks >= 1) || g.speedDiff > 0.12D) {
                double groundDiff = Math.max(g.speedDiff, Math.max(g.accelDiff, g.vectorAccelDiff));
                float groundMult = groundDiff > 0.1D ? 35.0F : 20.0F;
                st.horizontalBuffer += (float) (groundDiff * groundMult);
            } else {
                st.horizontalBuffer = Math.max(0F, st.horizontalBuffer - 0.05F);
            }

            if (st.horizontalBuffer >= HORIZONTAL_FLAG_THRESHOLD) {
                fail(data, "Симуляция (Ground): buf=%.3f XZ=%.4f cap=%.4f accel=%.4f accelMax=%.4f",
                        st.horizontalBuffer, deltaXZ, g.speedCap, g.accel, g.accelCap);
                st.horizontalBuffer = Math.min(st.horizontalBuffer, 2.0F);
            }
            return;
        } else {
            st.groundSpeedTicks = 0;
            st.groundAccelTicks = 0;
        }

        // ── Обобщённый потолок скорости ──────────────────────────────────────
        double hDiff = deltaXZ - maxAllowed;
        if (hDiff <= H_NOISE_FLOOR) {
            st.overSpeedTicks = 0;
            float hDecay = tags.contains(Tag.AIRBORNE_XZ) ? 0.02F : 0.08F;
            st.horizontalBuffer = Math.max(0F, st.horizontalBuffer - hDecay);
            return;
        }

        if (hDiff > 1.25D) st.overSpeedTicks = Math.max(st.overSpeedTicks, 2);
        st.overSpeedTicks++;
        if (st.overSpeedTicks < 2) return;

        float hMult = hDiff > 0.5D ? 30.0F : hDiff > 0.15D ? 15.0F : 6.0F;
        st.horizontalBuffer += (float) (hDiff * hMult);

        if (st.horizontalBuffer >= HORIZONTAL_FLAG_THRESHOLD) {
            fail(data, "Симуляция (Speed): buf=%.3f hDiff=%.4f XZ=%.4f max=%.4f air=%d",
                    st.horizontalBuffer, hDiff, deltaXZ, maxAllowed, airTicks);
            st.horizontalBuffer = Math.min(st.horizontalBuffer, 2.0F);
        }
    }

    // ─── Вывод направления клавиш из вектора ускорения ───────────────────────
    private double[] inferMoveRelativeAccel(PlayerData data, double friction, State st) {
        double dvX = data.getDeltaX() - data.getLastDeltaX() * PhysicsConstants.AIR_DRAG_XZ;
        double dvZ = data.getDeltaZ() - data.getLastDeltaZ() * PhysicsConstants.AIR_DRAG_XZ;
        double dvMag = Math.hypot(dvX, dvZ);

        double yawRad = Math.toRadians(data.getYaw());
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        int forward, strafe;
        if (dvMag > 0.003D) {
            double localForward = -dvX * sinYaw + dvZ * cosYaw;
            double localStrafe  =  dvX * cosYaw + dvZ * sinYaw;
            forward = (int) Math.signum(localForward);
            strafe  = (int) Math.signum(localStrafe);
            st.lastForward = forward;
            st.lastStrafe  = strafe;
        } else {
            forward = st.lastForward;
            strafe  = st.lastStrafe;
        }

        double f = forward * KEY_SCALE;
        double s = strafe  * KEY_SCALE;
        double len = Math.sqrt(f * f + s * s);
        if (len < 0.0001D) return new double[]{0.0D, 0.0D};

        double scale = friction / Math.max(1.0D, len);
        f *= scale;
        s *= scale;
        return new double[]{ s * cosYaw - f * sinYaw, f * cosYaw + s * sinYaw };
    }

    // ─── Наземная скорость ───────────────────────────────────────────────────
    private GroundSpeedResult checkGroundSpeed(PlayerData data, EnumSet<Tag> tags,
                                               double deltaXZ, double lastDeltaXZ,
                                               boolean onGround, int speedLevel) {
        GroundSpeedResult r = new GroundSpeedResult();
        if (!onGround || tags.contains(Tag.IN_AIR)) return r;
        // На тике приземления sprint-jump XZ — это ещё воздушная инерция, не наземный разгон.
        if (tags.contains(Tag.SPRINT_JUMP) && (tags.contains(Tag.JUMP_START) || tags.contains(Tag.STEP_Y)))
            return r;

        r.applicable = true;

        double attributeScale  = Math.max(0.2D, data.getWalkSpeed() / 0.1D);
        double speedMultiplier = 1.0D;
        if (speedLevel > 0) speedMultiplier += 0.20D * speedLevel;
        int slownessLevel = data.getPotionLevel(PotionEffectType.SLOWNESS);
        if (slownessLevel > 0) speedMultiplier *= Math.max(0.0D, 1.0D - 0.15D * slownessLevel);

        double speedCap = (data.isSprinting()
                ? PhysicsConstants.GROUND_SPRINT_CAP : PhysicsConstants.GROUND_WALK_CAP)
                * attributeScale * speedMultiplier + GROUND_CAP_LENIENCY;

        if (tags.contains(Tag.SNEAKING))  speedCap = Math.min(speedCap, 0.15D + GROUND_CAP_LENIENCY);
        if (tags.contains(Tag.USING_ITEM))speedCap = Math.min(speedCap, 0.12D + GROUND_CAP_LENIENCY);
        if (tags.contains(Tag.SOUL_SAND)) speedCap = Math.min(speedCap, 0.13D + GROUND_CAP_LENIENCY);
        if (tags.contains(Tag.HONEY))     speedCap = Math.min(speedCap, 0.12D + GROUND_CAP_LENIENCY);
        if (tags.contains(Tag.ICE))       speedCap += 0.15D;

        double friction   = computeGroundFriction(data);
        double accel      = Math.max(0.0D, deltaXZ - lastDeltaXZ * friction);
        double rawAccelCap= computeGroundAccelCap(data, speedLevel, friction, tags);
        double accelCap   = rawAccelCap + GROUND_ACCEL_LENIENCY;

        double accelX = data.getDeltaX() - data.getLastDeltaX() * friction;
        double accelZ = data.getDeltaZ() - data.getLastDeltaZ() * friction;
        double vectorAccel = Math.hypot(accelX, accelZ);
        double vectorAccelCap = rawAccelCap + GROUND_VECTOR_ACCEL_LENIENCY;

        r.speedCap = speedCap;
        r.accelCap = accelCap;
        r.accel = accel;
        r.speedDiff = deltaXZ - speedCap;
        r.accelDiff = accel - accelCap;
        r.vectorAccelDiff = vectorAccel - vectorAccelCap;
        return r;
    }

    private double computeGroundAccelCap(PlayerData data, int speedLevel, double friction, EnumSet<Tag> tags) {
        double base = data.getWalkSpeed();
        if (speedLevel > 0) base *= 1.0D + 0.20D * speedLevel;
        int slownessLevel = data.getPotionLevel(PotionEffectType.SLOWNESS);
        if (slownessLevel > 0) base *= Math.max(0.0D, 1.0D - 0.15D * slownessLevel);
        if (data.isSprinting()) base *= 1.30D;
        if (tags.contains(Tag.SNEAKING)) base *= 0.30D;
        if (tags.contains(Tag.USING_ITEM)) base *= 0.30D;
        double f3 = friction * friction * friction;
        if (f3 <= 1.0E-5D) return 0.0D;
        return base * (0.16277136D / f3);
    }

    private double computeMaxHorizontalSpeed(PlayerData data, int speedLevel) {
        double base = 0.2158D; // ванильный потолок обычной ходьбы (walk)
        if (data.isSprinting()) base = 0.2806D;
        double attributeScale = Math.max(0.2D, data.getWalkSpeed() / 0.1D);
        base *= attributeScale;
        if (speedLevel > 0) base *= 1.0D + 0.20D * speedLevel;
        int slownessLevel = data.getPotionLevel(PotionEffectType.SLOWNESS);
        if (slownessLevel > 0) base *= Math.max(0.0D, 1.0D - 0.15D * slownessLevel);
        return base;
    }

    /** Эффективное горизонтальное трение = slipperiness * AIR_DRAG_XZ (по самому скользкому блоку под ногами). */
    private double computeGroundFriction(PlayerData data) {
        return getSlipperiness(data.getCurrentLocation()) * PhysicsConstants.AIR_DRAG_XZ;
    }

    private float getSlipperiness(Location loc) {
        if (loc == null || loc.getWorld() == null) return PhysicsConstants.DEFAULT_SLIPPERINESS;
        float maxFriction = PhysicsConstants.DEFAULT_SLIPPERINESS;
        double startX = loc.getX() - 0.3;
        double startZ = loc.getZ() - 0.3;
        for (double x = startX; x <= startX + 0.6; x += 0.3) {
            for (double z = startZ; z <= startZ + 0.6; z += 0.3) {
                try {
                    Material type = new Location(loc.getWorld(), x, loc.getY() - 1.0, z).getBlock().getType();
                    float f = frictionOf(type);
                    if (f > maxFriction) maxFriction = f;
                } catch (Exception ignored) {}
            }
        }
        return maxFriction;
    }

    private float frictionOf(Material type) {
        if (type == Material.BLUE_ICE) return PhysicsConstants.FRICTION_BLUE_ICE;
        if (isIce(type)) return PhysicsConstants.FRICTION_ICE;
        if (type == Material.SLIME_BLOCK) return PhysicsConstants.FRICTION_SLIME;
        if (type == Material.SOUL_SAND || type == Material.SOUL_SOIL || type == Material.HONEY_BLOCK)
            return PhysicsConstants.FRICTION_SOUL_SAND;
        return PhysicsConstants.DEFAULT_SLIPPERINESS;
    }

    private boolean isIce(Material type) {
        return type == Material.ICE || type == Material.PACKED_ICE
                || type == Material.BLUE_ICE || type == Material.FROSTED_ICE;
    }

    private Material blockTypeBelow(Location loc) {
        if (loc == null || loc.getWorld() == null) return Material.AIR;
        try {
            return loc.clone().subtract(0, 0.5, 0).getBlock().getType();
        } catch (Exception e) {
            return Material.AIR;
        }
    }

    // ─── Горизонтальная коллизия (стена) ─────────────────────────────────────
    private boolean isNearHorizontalCollision(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ(), r = 0.36D;
        return hasSolid(loc, x + r, y + 0.2D, z) || hasSolid(loc, x - r, y + 0.2D, z)
                || hasSolid(loc, x, y + 0.2D, z + r) || hasSolid(loc, x, y + 0.2D, z - r)
                || hasSolid(loc, x + r, y + 1.0D, z) || hasSolid(loc, x - r, y + 1.0D, z)
                || hasSolid(loc, x, y + 1.0D, z + r) || hasSolid(loc, x, y + 1.0D, z - r);
    }

    private boolean hasSolid(Location ref, double x, double y, double z) {
        try {
            Block b = new Location(ref.getWorld(), x, y, z).getBlock();
            return b.getType().isSolid();
        } catch (Exception e) {
            return false;
        }
    }

    private void resetState(UUID uuid) {
        State st = states.get(uuid);
        if (st != null) {
            st.verticalBuffer = 0F;
            st.horizontalBuffer = 0F;
            st.hoverTicks = 0;
            st.driftAccumulator = 0;
            st.driftViolationTicks = 0;
            st.sprintJumpTick = -999;
        }
    }

    private static final class GroundSpeedResult {
        boolean applicable;
        double speedCap, accelCap, accel;
        double speedDiff, accelDiff, vectorAccelDiff;
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
