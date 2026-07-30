package net.neos.neosac.checks.interaction;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.raytrace.RaytraceEngine;
import net.neos.neosac.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RaytraceInteractCheck extends Check implements PacketAware {

    private static final double MAX_BLOCK_REACH = 4.5;
    private static final double MAX_BLOCK_REACH_CREATIVE = 6.0;
    private static final double MAX_ANGLE = Math.toRadians(100);

    public RaytraceInteractCheck(NeosAC plugin) {
        super(plugin, "RaytraceInteract", CheckType.INTERACTION,
                "Воксельный рейкаст + LoS для проверок взаимодействий с блоками");
    }

    @Override
    public void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        var blockPos = place.getBlockPosition();
        if (blockPos == null) return;

        if (System.currentTimeMillis() - data.getJoinTime() < 2000) return;

        boolean isCreative = player.getGameMode().toString().equals("CREATIVE");
        double maxReach = isCreative ? MAX_BLOCK_REACH_CREATIVE : MAX_BLOCK_REACH;

        Location eye = player.getEyeLocation();
        Location targetCenter = new Location(eye.getWorld(),
                blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);

        if (eye.getWorld() == null) return;

        double distance = eye.distance(targetCenter);
        if (distance > maxReach + 0.5) {
            fail(data, "BlockPlace вне reach: dist=%.2f > %.2f, блок=[%d,%d,%d]",
                    distance, maxReach, blockPos.x, blockPos.y, blockPos.z);
            return;
        }


        double angle = RaytraceEngine.getAngleToTarget(player, targetCenter);
        if (angle > MAX_ANGLE) {
            fail(data, "BlockPlace вне угла обзора: angle=%.1f° > %.1f°, блок=[%d,%d,%d]",
                    Math.toDegrees(angle), Math.toDegrees(MAX_ANGLE),
                    blockPos.x, blockPos.y, blockPos.z);
        }
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        var blockPos = dig.getBlockPosition();
        if (blockPos == null) return;

        if (System.currentTimeMillis() - data.getJoinTime() < 2000) return;

        boolean isCreative = player.getGameMode().toString().equals("CREATIVE");
        double maxReach = isCreative ? MAX_BLOCK_REACH_CREATIVE : MAX_BLOCK_REACH;

        Location eye = player.getEyeLocation();
        Location targetCenter = new Location(eye.getWorld(),
                blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
        if (eye.getWorld() == null) return;

        double distance = eye.distance(targetCenter);
        if (distance > maxReach + 0.5) {
            fail(data, "BlockDig вне reach: dist=%.2f > %.2f, блок=[%d,%d,%d]",
                    distance, maxReach, blockPos.x, blockPos.y, blockPos.z);
            return;
        }

        double angle = RaytraceEngine.getAngleToTarget(player, targetCenter);
        if (angle > MAX_ANGLE) {
            fail(data, "BlockDig вне угла обзора: angle=%.1f° > %.1f°, блок=[%d,%d,%d]",
                    Math.toDegrees(angle), Math.toDegrees(MAX_ANGLE),
                    blockPos.x, blockPos.y, blockPos.z);
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.5;
    }
}
