package net.neos.neosac.data;

import net.neos.neosac.NeosAC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class ViolationStorage {

    private static final long MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private final NeosAC plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ViolationStorage(@NotNull NeosAC plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "violations.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void load(@NotNull PlayerData data) {
        String base = data.getUuid().toString();
        ConfigurationSection sec = yaml.getConfigurationSection(base);
        if (sec == null) return;

        long lastSeen = sec.getLong("lastSeen", 0L);
        if (lastSeen > 0 && (System.currentTimeMillis() - lastSeen) > MAX_AGE_MS) {
            yaml.set(base, null);
            saveFile();
            return;
        }

        ConfigurationSection v = sec.getConfigurationSection("violations");
        if (v == null) return;
        for (String key : v.getKeys(false)) {
            double vl = v.getDouble(key);
            if (vl > 0) {
                data.getViolationsMap().put(key, vl);
            }
        }
    }

    public void save(@NotNull PlayerData data) {
        String base = data.getUuid().toString();
        Map<String, Double> vios = data.getViolationsMap();

        if (vios.isEmpty()) {
            yaml.set(base, null);
            saveFile();
            return;
        }

        yaml.set(base + ".lastSeen", System.currentTimeMillis());
        yaml.set(base + ".violations", null);
        for (Map.Entry<String, Double> e : vios.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                yaml.set(base + ".violations." + e.getKey(), e.getValue());
            }
        }
        saveFile();
    }

    public void clear(@NotNull UUID uuid) {
        yaml.set(uuid.toString(), null);
        saveFile();
    }

    private synchronized void saveFile() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить violations.yml: " + e.getMessage());
        }
    }
}
