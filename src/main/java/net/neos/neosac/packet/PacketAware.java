package net.neos.neosac.packet;

import com.github.retrooper.packetevents.wrapper.play.client.*;
import net.neos.neosac.data.PlayerData;
import org.bukkit.entity.Player;

public interface PacketAware {

    default void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {}

    default void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {}

    default void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {}

    default void onInteractEntity(Player player, PlayerData data, WrapperPlayClientInteractEntity interact) {}

    default void onUseItem(Player player, PlayerData data, WrapperPlayClientUseItem use) {}

    default void onHeldItemChange(Player player, PlayerData data, WrapperPlayClientHeldItemChange change) {}

    default void onChat(Player player, PlayerData data, WrapperPlayClientChatMessage chat) {}

    default void onClickWindow(Player player, PlayerData data, WrapperPlayClientClickWindow click) {}
}
