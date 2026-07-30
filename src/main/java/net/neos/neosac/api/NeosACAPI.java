package net.neos.neosac.api;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public class NeosACAPI {

    private static NeosACAPI instance;
    private final NeosAC plugin;

    public NeosACAPI(@NotNull NeosAC plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static NeosACAPI getInstance() {
        return instance;
    }

    public double getViolations(@NotNull Player player, @NotNull String checkName) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return 0;
        Check check = plugin.getCheckManager().getCheck(checkName);
        if (check == null) return 0;
        return data.getViolations(check);
    }

    public double getTotalViolations(@NotNull Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return 0;
        return data.getViolationsMap().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public void resetViolations(@NotNull Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            data.resetAllViolations();
        }
    }

    public void exempt(@NotNull UUID uuid, @NotNull String reason) {
        plugin.getExemptionManager().exempt(uuid, reason);
    }

    public void unexempt(@NotNull UUID uuid, @NotNull String reason) {
        plugin.getExemptionManager().unexempt(uuid, reason);
    }

    public boolean isExempt(@NotNull Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        return data == null || plugin.getExemptionManager().isExempt(data);
    }

    public void forceSetback(@NotNull Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null) {
            plugin.getSetbackManager().forceSetback(data);
        }
    }

    public Collection<Check> getChecks() {
        return plugin.getCheckManager().getChecks();
    }

    public void setCheckEnabled(@NotNull String checkName, boolean enabled) {
        Check check = plugin.getCheckManager().getCheck(checkName);
        if (check != null) {
            check.setEnabled(enabled);
        }
    }

    @Nullable
    public PlayerData getPlayerData(@NotNull Player player) {
        return plugin.getPlayerDataManager().getPlayerData(player);
    }

    public boolean isPacketEventsAvailable() {
        return plugin.isPacketEventsAvailable();
    }
}
