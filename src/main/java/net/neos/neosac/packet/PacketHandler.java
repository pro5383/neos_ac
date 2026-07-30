package net.neos.neosac.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import net.neos.neosac.NeosAC;
import net.neos.neosac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PacketHandler {

    private final NeosAC plugin;
    private PacketListenerAbstract listener;

    public PacketHandler(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public void init() {
        listener = new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(@NotNull PacketReceiveEvent event) {
                handleReceive(event);
            }

            @Override
            public void onPacketSend(@NotNull PacketSendEvent event) {
                handleSend(event);
            }
        };

        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }

    private void handleReceive(@NotNull PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null) return;
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data == null) {
            data = plugin.getPlayerDataManager().getOrCreate(player);
        }

        data.incrementPacketsThisTick();
        data.incrementPacketCounter();
        data.setLastPacketTime(System.currentTimeMillis());

        try {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION ||
                    event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION ||
                    event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                    event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {

                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                handleFlyingPacket(player, data, flying);
                if (data.consumeSetbackRequested()) {
                    event.setCancelled(true);
                }
            }
            else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
                WrapperPlayClientPlayerBlockPlacement place = new WrapperPlayClientPlayerBlockPlacement(event);
                handleBlockPlace(player, data, place);
            }
            else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);
                handleBlockDig(player, data, dig);
            }
            else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
                handleInteractEntity(player, data, interact);
            }
            else if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
                WrapperPlayClientUseItem use = new WrapperPlayClientUseItem(event);
                handleUseItem(player, data, use);
            }
            else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
                WrapperPlayClientHeldItemChange change = new WrapperPlayClientHeldItemChange(event);
                handleHeldItemChange(player, data, change);
            }
            else if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
                WrapperPlayClientChatMessage chat = new WrapperPlayClientChatMessage(event);
                handleChat(player, data, chat);
            }
            else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
                handleClickWindow(player, data, click);
            }
            else if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
                WrapperPlayClientClientStatus status = new WrapperPlayClientClientStatus(event);
                handleClientStatus(player, data, status);
            }
            else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
                WrapperPlayClientSteerVehicle steer = new WrapperPlayClientSteerVehicle(event);
                handleSteerVehicle(player, data, steer);
            }
            else if (event.getPacketType() == PacketType.Play.Client.PONG) {
                WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
                handlePong(player, data, pong);
            }
        } catch (Exception e) {
            if (plugin.configuration().isVerbose()) {
                plugin.getLogger().warning("Ошибка обработки пакета " + event.getPacketType() + ": " + e.getMessage());
            }
        }
    }

    private void handleSend(@NotNull PacketSendEvent event) {
    }


    private void handleFlyingPacket(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (flying.hasPositionChanged()) {
            var loc = flying.getLocation();
            var pos = loc.getPosition();
            org.bukkit.Location bukkitLoc = new org.bukkit.Location(player.getWorld(), pos.x, pos.y, pos.z);
            data.setCurrentLocation(bukkitLoc);
            data.setLastPositionPacket(System.currentTimeMillis());
            data.setLastPositionRotationPacket(System.currentTimeMillis());
        }
        if (flying.hasRotationChanged()) {
            var loc = flying.getLocation();
            data.setLastYaw(data.getCurrentYaw());
            data.setLastPitch(data.getCurrentPitch());
            data.setCurrentYaw(loc.getYaw());
            data.setCurrentPitch(loc.getPitch());
            data.setLastRotationTime(System.currentTimeMillis());
            data.setLastRotationPacket(System.currentTimeMillis());
        }

        data.setOnGround(flying.isOnGround());
        data.setLastFlyingPacket(System.currentTimeMillis());

        if (flying.hasPositionChanged()) {
            org.bukkit.Location cur = data.getCurrentLocation();
            org.bukkit.Location prev = data.getLastLocation();

            if (cur != null && prev != null && cur.getWorld() != null
                    && cur.getWorld().equals(prev.getWorld())) {
                data.pushDeltaY(cur.getY() - prev.getY());
            }

            if (flying.isOnGround()) {
                data.resetAirTicks();
                data.incrementGroundTicks();
            } else {
                data.incrementAirTicks();
                data.resetGroundTicks();
            }

            if (cur != null) {
                data.setInLiquid(net.neos.neosac.util.LocationUtil.isInLiquid(cur));
                data.setOnClimbable(net.neos.neosac.util.LocationUtil.isOnClimbable(cur));
            }
            data.setSprinting(player.isSprinting());
            data.setSneaking(player.isSneaking());
        }

        if (flying.hasPositionChanged() || flying.hasRotationChanged()) {
            data.incrementTicksExisted();
        }

        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onFlying(player, data, flying);
                    }
                } catch (Exception e) {
                    if (plugin.configuration().isVerbose()) {
                        plugin.getLogger().warning("Ошибка в проверке " + check.getName() + ": " + e.getMessage());
                    }
                }
            }
        });

        if (flying.isOnGround()) {
            plugin.getSetbackManager().updateSafeLocation(data);
        }
    }

    private void handleBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        var blockPos = place.getBlockPosition();

        if (!isRealBlockPlacement(player, blockPos)) {
            return;
        }

        data.setLastBlockPlace(System.currentTimeMillis());
        data.incrementBlocksPlacedThisTick();

        org.bukkit.Location placedAt = new org.bukkit.Location(player.getWorld(),
                blockPos.x, blockPos.y, blockPos.z);
        data.setLastPlacedBlock(placedAt);

        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onBlockPlace(player, data, place);
                    }
                } catch (Exception e) {
                    if (plugin.configuration().isVerbose()) {
                        plugin.getLogger().warning("Ошибка в проверке " + check.getName() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    private boolean isRealBlockPlacement(Player player,
            com.github.retrooper.packetevents.util.Vector3i blockPos) {
        try {
            org.bukkit.block.Block clicked =
                    player.getWorld().getBlockAt(blockPos.x, blockPos.y, blockPos.z);
            if (clicked.getType().isInteractable() && !player.isSneaking()) {
                return false;
            }
        } catch (Exception ignored) {
        }

        org.bukkit.Material main = player.getInventory().getItemInMainHand().getType();
        org.bukkit.Material off = player.getInventory().getItemInOffHand().getType();
        boolean placeableInHand = (main.isBlock() && !main.isAir())
                || (off.isBlock() && !off.isAir());
        return placeableInHand;
    }

    private void handleBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        data.setLastBlockBreak(System.currentTimeMillis());
        data.incrementBlocksBrokenThisTick();

        var blockPos = dig.getBlockPosition();
        org.bukkit.Location brokenAt = new org.bukkit.Location(player.getWorld(),
                blockPos.x, blockPos.y, blockPos.z);
        data.setLastBrokenBlock(brokenAt);

        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onBlockDig(player, data, dig);
                    }
                } catch (Exception e) {
                    if (plugin.configuration().isVerbose()) {
                        plugin.getLogger().warning("Ошибка в проверке " + check.getName() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    private void handleInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {
        data.setLastAttackTime(System.currentTimeMillis());

        int entityId = interact.getEntityId();
        net.neos.neosac.data.EntityTracker.Snapshot target =
                plugin.getEntityTracker().get(entityId);

        if (target != null) {
            org.bukkit.Location self = data.getCurrentLocation();
            double dist = self == null ? 0.0 : Math.sqrt(
                    Math.pow(self.getX() - target.x, 2)
                            + Math.pow(self.getY() - target.y, 2)
                            + Math.pow(self.getZ() - target.z, 2));
            data.setLastAttackedEntity(target.uuid);
            data.setLastAttackDistance(dist);
            data.setAttackYaw(data.getCurrentYaw());
            data.setAttackPitch(data.getCurrentPitch());
            data.setLastRotationBeforeAttack(data.getLastRotationTime());
        }

        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onInteractEntity(player, data, interact);
                    }
                } catch (Exception e) {
                    if (plugin.configuration().isVerbose()) {
                        plugin.getLogger().warning("Ошибка в проверке " + check.getName() + ": " + e.getMessage());
                    }
                }
            }
        });
    }

    private void handleUseItem(Player player, PlayerData data, WrapperPlayClientUseItem use) {
        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onUseItem(player, data, use);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void handleHeldItemChange(Player player, PlayerData data, WrapperPlayClientHeldItemChange change) {
        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onHeldItemChange(player, data, change);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void handleChat(Player player, PlayerData data, WrapperPlayClientChatMessage chat) {
        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onChat(player, data, chat);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void handleClickWindow(Player player, PlayerData data, WrapperPlayClientClickWindow click) {
        plugin.getCheckManager().getChecks().forEach(check -> {
            if (check.isEnabled() && !plugin.getExemptionManager().isExempt(data)) {
                try {
                    if (check instanceof PacketAware packetAware) {
                        packetAware.onClickWindow(player, data, click);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void handleClientStatus(Player player, PlayerData data, WrapperPlayClientClientStatus status) {
    }

    private void handleSteerVehicle(Player player, PlayerData data, WrapperPlayClientSteerVehicle steer) {
    }

    private void handlePong(Player player, PlayerData data, WrapperPlayClientPong pong) {
    }

    public void shutdown() {
        if (listener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(listener);
            } catch (Exception ignored) {}
        }
    }

    public ServerVersion getServerVersion() {
        try {
            return PacketEvents.getAPI().getServerManager().getVersion();
        } catch (Exception e) {
            return ServerVersion.V_1_21_1;
        }
    }
}
