package net.neos.neosac.manager;

import net.neos.neosac.NeosAC;
import net.neos.neosac.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ExemptionManager {

    private final NeosAC plugin;

    private final Map<java.util.UUID, Set<String>> temporaryExemptions = new ConcurrentHashMap<>();

    private static final long JOIN_GRACE_MS = 5000;

    public ExemptionManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public boolean isExempt(PlayerData data) {
        return isExempt(data, false);
    }

    public boolean isExempt(PlayerData data, boolean ignoreVehicle) {
        if (data == null || data.getPlayer() == null) return true;
        Player player = data.getPlayer();

        if (player.hasPermission("neosac.bypass")) return true;

        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return true;

        if (player.isFlying() || player.getAllowFlight()) return true;

        if (player.isGliding()) return true;

        if (player.isRiptiding()) return true;

        if (!ignoreVehicle && player.isInsideVehicle()) return true;

        if (System.currentTimeMillis() - data.getJoinTime() < JOIN_GRACE_MS) return true;

        int ping = player.getPing();
        if (ping > 800) return true;

        double tps = net.neos.neosac.util.ServerUtil.getTps();
        if (tps < 17.0) return true;

        Set<String> exemptions = temporaryExemptions.get(player.getUniqueId());
        if (exemptions != null && !exemptions.isEmpty()) {
            return true;
        }

        if (player.isDead()) return true;

        return false;
    }

    public void exempt(java.util.UUID uuid, String reason) {
        temporaryExemptions.computeIfAbsent(uuid, k -> new CopyOnWriteArraySet<>()).add(reason);
    }

    public void unexempt(java.util.UUID uuid, String reason) {
        Set<String> set = temporaryExemptions.get(uuid);
        if (set != null) {
            set.remove(reason);
            if (set.isEmpty()) {
                temporaryExemptions.remove(uuid);
            }
        }
    }

    public void remove(java.util.UUID uuid) {
        temporaryExemptions.remove(uuid);
    }
}
