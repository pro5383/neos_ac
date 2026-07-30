package net.neos.neosac.checks.simulation;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SimulationCheck extends Check implements PacketAware {

    private static final double HORIZONTAL_TOLERANCE = 0.15;
    private static final double VERTICAL_TOLERANCE = 0.15;

    public SimulationCheck(NeosAC plugin) {
        super(plugin, "Simulation", CheckType.SIMULATION,
                "Общая проверка физики движения (X/Y/Z отклонение от симуляции)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasPositionChanged()) return;

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        if (current == null || last == null) return;
        if (current.getWorld() == null || last.getWorld() == null) return;
        if (!current.getWorld().equals(last.getWorld())) return;

        if ((System.currentTimeMillis() - data.getLastSetbackTime()) < 1000) {
            return;
        }

        data.getPhysics().simulateTick(current, flying.isOnGround(),
                player.isSprinting(), player.isSneaking());

        double simX = data.getPhysics().getSimulatedX();
        double simY = data.getPhysics().getSimulatedY();
        double simZ = data.getPhysics().getSimulatedZ();

        double realDx = current.getX() - last.getX();
        double realDy = current.getY() - last.getY();
        double realDz = current.getZ() - last.getZ();

        double maxSpeed = data.getPhysics().getMaxHorizontalSpeed(player, player.isSprinting(), player.isSneaking());
        double realHorizontal = MathUtil.distance2D(realDx, realDz);

        int jumpBoostLevel = 0;
        if (player.getPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST) != null) {
            jumpBoostLevel = player.getPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST).getAmplifier() + 1;
        }
        double jumpBoostBonus = jumpBoostLevel * 0.1;

        if (!data.getPhysics().isVerticalMoveLegit(realDy, flying.isOnGround(), data.wasOnGround(), jumpBoostBonus)) {
            fail(data, "Аномальное движение по Y: dy=%.3f, onGround=%s, wasOnGround=%s, airTicks=%d",
                    realDy, flying.isOnGround(), data.wasOnGround(), data.getAirTicks());
        }

        if (realHorizontal > maxSpeed + HORIZONTAL_TOLERANCE) {
            long now = System.currentTimeMillis();
            boolean recentTeleport = (now - data.getLastSetbackTime()) < 1000;
            if (!recentTeleport && !player.isInsideVehicle() && !player.isGliding()) {
                fail(data, "Превышение горизонтальной скорости: %.3f > %.3f (sprint=%s, sneak=%s, ground=%s)",
                        realHorizontal, maxSpeed, player.isSprinting(), player.isSneaking(), flying.isOnGround());
            }
        }

        double maxVertical = data.getPhysics().getMaxVerticalSpeed();

        if (realDy > 0 && data.getAirTicks() <= 6) {
            double jumpCeil = MathUtil.JUMP_VELOCITY
                    * Math.pow(MathUtil.DRAG, Math.max(0, data.getAirTicks() - 1));
            maxVertical = Math.max(maxVertical, jumpCeil);
        }

        if (Math.abs(realDy) > maxVertical + VERTICAL_TOLERANCE && !flying.isOnGround()) {
            double maxVerticalWithJump = maxVertical + (jumpBoostLevel * 0.2);

            if (Math.abs(realDy) > maxVerticalWithJump + VERTICAL_TOLERANCE) {
                fail(data, "Аномальная вертикальная скорость: dy=%.3f > %.3f (jumpBoost=%d, airTicks=%d)",
                        realDy, maxVerticalWithJump, jumpBoostLevel, data.getAirTicks());
            }
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
