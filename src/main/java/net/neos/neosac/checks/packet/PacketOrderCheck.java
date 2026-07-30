package net.neos.neosac.checks.packet;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.entity.Player;

public class PacketOrderCheck extends Check implements PacketAware {

    private static final int MAX_ACTIONS_PER_TICK = 2;

    public PacketOrderCheck(NeosAC plugin) {
        super(plugin, "PacketOrder", CheckType.PACKET,
                "Проверка порядка пакетов (действия до позиции, мульти-действия)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        data.resetPacketsThisTick();
        data.resetBlocksBrokenThisTick();
        data.resetBlocksPlacedThisTick();
    }

    @Override
    public void onInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {
        long now = System.currentTimeMillis();
        long lastFlying = data.getLastFlyingPacket();
        long delta = now - lastFlying;

        if (delta < 5 && data.getPacketsThisTick() == 0) {
            fail(data, "USE_ENTITY до Flying пакета: delta=%dms, packets=%d",
                    delta, data.getPacketsThisTick());
        }

        if (data.getLastRotationPacket() > 0) {
            long rotDelta = now - data.getLastRotationPacket();
            if (rotDelta > 100 && data.getPacketsThisTick() == 0) {
                fail(data, "USE_ENTITY без свежего rotation: rotDelta=%dms",
                        rotDelta);
            }
        }

        if (data.getPacketsThisTick() > MAX_ACTIONS_PER_TICK) {
            fail(data, "Слишком много действий в тике: %d > %d",
                    data.getPacketsThisTick(), MAX_ACTIONS_PER_TICK);
        }
    }

    @Override
    public void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        long now = System.currentTimeMillis();
        long lastFlying = data.getLastFlyingPacket();
        long delta = now - lastFlying;

        if (delta < 5 && data.getPacketsThisTick() == 0) {
            fail(data, "BLOCK_PLACE до Flying пакета: delta=%dms", delta);
        }

        if (data.getBlocksPlacedThisTick() > 1) {
            fail(data, "Multiple placements в одном тике: %d",
                    data.getBlocksPlacedThisTick());
        }
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        long now = System.currentTimeMillis();
        long lastFlying = data.getLastFlyingPacket();
        long delta = now - lastFlying;

        if (delta < 5 && data.getPacketsThisTick() == 0) {
            fail(data, "BLOCK_DIG до Flying пакета: delta=%dms", delta);
        }

        if (data.getBlocksBrokenThisTick() > 1) {
            fail(data, "Multiple block digs в одном тике: %d",
                    data.getBlocksBrokenThisTick());
        }
    }

    @Override
    public void onUseItem(Player player, PlayerData data, WrapperPlayClientUseItem use) {
        long now = System.currentTimeMillis();
        long lastFlying = data.getLastFlyingPacket();
        long delta = now - lastFlying;

        if (delta < 5 && data.getPacketsThisTick() == 0) {
            fail(data, "USE_ITEM до Flying пакета: delta=%dms", delta);
        }
    }


    @Override
    protected double getViolationAmount() {
        return 0.8;
    }
}
