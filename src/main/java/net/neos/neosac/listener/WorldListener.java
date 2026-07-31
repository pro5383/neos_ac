package net.neos.neosac.listener;

import net.neos.neosac.NeosAC;
import net.neos.neosac.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class WorldListener implements Listener {

    private final NeosAC plugin;
    private int taskId;

    public WorldListener(@NotNull NeosAC plugin) {
        this.plugin = plugin;

        startTickTask();
        startDecayTask();
        startCleanupTask();
    }

    private void startTickTask() {
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getEntityTracker().update();

                var vehicleCheck = plugin.getCheckManager().getCheck("Vehicle");
                for (PlayerData data : plugin.getPlayerDataManager().getAll().values()) {
                    data.resetPacketsThisTick();
                    data.resetBlocksBrokenThisTick();
                    data.resetBlocksPlacedThisTick();

                    if (vehicleCheck instanceof net.neos.neosac.checks.simulation.VehicleCheck vc) {
                        org.bukkit.entity.Player p = data.getPlayer();
                        if (p != null && p.isOnline()) {
                            try {
                                vc.tick(p, data);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L).getTaskId();
    }

    private void startDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPlayerDataManager().decayAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPlayerDataManager().cleanup();
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setLastBrokenBlock(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setLastPlacedBlock(event.getBlock().getLocation());
    }

    // Использование фейерверка для разгона на элитрах — даёт грацию симуляции элитр.
    // Резервный сигнал: ловит только клик рукой (пакетное использование он не покрывает).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.FIREWORK_ROCKET) return;
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null) return;
        data.setLastFireworkUse(System.currentTimeMillis());
    }

    // Основной сигнал: спавн сущности-ракеты рядом с планирующим игроком = буст элитр.
    // Надёжнее клика — срабатывает и при пакетном использовании (offhand/swap).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Firework)) return;
        Location loc = event.getEntity().getLocation();
        if (loc.getWorld() == null) return;
        long now = System.currentTimeMillis();
        for (org.bukkit.entity.Player p : loc.getWorld().getPlayers()) {
            if (!p.isGliding()) continue;
            // Буст-ракета спавнится вплотную к игроку (~2.5 блока с запасом на лаг).
            if (p.getLocation().distanceSquared(loc) > 6.25) continue;
            var data = plugin.getPlayerDataManager().getPlayerData(p);
            if (data != null) data.setLastFireworkUse(now);
        }
    }
}

