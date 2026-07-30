package net.neos.neosac.checks.simulation;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleCheck extends Check {

    private static final double BOOST_HORIZONTAL = 0.8;
    private static final double BOOST_VERTICAL = 0.6;
    private static final long DISMOUNT_WINDOW_MS = 400;

    private static final int MAX_BOAT_AIR_TICKS = 10;

    private final Map<UUID, double[]> lastTickPos = new ConcurrentHashMap<>();

    public VehicleCheck(NeosAC plugin) {
        super(plugin, "Vehicle", CheckType.SIMULATION,
                "Проверка транспорта (VehicleBoost при выходе, BoatFly)");
    }

    public void tick(Player player, PlayerData data) {
        if (!isEnabled()) return;

        UUID id = player.getUniqueId();
        boolean inVehicle = player.isInsideVehicle();
        Location loc = player.getLocation();

        if (data.wasInVehicle() && !inVehicle) {
            data.setDismountTime(System.currentTimeMillis());
        }
        data.setWasInVehicle(inVehicle);

        double[] prev = lastTickPos.get(id);
        long now = System.currentTimeMillis();
        long dismount = data.getDismountTime();

        if (!inVehicle && prev != null && dismount > 0 && (now - dismount) <= DISMOUNT_WINDOW_MS
                && (now - data.getLastSetbackTime()) >= 1000
                && !isBoostExempt(player)) {
            double dx = loc.getX() - prev[0];
            double dy = loc.getY() - prev[1];
            double dz = loc.getZ() - prev[2];
            double horizontal = Math.sqrt(dx * dx + dz * dz);

            if (horizontal > BOOST_HORIZONTAL || dy > BOOST_VERTICAL) {
                fail(data, "VehicleBoost: рывок после выхода из транспорта: horiz=%.3f, dy=%.3f",
                        horizontal, dy);
                data.setDismountTime(0);
            }
        }

        lastTickPos.put(id, new double[]{loc.getX(), loc.getY(), loc.getZ()});

        if (plugin.getExemptionManager().isExempt(data, true)) {
            data.resetVehicleAirTicks();
            return;
        }

        if (!inVehicle) {
            data.resetVehicleAirTicks();
            return;
        }

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof org.bukkit.entity.Boat)) {
            data.resetVehicleAirTicks();
            return;
        }

        Location vloc = vehicle.getLocation();
        double vdy = vloc.getY() - data.getLastVehicleY();
        data.setLastVehicleY(vloc.getY());

        if (hasSupportBelow(vloc, 1.0)) {
            data.resetVehicleAirTicks();
            return;
        }

        if (vdy > -0.05) {
            data.incrementVehicleAirTicks();
            if (data.getVehicleAirTicks() > MAX_BOAT_AIR_TICKS) {
                fail(data, "BoatFly: лодка в воздухе без опоры не падает (dy=%.3f, airTicks=%d)",
                        vdy, data.getVehicleAirTicks());
                data.resetVehicleAirTicks();
            }
        } else {
            data.resetVehicleAirTicks();
        }
    }

    private boolean isBoostExempt(Player player) {
        return player.isGliding() || player.isFlying() || player.getAllowFlight()
                || player.getPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION) != null;
    }

    private boolean hasSupportBelow(Location loc, double distance) {
        if (loc == null || loc.getWorld() == null) return true;
        for (double d = 0; d <= distance; d += 0.5) {
            Block b = loc.clone().subtract(0, d + 0.1, 0).getBlock();
            Material type = b.getType();
            if (type.isSolid()) return true;
            if (type == Material.WATER || b.isLiquid()) return true;
        }
        return false;
    }

    public void clear(UUID id) {
        lastTickPos.remove(id);
    }

    @Override
    protected double getViolationAmount() {
        return 1.5;
    }
}
