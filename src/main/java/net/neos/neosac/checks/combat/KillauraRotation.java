package net.neos.neosac.checks.combat;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import net.neos.neosac.raytrace.RaytraceEngine;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class KillauraRotation extends Check implements PacketAware {

    private static final float MAX_ROTATION_DELTA = 60.0f;
    private static final double PERFECT_AIM_THRESHOLD = 0.5;

    public KillauraRotation(NeosAC plugin) {
        super(plugin, "KillauraRotation", CheckType.COMBAT,
                "Проверка Killaura через аномальные ротации (snap, perfect aim)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasRotationChanged()) return;

        float deltaYaw = Math.abs(data.getCurrentYaw() - data.getLastYaw());
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
        float deltaPitch = Math.abs(data.getCurrentPitch() - data.getLastPitch());
        float totalDelta = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (totalDelta > MAX_ROTATION_DELTA && data.getTicksExisted() > 40) {
            long now = System.currentTimeMillis();
            if ((now - data.getLastSetbackTime()) > 1000) {
            }
        }
    }

    @Override
    public void onInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int entityId = interact.getEntityId();
        net.neos.neosac.data.EntityTracker.Snapshot target =
                plugin.getEntityTracker().get(entityId);
        if (target == null) return;
        if (target.npc) return;

        org.bukkit.Location targetLoc =
                new org.bukkit.Location(player.getWorld(), target.x, target.y, target.z);
        double angle = RaytraceEngine.getAngleToTarget(player, targetLoc);

        if (angle < Math.toRadians(PERFECT_AIM_THRESHOLD) && data.getTicksExisted() > 40) {
            fail(data, "Perfect aim: angle=%.2f° < %.2f°",
                    Math.toDegrees(angle), PERFECT_AIM_THRESHOLD);
        }

        long now = System.currentTimeMillis();
        long lastRotUpdate = data.getLastRotationTime();
        long rotDelta = now - lastRotUpdate;

        if (rotDelta > 200) {
            fail(data, "Атака без свежего rotation: rotDelta=%dms", rotDelta);
        }

        float deltaYaw = net.neos.neosac.util.MathUtil.yawDifference(data.getCurrentYaw(), data.getLastYaw());
        float deltaPitch = Math.abs(data.getCurrentPitch() - data.getLastPitch());
        float totalDelta = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        if (totalDelta > MAX_ROTATION_DELTA && data.getTicksExisted() > 40) {
            long timeSinceSnap = now - data.getLastRotationTime();
            if (timeSinceSnap < 100) {
                fail(data, "Rotation snap перед атакой: delta=%.1f° (max %.1f°)",
                        totalDelta, MAX_ROTATION_DELTA);
            }
        }

        if (data.getLastAttackedEntity() != null
                && !data.getLastAttackedEntity().equals(target.uuid)
                && data.getCurrentYaw() == data.getAttackYaw()
                && data.getCurrentPitch() == data.getAttackPitch()
                && data.getTicksExisted() > 40) {
            long attackDelta = now - data.getLastAttackTime();
            if (attackDelta < 500) {
                fail(data, "Идентичная ротация для разных целей: yaw=%.1f, pitch=%.1f",
                        data.getCurrentYaw(), data.getCurrentPitch());
            }
        }

        if (data.getPacketsThisTick() == 0) {
            if (angle > Math.toRadians(30)) {
                fail(data, "Attack без rotation update, angle=%.1f°", Math.toDegrees(angle));
            }
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.2;
    }
}
