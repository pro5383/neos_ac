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

public class NoFallCheck extends Check implements PacketAware {

    private static final int MIN_AIR_TICKS = 3;

    public NoFallCheck(NeosAC plugin) {
        super(plugin, "NoFall", CheckType.SIMULATION,
                "Проверка на чит NoFall (спуфинг on-ground для избежания урона от падения)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        boolean claimedOnGround = flying.isOnGround();

        if (!claimedOnGround) return;

        Location loc = data.getCurrentLocation();
        if (loc == null || loc.getWorld() == null) return;

        boolean serverOnGround = isReallyOnGround(loc);

        if (data.getAirTicks() < MIN_AIR_TICKS) {
            return;
        }

        if (!serverOnGround && data.getAirTicks() > MIN_AIR_TICKS) {
            Block below = loc.clone().subtract(0, 1, 0).getBlock();
            Block feet = loc.getBlock();

            if (!below.getType().isSolid() && !isPassableForGround(feet.getType())) {
                double fallDistance = calculateFallDistance(loc);

                if (fallDistance > 3.0) {
                    fail(data, "Спуфинг onGround: airTicks=%d, fallDistance=%.2f, блок_снизу=%s",
                            data.getAirTicks(), fallDistance, below.getType().name());
                }
            }
        }
    }

    private boolean isReallyOnGround(Location loc) {
        if (loc.getWorld() == null) return true;

        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();

        for (double ox = -0.3; ox <= 0.3; ox += 0.3) {
            for (double oz = -0.3; oz <= 0.3; oz += 0.3) {
                Block b = new Location(loc.getWorld(), px + ox, py - 0.05, pz + oz).getBlock();
                if (b.getType().isSolid()) {
                    return true;
                }
            }
        }

        Block directly = loc.clone().subtract(0, 0.05, 0).getBlock();
        return directly.getType().isSolid();
    }

    private boolean isPassableForGround(Material type) {
        if (type.isSolid()) return true;
        return false;
    }

    private double calculateFallDistance(Location loc) {
        if (loc.getWorld() == null) return 0;
        for (int i = 0; i < 30; i++) {
            Block b = loc.clone().subtract(0, i + 1, 0).getBlock();
            if (b.getType().isSolid()) {
                return i + 1;
            }
        }
        return 30;
    }

    @Override
    protected double getViolationAmount() {
        return 1.5;
    }
}
