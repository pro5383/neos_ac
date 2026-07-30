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

public class SpeedCheck extends Check implements PacketAware {

    private static final double TOLERANCE = 0.05;

    public SpeedCheck(NeosAC plugin) {
        super(plugin, "Speed", CheckType.SIMULATION,
                "Проверка горизонтальной скорости (Speed, Bhop, YPort, NoSlow)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasPositionChanged()) return;

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        if (current == null || last == null) return;
        if (current.getWorld() == null || !current.getWorld().equals(last.getWorld())) return;

        double dx = current.getX() - last.getX();
        double dz = current.getZ() - last.getZ();
        double horizontalSpeed = MathUtil.distance2D(dx, dz);

        long now = System.currentTimeMillis();
        if ((now - data.getLastSetbackTime()) < 1000) return;

        if (player.isInsideVehicle() || player.isGliding() || player.isFlying()) return;

        double maxSpeed = data.getPhysics().getMaxHorizontalSpeed(player,
                player.isSprinting(), player.isSneaking());

        if (data.isInLiquid()) {
            maxSpeed *= 0.5;
        }
        if (data.isOnClimbable()) {
            maxSpeed = 0.15;
        }
        if (data.isInWeb()) {
            maxSpeed = 0.05;
        }

        if (player.isHandRaised() && !player.isRiptiding()) {
            maxSpeed *= 0.35;
        }

        if (data.getTicksExisted() < 20) return;

        if (horizontalSpeed > maxSpeed + TOLERANCE) {
            double excess = horizontalSpeed - maxSpeed;
            fail(data, "Скорость: %.4f > %.4f (превышение=%.4f, sprint=%s, sneak=%s, ground=%s)",
                    horizontalSpeed, maxSpeed, excess, player.isSprinting(),
                    player.isSneaking(), flying.isOnGround());
        }

        if (!flying.isOnGround() && data.getAirTicks() > 5 && horizontalSpeed > 0.45) {
            fail(data, "Аномальная скорость в воздухе: %.4f > 0.45 (airTicks=%d)",
                    horizontalSpeed, data.getAirTicks());
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
