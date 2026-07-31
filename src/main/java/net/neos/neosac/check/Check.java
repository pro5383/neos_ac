package net.neos.neosac.check;

import net.neos.neosac.NeosAC;
import net.neos.neosac.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public abstract class Check {

    protected final NeosAC plugin;
    protected final String name;
    protected final CheckType type;
    protected final String description;

    protected boolean enabled = true;
    protected double maxViolations = 10.0;
    protected double setbackViolations = 5.0;
    protected double banViolations = 20.0;

    public Check(@NotNull NeosAC plugin, @NotNull String name, @NotNull CheckType type, @NotNull String description) {
        this.plugin = plugin;
        this.name = name;
        this.type = type;
        this.description = description;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks." + name.toLowerCase());
        if (section != null) {
            this.enabled = section.getBoolean("enabled", enabled);
            this.maxViolations = section.getDouble("max-violations", maxViolations);
            this.setbackViolations = section.getDouble("setback-violations", setbackViolations);
            this.banViolations = section.getDouble("ban-violations", banViolations);
        }
    }

    public void reload() {
        loadConfig();
    }

    public void fail(String detail, Object... args) {
        fail(null, detail, args);
    }

    public void fail(PlayerData data, String detail, Object... args) {
        if (data == null) {
            return;
        }

        String formatted = args.length == 0 ? detail : String.format(detail, args);
        double violationAmount = getViolationAmount();
        data.addViolation(this, violationAmount);

        double total = data.getViolations(this);

        plugin.getAlertManager().alert(data.getPlayer(), this, formatted, total);

        plugin.getLogManager().log(data.getPlayer(), this, formatted, total);

        if (total >= setbackViolations && setbackViolations > 0) {
            plugin.getSetbackManager().setback(data, this, formatted);
        }

        if (total >= banViolations && banViolations > 0) {
            plugin.getPunishmentManager().ban(data, this, formatted);
        }
    }

    protected double getViolationAmount() {
        return 1.0;
    }

    /**
     * Должна ли проверка выполняться, пока игрок планирует на элитрах.
     * По умолчанию нет (планирование глобально освобождено). Проверки с собственной
     * моделью полёта на элитрах переопределяют на {@code true}.
     */
    public boolean allowDuringGliding() {
        return false;
    }

    public String getName() {
        return name;
    }

    public CheckType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getMaxViolations() {
        return maxViolations;
    }

    public double getSetbackViolations() {
        return setbackViolations;
    }

    public double getBanViolations() {
        return banViolations;
    }

    public String getDisplayName() {
        return type.name() + "/" + name;
    }
}
