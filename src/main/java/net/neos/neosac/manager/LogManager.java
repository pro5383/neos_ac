package net.neos.neosac.manager;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogManager {

    private final NeosAC plugin;
    private final File logsDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;
    private Thread writerThread;

    public LogManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
        this.logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        startWriter();
    }

    private void startWriter() {
        writerThread = new Thread(this::writerLoop, "NeosAC-LogWriter");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    private void writerLoop() {
        while (running || !queue.isEmpty()) {
            try {
                LogEntry entry;
                while ((entry = queue.poll()) != null) {
                    writeToFile(entry);
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[NeosAC] Ошибка записи лога: " + e.getMessage());
            }
        }
    }

    private void writeToFile(LogEntry entry) {
        if (!plugin.configuration().isLogToFile()) return;

        String fileName = "violations-" + fileDateFormat.format(new Date(entry.timestamp)) + ".log";
        File logFile = new File(logsDir, fileName);

        if (logFile.exists() && logFile.length() > plugin.configuration().getMaxLogSizeMb() * 1024L * 1024L) {
            File archived = new File(logsDir, fileName.replace(".log", "-archive-" + System.currentTimeMillis() + ".log"));
            logFile.renameTo(archived);
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            out.println(entry.formattedLine);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[NeosAC] Не удалось записать лог: " + e.getMessage());
        }
    }

    public void log(Player player, Check check, String detail, double vLevel) {
        if (player == null || !plugin.configuration().isLogToFile()) return;

        long timestamp = System.currentTimeMillis();
        String dateStr = dateFormat.format(new Date(timestamp));
        String line = String.format("[%s] %s | %s | V=%.2f | %s",
                dateStr, player.getName(), check.getDisplayName(), vLevel, detail);

        queue.add(new LogEntry(timestamp, line));
    }

    public void shutdown() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record LogEntry(long timestamp, String formattedLine) {}
}
