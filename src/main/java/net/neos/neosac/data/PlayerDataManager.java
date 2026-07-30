package net.neos.neosac.data;

import net.neos.neosac.NeosAC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final NeosAC plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public PlayerData create(@NotNull Player player) {
        PlayerData data = new PlayerData(plugin, player);
        dataMap.put(player.getUniqueId(), data);
        if (plugin.getViolationStorage() != null) {
            plugin.getViolationStorage().load(data);
        }
        return data;
    }

    public PlayerData remove(@NotNull UUID uuid) {
        return dataMap.remove(uuid);
    }

    @Nullable
    public PlayerData getPlayerData(@NotNull UUID uuid) {
        return dataMap.get(uuid);
    }

    @Nullable
    public PlayerData getPlayerData(@NotNull Player player) {
        return getPlayerData(player.getUniqueId());
    }

    @NotNull
    public PlayerData getOrCreate(@NotNull Player player) {
        PlayerData data = dataMap.get(player.getUniqueId());
        if (data == null) {
            data = create(player);
        }
        return data;
    }

    public Map<UUID, PlayerData> getAll() {
        return dataMap;
    }

    public void decayAll() {
        dataMap.values().forEach(PlayerData::decayViolations);
    }

    public void resetAll() {
        dataMap.values().forEach(PlayerData::resetAllViolations);
    }

    public int getOnlineCount() {
        return dataMap.size();
    }

    public void cleanup() {
        dataMap.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
}
