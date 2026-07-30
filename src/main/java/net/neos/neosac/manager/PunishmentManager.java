package net.neos.neosac.manager;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PunishmentManager {

    private final NeosAC plugin;

    public PunishmentManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public void ban(PlayerData data, Check check, String detail) {
        if (data == null || data.getPlayer() == null) return;
        Player player = data.getPlayer();

        plugin.getAlertManager().banAlert(player, check, data.getViolations(check));

        String reason = plugin.configuration().getBanReason()
                .replace("%check%", check.getDisplayName())
                .replace("%detail%", detail);

        String command = plugin.configuration().getBanCommand()
                .replace("%player%", player.getName())
                .replace("%reason%", reason);

        if (plugin.configuration().isBanByConsole()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                CommandSender sender = Bukkit.getConsoleSender();
                Bukkit.dispatchCommand(sender, command);
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }

        plugin.getLogger().warning(String.format(
                "[BAN] %s забанен за %s (V=%.2f). Detail: %s",
                player.getName(), check.getDisplayName(), data.getViolations(check), detail
        ));

        data.resetAllViolations();
        if (plugin.getViolationStorage() != null) {
            plugin.getViolationStorage().clear(player.getUniqueId());
        }
    }

    public void kick(PlayerData data, Check check, String reason) {
        if (data == null || data.getPlayer() == null) return;
        Player player = data.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer("§c[NeosAC] §fОбнаружено использование читов\n§7Проверка: §e" + check.getDisplayName() + "\n§7" + reason);
        });
    }
}
