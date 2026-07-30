package net.neos.neosac.checks.interaction;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.raytrace.AABB;
import net.neos.neosac.raytrace.RaytraceEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ReachCheck extends Check implements PacketAware {

    private static final double MAX_REACH_SURVIVAL = 3.0;
    private static final double MAX_REACH_CREATIVE = 5.0;

    private static final double PING_TOLERANCE = 0.3;

    public ReachCheck(NeosAC plugin) {
        super(plugin, "Reach", CheckType.INTERACTION,
                "Проверка дистанции атаки сущности (Reach hack, Killaura)");
    }

    @Override
    public void onInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int entityId = interact.getEntityId();
        net.neos.neosac.data.EntityTracker.Snapshot target =
                plugin.getEntityTracker().get(entityId);
        if (target == null) return;

        if (target.npc) return;

        boolean isCreative = player.getGameMode().toString().equals("CREATIVE");
        double maxReach = isCreative ? MAX_REACH_CREATIVE : MAX_REACH_SURVIVAL;
        maxReach += PING_TOLERANCE;

        AABB targetAABB = target.box;

        Location eye = player.getEyeLocation();
        Vector eyeVec = eye.toVector();

        double closestDist = RaytraceEngine.closestDistanceToAABB(eyeVec, targetAABB);

        Vector look = eye.getDirection();
        double[] hit = targetAABB.intersectRay(
                eye.getX(), eye.getY(), eye.getZ(),
                look.getX(), look.getY(), look.getZ(),
                maxReach * 2
        );

        double actualDist = (hit != null) ? hit[3] : closestDist;

        if (actualDist > maxReach) {
            fail(data, "Reach: %.3f > %.3f, цель=%s, distToAABB=%.3f, rayHit=%s",
                    actualDist, maxReach, target.typeName,
                    closestDist, hit != null ? "да" : "нет");
        }

        Location targetLoc = new Location(player.getWorld(), target.x, target.y, target.z);
        double angle = RaytraceEngine.getAngleToTarget(player, targetLoc);
        if (angle > Math.PI / 2 + 0.3) {
            fail(data, "Атака вне угла обзора: angle=%.1f° > 90°, цель=%s",
                    Math.toDegrees(angle), target.typeName);
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.2;
    }
}
