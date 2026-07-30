package net.neos.neosac.command;

import net.neos.neosac.NeosAC;
import net.neos.neosac.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AlertsCommand implements CommandExecutor {

    private final NeosAC plugin;

    public AlertsCommand(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().format(plugin.messages().getPlayerOnly()));
            return true;
        }

        if (!player.hasPermission("neosac.alerts")) {
            player.sendMessage(plugin.messages().format(plugin.messages().getNoPermission()));
            return true;
        }

        plugin.getAlertManager().toggleAlerts(player.getUniqueId());
        boolean enabled = plugin.getAlertManager().hasAlerts(player.getUniqueId());

        if (enabled) {
            player.sendMessage(ColorUtil.color(plugin.messages().getPrefix() + "&aАлерты нарушений включены."));
        } else {
            player.sendMessage(ColorUtil.color(plugin.messages().getPrefix() + "&cАлерты нарушений выключены."));
        }
        return true;
    }
}
