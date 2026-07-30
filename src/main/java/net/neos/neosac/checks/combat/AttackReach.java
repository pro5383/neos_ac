package net.neos.neosac.checks.combat;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.raytrace.AABB;
import net.neos.neosac.raytrace.RaytraceEngine;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AttackReach extends Check implements PacketAware {

    private static final double MAX_REACH = 3.0;
    private static final double PING_TOLERANCE = 0.4;

    private final java.util.Map<java.util.UUID, double[]> reachHistory = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int HISTORY_SIZE = 10;

    public AttackReach(NeosAC plugin) {
        super(plugin, "AttackReach", CheckType.COMBAT,
                "Точная проверка reach для атак с raycast и статистикой");
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
        double maxReach = (isCreative ? 5.0 : MAX_REACH) + PING_TOLERANCE;

        Location eye = player.getEyeLocation();
        AABB targetAABB = target.box;

        Vector look = eye.getDirection();
        double[] hit = targetAABB.intersectRay(
                eye.getX(), eye.getY(), eye.getZ(),
                look.getX(), look.getY(), look.getZ(),
                maxReach * 2
        );

        double closestDist = RaytraceEngine.closestDistanceToAABB(eye.toVector(), targetAABB);
        double actualDist = (hit != null) ? hit[3] : closestDist;

        double[] history = reachHistory.computeIfAbsent(player.getUniqueId(), k -> new double[HISTORY_SIZE]);
        System.arraycopy(history, 1, history, 0, HISTORY_SIZE - 1);
        history[HISTORY_SIZE - 1] = actualDist;

        if (actualDist > maxReach) {
            fail(data, "AttackReach: %.3f > %.3f, rayHit=%s",
                    actualDist, maxReach, hit != null ? "да" : "нет");
            return;
        }

        double avg = 0;
        int count = 0;
        for (double d : history) {
            if (d > 0) {
                avg += d;
                count++;
            }
        }
        if (count >= 5) {
            avg /= count;
            if (avg > 3.3) {
                fail(data, "Средний reach высок: %.3f за %d атак", avg, count);
            }
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
