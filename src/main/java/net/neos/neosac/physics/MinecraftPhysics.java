package net.neos.neosac.physics;

import net.neos.neosac.NeosAC;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.util.LocationUtil;
import net.neos.neosac.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class MinecraftPhysics {

    private final NeosAC plugin;
    private final PlayerData data;

    private double velocityX;
    private double velocityY;
    private double velocityZ;

    private double simulatedX;
    private double simulatedY;
    private double simulatedZ;

    public MinecraftPhysics(@NotNull NeosAC plugin, @NotNull PlayerData data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void reset(Location location) {
        if (location == null) return;
        this.simulatedX = location.getX();
        this.simulatedY = location.getY();
        this.simulatedZ = location.getZ();
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
    }

    public void simulateTick(Location current, boolean onGround, boolean sprinting, boolean sneaking) {
        if (current == null) return;
        Player player = data.getPlayer();
        if (player == null) return;

        double grav = MathUtil.GRAVITY;
        double drag = MathUtil.DRAG;

        PotionEffect speedEffect = player.getPotionEffect(PotionEffectType.SPEED);
        PotionEffect slownessEffect = player.getPotionEffect(PotionEffectType.SLOWNESS);
        PotionEffect jumpBoost = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        PotionEffect levitation = player.getPotionEffect(PotionEffectType.LEVITATION);
        PotionEffect slowFalling = player.getPotionEffect(PotionEffectType.SLOW_FALLING);

        if (levitation != null) {
            velocityY += 0.045 * (levitation.getAmplifier() + 1);
        }

        if (slowFalling != null) {
            grav = Math.min(grav, 0.01);
            velocityY = Math.min(velocityY, -grav);
        }

        if (jumpBoost != null && onGround && data.wasOnGround() == false) {
        }

        if (!onGround && !data.isInLiquid() && !data.isOnClimbable() && !data.isGliding() && !data.isFlying()) {
            velocityY -= grav;
            velocityY *= drag;
        }

        if (onGround) {
            double friction = getBlockFriction(current);
            velocityX *= friction;
            velocityZ *= friction;
        } else {
            velocityX *= drag;
            velocityZ *= drag;
        }

        if (data.isInLiquid()) {
            velocityY = velocityY * 0.8 - 0.02;
            velocityX *= 0.8;
            velocityZ *= 0.8;

            if (current.getY() < data.getLastLocation().getY() + 0.1 && data.isSneaking()) {
                velocityY = 0.0;
            }
        }

        if (data.isOnClimbable()) {
            if (data.isSneaking()) {
                velocityY = 0;
            } else if (velocityY < 0.1176) {
                velocityY = 0.1176;
            }
        }

        if (data.isInWeb()) {
            velocityX *= 0.25;
            velocityY *= 0.05;
            velocityZ *= 0.25;
        }

        double speedMultiplier = 1.0;
        if (sprinting && onGround) {
            speedMultiplier *= MathUtil.SPRINT_MULTIPLIER;
        }
        if (sneaking) {
            speedMultiplier *= MathUtil.SNEAK_MULTIPLIER;
        }

        if (speedEffect != null) {
            speedMultiplier *= 1.0 + 0.2 * (speedEffect.getAmplifier() + 1);
        }
        if (slownessEffect != null) {
            speedMultiplier *= 1.0 - 0.15 * (slownessEffect.getAmplifier() + 1);
        }

        double dx = current.getX() - data.getLastLocation().getX();
        double dz = current.getZ() - data.getLastLocation().getZ();

        simulatedX += velocityX * speedMultiplier;
        simulatedY += velocityY;
        simulatedZ += velocityZ * speedMultiplier;

        velocityX = dx * (onGround ? 0.91 : drag);
        velocityZ = dz * (onGround ? 0.91 : drag);

        data.setSimulatedY(simulatedY);
    }

    private double getBlockFriction(Location loc) {
        if (loc == null || loc.getWorld() == null) return 0.6;
        Block below = loc.clone().subtract(0, 0.5, 0).getBlock();
        Material type = below.getType();
        if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE) {
            return type == Material.BLUE_ICE ? 0.989 : 0.98;
        }
        if (type == Material.SLIME_BLOCK) {
            return 0.8;
        }
        if (type == Material.SOUL_SAND) {
            return 0.4;
        }
        return 0.6;
    }

    public double getMaxHorizontalSpeed(Player player, boolean sprinting, boolean sneaking) {
        double base = MathUtil.WALK_SPEED;
        if (sprinting) base *= MathUtil.SPRINT_MULTIPLIER;
        if (sneaking) base *= MathUtil.SNEAK_MULTIPLIER;

        PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            base *= 1.0 + 0.2 * (speed.getAmplifier() + 1);
        }
        PotionEffect slowness = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slowness != null) {
            base *= 1.0 - 0.15 * (slowness.getAmplifier() + 1);
        }

        if (data.isInLiquid()) {
            base *= 0.5;
        }
        if (data.isInWeb()) {
            base *= 0.25;
        }
        if (data.isOnGround() && data.getCurrentLocation() != null) {
            Block below = data.getCurrentLocation().clone().subtract(0, 0.5, 0).getBlock();
            if (below.getType() == Material.SOUL_SAND) {
                base *= 0.4;
            }
        }

        if (!data.isOnGround()) {
            base += sprinting ? 0.28 : 0.15;
        } else if (data.getGroundTicks() <= 3) {
            base += sprinting ? 0.17 : 0.10;
        }

        base += 0.02;

        base *= 1.05;

        return base;
    }

    public boolean isVerticalMoveLegit(double deltaY, boolean onGround, boolean wasOnGround) {
        return isVerticalMoveLegit(deltaY, onGround, wasOnGround, 0.0);
    }

    public boolean isVerticalMoveLegit(double deltaY, boolean onGround, boolean wasOnGround, double jumpBoostBonus) {
        if (wasOnGround && !onGround && deltaY > 0) {
            return deltaY <= MathUtil.JUMP_VELOCITY + jumpBoostBonus + 0.1;
        }
        if (onGround && !wasOnGround) {
            if (deltaY > 0) {
                return deltaY <= MathUtil.STEP_HEIGHT + 0.02;
            }
            return deltaY >= -4.0;
        }
        if (!onGround && deltaY < 0) {
            double maxFallSpeed = -MathUtil.GRAVITY * (1 - Math.pow(MathUtil.DRAG, data.getAirTicks())) / (1 - MathUtil.DRAG);
            return deltaY >= maxFallSpeed - 0.1;
        }
        if (onGround && Math.abs(deltaY) > 0.1) {
            if (deltaY > 0) {
                return deltaY <= MathUtil.STEP_HEIGHT + 0.02;
            }
            return deltaY >= -4.0;
        }
        return true;
    }

    public double getMaxVerticalSpeed() {
        if (data.isInLiquid()) return 0.5;
        if (data.isOnClimbable()) return 0.15;
        if (data.isGliding() || data.isFlying()) return 1.0;

        int ticks = Math.min(data.getAirTicks(), 40);
        double maxFall = -MathUtil.GRAVITY * (1 - Math.pow(MathUtil.DRAG, ticks)) / (1 - MathUtil.DRAG);
        return Math.abs(maxFall) + 0.15;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public double getSimulatedX() {
        return simulatedX;
    }

    public double getSimulatedY() {
        return simulatedY;
    }

    public double getSimulatedZ() {
        return simulatedZ;
    }
}
