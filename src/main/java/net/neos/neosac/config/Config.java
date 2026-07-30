package net.neos.neosac.config;

import net.neos.neosac.NeosAC;
import org.jetbrains.annotations.NotNull;

public class Config {

    private final NeosAC plugin;

    private boolean verbose;
    private boolean broadcastSetbacks;
    private boolean logToFile;
    private int maxLogSizeMb;
    private double setbackCooldownMs;
    private boolean preventSetbackSpam;
    private String banCommand;
    private String banReason;
    private boolean banByConsole;
    private int cleanupInterval;

    public Config(@NotNull NeosAC plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        verbose = plugin.getConfig().getBoolean("verbose", false);
        broadcastSetbacks = plugin.getConfig().getBoolean("broadcast-setbacks", false);
        logToFile = plugin.getConfig().getBoolean("log-to-file", true);
        maxLogSizeMb = plugin.getConfig().getInt("max-log-size-mb", 10);
        setbackCooldownMs = plugin.getConfig().getDouble("setback-cooldown-ms", 500.0);
        preventSetbackSpam = plugin.getConfig().getBoolean("prevent-setback-spam", true);
        banCommand = plugin.getConfig().getString("ban-command", "ban %player% %reason%");
        banReason = plugin.getConfig().getString("ban-reason", "[NeosAC] Обнаружено использование читов");
        banByConsole = plugin.getConfig().getBoolean("ban-by-console", true);
        cleanupInterval = plugin.getConfig().getInt("cleanup-interval-seconds", 60);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isBroadcastSetbacks() {
        return broadcastSetbacks;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public int getMaxLogSizeMb() {
        return maxLogSizeMb;
    }

    public double getSetbackCooldownMs() {
        return setbackCooldownMs;
    }

    public boolean isPreventSetbackSpam() {
        return preventSetbackSpam;
    }

    public String getBanCommand() {
        return banCommand;
    }

    public String getBanReason() {
        return banReason;
    }

    public boolean isBanByConsole() {
        return banByConsole;
    }

    public int getCleanupInterval() {
        return cleanupInterval;
    }

    public NeosAC getPlugin() {
        return plugin;
    }
}
