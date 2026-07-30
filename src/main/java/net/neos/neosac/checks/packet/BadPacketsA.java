package net.neos.neosac.checks.packet;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.entity.Player;

public class BadPacketsA extends Check implements PacketAware {

    private static final double MAX_COORD = 3.0E7;
    private static final float MAX_YAW = 360.0f;
    private static final float MAX_PITCH = 90.0f;

    public BadPacketsA(NeosAC plugin) {
        super(plugin, "BadPacketsA", CheckType.PACKET,
                "Проверка невалидных пакетов (NaN, Infinity, дубликаты)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (flying.hasPositionChanged()) {
            var loc = flying.getLocation();
            var pos = loc.getPosition();
            double x = pos.x;
            double y = pos.y;
            double z = pos.z;

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                    || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
                fail(data, "Невалидные координаты: x=%s, y=%s, z=%s", x, y, z);
                return;
            }

            if (Math.abs(x) > MAX_COORD || Math.abs(z) > MAX_COORD) {
                fail(data, "Превышение лимита координат: x=%.2f, z=%.2f", x, z);
                return;
            }

            if (y < -64 || y > 320) {
                fail(data, "Невалидный Y: %.2f (допустимо -64..320)", y);
                return;
            }
        }

        if (flying.hasRotationChanged()) {
            var loc = flying.getLocation();
            float yaw = loc.getYaw();
            float pitch = loc.getPitch();

            if (Float.isNaN(yaw) || Float.isNaN(pitch)
                    || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
                fail(data, "Невалидная ротация: yaw=%s, pitch=%s", yaw, pitch);
                return;
            }

            if (Math.abs(pitch) > MAX_PITCH + 0.1f) {
                fail(data, "Невалидный pitch: %.2f (допустимо ±90)", pitch);
                return;
            }
        }
    }

    @Override
    public void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        var pos = place.getBlockPosition();
        if (pos == null) return;
        if (Math.abs(pos.x) > MAX_COORD || Math.abs(pos.z) > MAX_COORD
                || pos.y < -64 || pos.y > 320) {
            fail(data, "Невалидная позиция блока: %d, %d, %d", pos.x, pos.y, pos.z);
        }
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        var pos = dig.getBlockPosition();
        if (pos == null) return;
        if (Math.abs(pos.x) > MAX_COORD || Math.abs(pos.z) > MAX_COORD
                || pos.y < -64 || pos.y > 320) {
            fail(data, "Невалидная позиция копания: %d, %d, %d", pos.x, pos.y, pos.z);
        }
    }

    @Override
    protected double getViolationAmount() {
        return 2.0;
    }
}
