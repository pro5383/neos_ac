package net.neos.neosac.manager;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.util.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {

    private final NeosAC plugin;
    private final Set<java.util.UUID> alertRecipients = ConcurrentHashMap.newKeySet();
    private final Set<java.util.UUID> verboseRecipients = ConcurrentHashMap.newKeySet();

    public AlertManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public void alert(Player player, Check check, String detail, double vLevel) {
        if (player == null) return;

        boolean verbose = plugin.configuration().isVerbose();
        String template = verbose ? plugin.messages().getAlertVerbose() : plugin.messages().getAlert();

        String vLevelStr = String.format("%.1f", vLevel);
        String pingStr = String.valueOf(ServerUtil.getPing(player));
        String tpsStr = String.format("%.1f", ServerUtil.getTps());

        String message = plugin.messages().format(template,
                "player", player.getName(),
                "check", check.getDisplayName(),
                "vlevel", vLevelStr,
                "detail", detail,
                "ping", pingStr,
                "tps", tpsStr
        );

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!recipient.hasPermission("neosac.alerts")) continue;

            if (verbose) {
                recipient.sendMessage(message);
            } else if (verboseRecipients.contains(recipient.getUniqueId()) ||
                       alertRecipients.contains(recipient.getUniqueId())) {
                recipient.sendMessage(message);
            }
        }

        Bukkit.getConsoleSender().sendMessage(message);
    }

    public void setbackAlert(Player player, Check check, String detail) {
        if (!plugin.configuration().isBroadcastSetbacks()) return;

        String message = plugin.messages().format(plugin.messages().getSetback(),
                "player", player.getName(),
                "check", check.getDisplayName(),
                "detail", detail
        );

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.hasPermission("neosac.alerts")) {
                recipient.sendMessage(message);
            }
        }
    }

    public void banAlert(Player player, Check check, double vLevel) {
        String message = plugin.messages().format(plugin.messages().getBan(),
                "player", player.getName(),
                "check", check.getDisplayName(),
                "vlevel", String.format("%.1f", vLevel)
        );

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.hasPermission("neosac.alerts")) {
                recipient.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public void toggleAlerts(java.util.UUID uuid) {
        if (alertRecipients.contains(uuid)) {
            alertRecipients.remove(uuid);
        } else {
            alertRecipients.add(uuid);
        }
    }

    public void toggleVerbose(java.util.UUID uuid) {
        if (verboseRecipients.contains(uuid)) {
            verboseRecipients.remove(uuid);
        } else {
            verboseRecipients.add(uuid);
        }
    }

    public boolean hasAlerts(java.util.UUID uuid) {
        return alertRecipients.contains(uuid);
    }

    public boolean hasVerbose(java.util.UUID uuid) {
        return verboseRecipients.contains(uuid);
    }
}
