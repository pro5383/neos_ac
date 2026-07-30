package net.neos.neosac.manager;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetbackManager {

    private final NeosAC plugin;

    public SetbackManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public void setback(PlayerData data, Check check, String detail) {
        if (data == null || data.getPlayer() == null) return;

        Player player = data.getPlayer();
        Location setbackLoc = data.getLastSetbackLocation();

        if (setbackLoc == null) {
            setbackLoc = data.getCurrentLocation();
        }
        if (setbackLoc == null) return;

        Location safe = LocationUtil.findSafeLocation(setbackLoc);

        data.requestSetback();

        data.resetLocationTo(safe);

        plugin.getAlertManager().setbackAlert(player, check, detail);

        long now = System.currentTimeMillis();
        long cooldown = plugin.configuration().isPreventSetbackSpam()
                ? (long) plugin.configuration().getSetbackCooldownMs()
                : 0L;
        if (now - data.getLastSetbackTime() >= cooldown) {
            data.setLastSetbackTime(now);
            final Location tp = safe;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    player.teleport(tp);
                    player.setVelocity(player.getVelocity().zero());
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка ресинка setback для "
                            + player.getName() + ": " + e.getMessage());
                }
            });
        }
    }

    public void updateSafeLocation(PlayerData data) {
        if (data == null) return;
        Location loc = data.getCurrentLocation();
        if (loc != null) {
            data.setLastSetbackLocation(loc.clone());
        }
    }

    public void forceSetback(PlayerData data) {
        if (data == null || data.getPlayer() == null) return;
        Player player = data.getPlayer();
        Location setbackLoc = data.getLastSetbackLocation();

        if (setbackLoc != null) {
            Location safe = LocationUtil.findSafeLocation(setbackLoc);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(safe);
                player.setVelocity(player.getVelocity().zero());
            });
        }
    }
}
