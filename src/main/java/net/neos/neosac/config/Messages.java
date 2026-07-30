package net.neos.neosac.config;

import net.neos.neosac.NeosAC;
import net.neos.neosac.util.ColorUtil;
import org.jetbrains.annotations.NotNull;

public class Messages {

    private final NeosAC plugin;
    private String prefix;
    private String alert;
    private String alertVerbose;
    private String setback;
    private String ban;
    private String noPermission;
    private String unknownCommand;
    private String playerOnly;
    private String playerNotFound;
    private String configReloaded;
    private String violationsReset;
    private String verboseEnabled;
    private String verboseDisabled;
    private String helpHeader;
    private String helpLine;
    private String infoLine;
    private String checksHeader;
    private String checkLine;
    private String helpFooter;

    public Messages(@NotNull NeosAC plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveResource("messages.yml", false);
        var cfg = plugin.getConfig();
        var messagesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "messages.yml"));

        prefix = ColorUtil.color(messagesConfig.getString("prefix", "&8[&cNeosAC&8] &r"));
        alert = ColorUtil.color(messagesConfig.getString("alert",
                "%prefix%&c%player% &7сработал &e%check% &7(&cV:%vlevel%&7) &8| &f%detail%"));
        alertVerbose = ColorUtil.color(messagesConfig.getString("alert-verbose",
                "%prefix%&c%player% &7сработал &e%check% &7(&cV:%vlevel%&7) &8| &f%detail% &8| &7ping: %ping%ms &8| &7tps: %tps%"));
        setback = ColorUtil.color(messagesConfig.getString("setback",
                "%prefix%&c%player% &7вернул(а) позицию: &f%check%"));
        ban = ColorUtil.color(messagesConfig.getString("ban",
                "%prefix%&c%player% &7забанен за &f%check% &7(&cV:%vlevel%&7)"));
        noPermission = ColorUtil.color(messagesConfig.getString("no-permission",
                "%prefix%&cУ вас нет прав для этой команды."));
        unknownCommand = ColorUtil.color(messagesConfig.getString("unknown-command",
                "%prefix%&cНеизвестная команда. Используйте &f/neosac help"));
        playerOnly = ColorUtil.color(messagesConfig.getString("player-only",
                "%prefix%&cЭта команда только для игроков."));
        playerNotFound = ColorUtil.color(messagesConfig.getString("player-not-found",
                "%prefix%&cИгрок не найден."));
        configReloaded = ColorUtil.color(messagesConfig.getString("config-reloaded",
                "%prefix%&aКонфигурация перезагружена."));
        violationsReset = ColorUtil.color(messagesConfig.getString("violations-reset",
                "%prefix%&aНарушения для &f%player% &aсброшены."));
        verboseEnabled = ColorUtil.color(messagesConfig.getString("verbose-enabled",
                "%prefix%&aПодробные алерты включены."));
        verboseDisabled = ColorUtil.color(messagesConfig.getString("verbose-disabled",
                "%prefix%&cПодробные алерты выключены."));
        helpHeader = ColorUtil.color(messagesConfig.getString("help-header",
                "&8&m==========&r &cNeosAC &7Help &8&m=========="));
        helpLine = ColorUtil.color(messagesConfig.getString("help-line",
                "&c/%command% &7- %description%"));
        infoLine = ColorUtil.color(messagesConfig.getString("info-line",
                "&7Игрок: &f%player% &8| &7Нарушений: &c%violations%"));
        checksHeader = ColorUtil.color(messagesConfig.getString("checks-header",
                "&8&m==========&r &cNeosAC &7Проверки &8&m=========="));
        checkLine = ColorUtil.color(messagesConfig.getString("check-line",
                "&e%check% &8(&7%type%&8) &7- %description% &8| &a%status%"));
        helpFooter = ColorUtil.color(messagesConfig.getString("help-footer",
                "&8&m==============================="));
    }

    public String format(String message, String... replacements) {
        String result = message.replace("%prefix%", prefix);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return result;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getAlert() {
        return alert;
    }

    public String getAlertVerbose() {
        return alertVerbose;
    }

    public String getSetback() {
        return setback;
    }

    public String getBan() {
        return ban;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUnknownCommand() {
        return unknownCommand;
    }

    public String getPlayerOnly() {
        return playerOnly;
    }

    public String getPlayerNotFound() {
        return playerNotFound;
    }

    public String getConfigReloaded() {
        return configReloaded;
    }

    public String getViolationsReset() {
        return violationsReset;
    }

    public String getVerboseEnabled() {
        return verboseEnabled;
    }

    public String getVerboseDisabled() {
        return verboseDisabled;
    }

    public String getHelpHeader() {
        return helpHeader;
    }

    public String getHelpLine() {
        return helpLine;
    }

    public String getInfoLine() {
        return infoLine;
    }

    public String getChecksHeader() {
        return checksHeader;
    }

    public String getCheckLine() {
        return checkLine;
    }

    public String getHelpFooter() {
        return helpFooter;
    }
}
