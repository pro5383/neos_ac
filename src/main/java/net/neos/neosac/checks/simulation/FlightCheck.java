package net.neos.neosac.checks.simulation;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FlightCheck extends Check implements PacketAware {

    public FlightCheck(NeosAC plugin) {
        super(plugin, "Flight", CheckType.SIMULATION,
                "Проверка на полёт (Flight, Jetpack, Hover, AirJump)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasPositionChanged()) return;

        Location current = data.getCurrentLocation();
        Location last = data.getLastLocation();
        if (current == null || last == null || current.getWorld() == null) return;

        if ((System.currentTimeMillis() - data.getLastSetbackTime()) < 1000) return;

        if (player.isInsideVehicle() || player.isGliding() || player.isRiptiding()) return;

        if (player.getPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION) != null) return;

        if (player.getPotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING) != null) return;

        boolean onGround = flying.isOnGround();
        if (onGround) return;

        double dy = current.getY() - last.getY();

        if (dy > 0.05 && data.getAirTicks() > 5
                && !data.isInLiquid() && !data.isOnClimbable() && !data.isInWeb()) {
            if (!hasBlockBelow(current, 2.0)) {
                fail(data, "Подъём в воздухе: dy=%.3f, airTicks=%d",
                        dy, data.getAirTicks());
            }
        }

        if (data.getAirTicks() > 20 && Math.abs(dy) < 0.01
                && !data.isInLiquid() && !data.isOnClimbable() && !data.isInWeb()) {
            if (!hasBlockBelow(current, 2.0)) {
                fail(data, "Зависание: dy=%.4f, airTicks=%d",
                        dy, data.getAirTicks());
            }
        }

        if (data.getAirTicks() > 10 && dy < 0 && dy > -0.05
                && !data.isInLiquid() && !data.isOnClimbable() && !data.isInWeb()
                && player.getPotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING) == null) {
            if (data.getAirTicks() > 15) {
                fail(data, "Замедленное падение: dy=%.4f, airTicks=%d",
                        dy, data.getAirTicks());
            }
        }

        double prevDy = data.getLastDeltaY();
        boolean upwardAcceleration = dy > 0 && dy > prevDy + 0.15;

        if (upwardAcceleration && !data.wasOnGround() && data.getAirTicks() > 1
                && !data.isInLiquid() && !data.isOnClimbable() && !data.isInWeb()
                && !hasBlockBelow(current, 1.5)) {
            fail(data, "AirJump: ускорение вверх в воздухе, dy=%.3f (пред. %.3f), airTicks=%d",
                    dy, prevDy, data.getAirTicks());
        }
    }

    private boolean hasBlockBelow(Location loc, double distance) {
        if (loc == null || loc.getWorld() == null) return true;
        for (double dy = 0; dy <= distance; dy += 0.5) {
            Block b = loc.clone().subtract(0, dy + 0.5, 0).getBlock();
            if (b.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected double getViolationAmount() {
        return 1.5;
    }
}
