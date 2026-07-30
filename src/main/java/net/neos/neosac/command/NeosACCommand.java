package net.neos.neosac.command;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NeosACCommand implements CommandExecutor, TabCompleter {

    private final NeosAC plugin;

    public NeosACCommand(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("neosac.admin")) {
            sender.sendMessage(plugin.messages().format(plugin.messages().getNoPermission()));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help" -> sendHelp(sender, label);
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.messages().format(plugin.messages().getConfigReloaded()));
            }
            case "info" -> {
                if (args.length < 2) {
                    if (sender instanceof Player p) {
                        sendInfo(sender, p);
                    } else {
                        sender.sendMessage(plugin.messages().format(plugin.messages().getPlayerOnly()));
                    }
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.messages().format(plugin.messages().getPlayerNotFound()));
                        return true;
                    }
                    sendInfo(sender, target);
                }
            }
            case "verbose" -> {
                if (args.length < 2) {
                    boolean current = plugin.configuration().isVerbose();
                    plugin.configuration().setVerbose(!current);
                    sender.sendMessage(plugin.messages().format(
                            current ? plugin.messages().getVerboseDisabled() : plugin.messages().getVerboseEnabled()));
                } else {
                    boolean enable = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                    plugin.configuration().setVerbose(enable);
                    sender.sendMessage(plugin.messages().format(
                            enable ? plugin.messages().getVerboseEnabled() : plugin.messages().getVerboseDisabled()));
                }
            }
            case "reset" -> {
                if (args.length < 2) {
                    plugin.getPlayerDataManager().resetAll();
                    sender.sendMessage(ColorUtil.color(plugin.messages().getPrefix() + "&aВсе нарушения сброшены."));
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.messages().format(plugin.messages().getPlayerNotFound()));
                        return true;
                    }
                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
                    if (data != null) {
                        data.resetAllViolations();
                    }
                    sender.sendMessage(plugin.messages().format(plugin.messages().getViolationsReset(),
                            "player", target.getName()));
                }
            }
            case "checks" -> sendChecksList(sender);
            default -> {
                sender.sendMessage(plugin.messages().format(plugin.messages().getUnknownCommand()));
                sendHelp(sender, label);
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.messages().getHelpHeader());
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " help", "description", "Показать эту справку"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " reload", "description", "Перезагрузить конфигурацию"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " info [player]", "description", "Информация о нарушениях"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " verbose <on|off>", "description", "Подробные алерты"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " reset [player]", "description", "Сброс нарушений"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", label + " checks", "description", "Список проверок"));
        sender.sendMessage(plugin.messages().format(plugin.messages().getHelpLine(),
                "command", "neosacalerts", "description", "Вкл/выкл личные алерты"));
        sender.sendMessage(plugin.messages().getHelpFooter());
    }

    private void sendInfo(CommandSender sender, Player target) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null) {
            sender.sendMessage(plugin.messages().format(plugin.messages().getPlayerNotFound()));
            return;
        }

        double total = data.getViolationsMap().values().stream().mapToDouble(Double::doubleValue).sum();
        sender.sendMessage(ColorUtil.color(plugin.messages().getPrefix() + "&7Информация об игроке: &c" + target.getName()));
        sender.sendMessage(ColorUtil.color("&8&m----------------------------"));
        sender.sendMessage(ColorUtil.color("&7Общих нарушений: &c" + String.format("%.1f", total)));
        sender.sendMessage(ColorUtil.color("&7В игре: &f" + (System.currentTimeMillis() - data.getJoinTime()) / 1000 + "с"));
        sender.sendMessage(ColorUtil.color("&7Пакетов/тик (avg): &f" + data.getPacketCounter()));
        sender.sendMessage(ColorUtil.color("&7Позиция: &f" + formatLoc(data.getCurrentLocation())));
        sender.sendMessage(ColorUtil.color("&7On ground: &f" + data.isOnGround() + " &8| &7Air ticks: &f" + data.getAirTicks()));

        if (!data.getViolationsMap().isEmpty()) {
            sender.sendMessage(ColorUtil.color("&8&m----------------------------"));
            sender.sendMessage(ColorUtil.color("&7Нарушения по проверкам:"));
            data.getViolationsMap().entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .forEach(e -> sender.sendMessage(ColorUtil.color(
                            "&8- &e" + e.getKey() + "&8: &c" + String.format("%.1f", e.getValue()))));
        }
    }

    private void sendChecksList(CommandSender sender) {
        sender.sendMessage(plugin.messages().getChecksHeader());
        for (Check check : plugin.getCheckManager().getChecks()) {
            String status = check.isEnabled() ? "&aВКЛ" : "&cВЫКЛ";
            sender.sendMessage(plugin.messages().format(plugin.messages().getCheckLine(),
                    "check", check.getName(),
                    "type", check.getType().name(),
                    "description", check.getDescription(),
                    "status", status));
        }
        sender.sendMessage(plugin.messages().getHelpFooter());
    }

    private String formatLoc(org.bukkit.Location loc) {
        if (loc == null) return "null";
        return String.format("[%.1f, %.1f, %.1f]", loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("neosac.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("help", "reload", "info", "verbose", "reset", "checks")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("reset")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("verbose")) {
                return Arrays.asList("on", "off").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
