package net.neos.neosac.checks.combat;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Criticals extends Check implements PacketAware {

    public Criticals(NeosAC plugin) {
        super(plugin, "Criticals", CheckType.COMBAT,
                "Детектор Critical-хака (фикирование критического удара)");
    }

    @Override
    public void onInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        if (player.isOnGround()) {
            if (data.getAirTicks() == 0) {
                if (data.wasOnGround() && data.isOnGround()) {
                }
            }
        }

        if (data.wasOnGround() && !player.isOnGround() && data.getAirTicks() <= 1) {
            if (data.getCurrentLocation() != null && data.getLastLocation() != null) {
                double dy = data.getCurrentLocation().getY() - data.getLastLocation().getY();
                if (dy < 0.05 && !data.isInLiquid() && !data.isOnClimbable()) {
                    fail(data, "Criticals: onGround=false без реального падения, dy=%.3f", dy);
                }
            }
        }

        if (data.isInLiquid() && data.getAirTicks() > 3) {
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
