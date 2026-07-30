package net.neos.neosac.listener;

import net.neos.neosac.NeosAC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {

    private final NeosAC plugin;

    public PlayerListener(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().create(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        var data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            plugin.getViolationStorage().save(data);
        }
        plugin.getPlayerDataManager().remove(player.getUniqueId());

        var vc = plugin.getCheckManager().getCheck("Vehicle");
        if (vc instanceof net.neos.neosac.checks.simulation.VehicleCheck vehicleCheck) {
            vehicleCheck.clear(player.getUniqueId());
        }
        plugin.getExemptionManager().remove(player.getUniqueId());
        plugin.getAlertManager().toggleAlerts(player.getUniqueId());
        if (plugin.getAlertManager().hasAlerts(player.getUniqueId())) {
            plugin.getAlertManager().toggleAlerts(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;

        data.setCurrentLocation(event.getTo());
        data.setLastSetbackLocation(event.getTo());
        data.setLastSetbackTime(System.currentTimeMillis());
        data.getPhysics().reset(event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setCurrentLocation(event.getRespawnLocation());
        data.setLastSetbackLocation(event.getRespawnLocation());
        data.setLastSetbackTime(System.currentTimeMillis());
        data.getPhysics().reset(event.getRespawnLocation());
        data.resetAllViolations();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setCurrentLocation(event.getPlayer().getLocation());
        data.setLastSetbackLocation(event.getPlayer().getLocation());
        data.setLastSetbackTime(System.currentTimeMillis());
        data.getPhysics().reset(event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled()) return;
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setFlying(event.getNewGameMode().name().equals("CREATIVE") || event.getNewGameMode().name().equals("SPECTATOR"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        data.setLastSetbackTime(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
        }
    }
}
