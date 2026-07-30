package net.neos.neosac;

import net.neos.neosac.api.NeosACAPI;
import net.neos.neosac.check.CheckManager;
import net.neos.neosac.command.AlertsCommand;
import net.neos.neosac.command.NeosACCommand;
import net.neos.neosac.config.Config;
import net.neos.neosac.config.Messages;
import net.neos.neosac.data.EntityTracker;
import net.neos.neosac.data.PlayerDataManager;
import net.neos.neosac.data.ViolationStorage;
import net.neos.neosac.listener.PlayerListener;
import net.neos.neosac.listener.WorldListener;
import net.neos.neosac.manager.AlertManager;
import net.neos.neosac.manager.ExemptionManager;
import net.neos.neosac.manager.LogManager;
import net.neos.neosac.manager.PunishmentManager;
import net.neos.neosac.manager.SetbackManager;
import net.neos.neosac.packet.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NeosAC extends JavaPlugin {

    private static NeosAC instance;

    private Config config;
    private Messages messages;

    private PlayerDataManager playerDataManager;
    private EntityTracker entityTracker;
    private ViolationStorage violationStorage;
    private CheckManager checkManager;

    private AlertManager alertManager;
    private LogManager logManager;
    private PunishmentManager punishmentManager;
    private SetbackManager setbackManager;
    private ExemptionManager exemptionManager;

    private PacketHandler packetHandler;

    private NeosACAPI api;

    private boolean packetEventsAvailable;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.config = new Config(this);
        this.messages = new Messages(this);

        this.violationStorage = new ViolationStorage(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.entityTracker = new EntityTracker();
        this.exemptionManager = new ExemptionManager(this);

        this.logManager = new LogManager(this);
        this.alertManager = new AlertManager(this);
        this.setbackManager = new SetbackManager(this);
        this.punishmentManager = new PunishmentManager(this);

        this.checkManager = new CheckManager(this);
        this.checkManager.registerDefaults();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldListener(this), this);

        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            this.packetHandler = new PacketHandler(this);
            this.packetHandler.init();
            this.packetEventsAvailable = true;
            getLogger().info("PacketEvents обнаружен — пакетные проверки активированы.");
        } catch (ClassNotFoundException e) {
            this.packetEventsAvailable = false;
            getLogger().warning("PacketEvents НЕ обнаружен! Пакетные проверки (PacketOrder, Timer, BadPackets) будут ограничены.");
            getLogger().warning("Установите packetevents для полноценной работы: https://github.com/retrooper/packetevents");
        }

        NeosACCommand mainCmd = new NeosACCommand(this);
        AlertsCommand alertsCmd = new AlertsCommand(this);
        getCommand("neosac").setExecutor(mainCmd);
        getCommand("neosac").setTabCompleter(mainCmd);
        getCommand("neosacalerts").setExecutor(alertsCmd);

        this.api = new NeosACAPI(this);

        Bukkit.getOnlinePlayers().forEach(playerDataManager::create);

        getLogger().info("==================================================");
        getLogger().info(" NeosAC v" + getDescription().getVersion() + " успешно включён.");
        getLogger().info(" Проверок зарегистрировано: " + checkManager.getChecks().size());
        getLogger().info(" PacketEvents: " + (packetEventsAvailable ? "ОК" : "ОТСУТСТВУЕТ"));
        getLogger().info(" Java: " + System.getProperty("java.version"));
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                var data = playerDataManager.getPlayerData(p.getUniqueId());
                if (data != null && violationStorage != null) {
                    violationStorage.save(data);
                }
                playerDataManager.remove(p.getUniqueId());
            });
        }

        if (packetHandler != null) {
            packetHandler.shutdown();
        }

        if (logManager != null) {
            logManager.shutdown();
        }

        getLogger().info("NeosAC выключен.");
        instance = null;
    }

    public void reload() {
        reloadConfig();
        config.load();
        messages.load();
        checkManager.reloadAll();
        getLogger().info("Конфигурация перезагружена.");
    }

    public static NeosAC getInstance() {
        return instance;
    }

    public Config configuration() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EntityTracker getEntityTracker() {
        return entityTracker;
    }

    public ViolationStorage getViolationStorage() {
        return violationStorage;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public SetbackManager getSetbackManager() {
        return setbackManager;
    }

    public ExemptionManager getExemptionManager() {
        return exemptionManager;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    public NeosACAPI getApi() {
        return api;
    }

    public boolean isPacketEventsAvailable() {
        return packetEventsAvailable;
    }
}
