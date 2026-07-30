package net.neos.neosac.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public final class ServerUtil {

    private static Method getHandleMethod;
    private static Method getPingMethod;
    private static Method recentTpsMethod;

    static {
        try {
        } catch (Exception ignored) {}
    }

    private ServerUtil() {}

    public static int getPing(@NotNull Player player) {
        try {
            if (getHandleMethod == null) {
                getHandleMethod = player.getClass().getMethod("getHandle");
            }
            Object entityPlayer = getHandleMethod.invoke(player);
            if (entityPlayer == null) return -1;

            try {
                if (getPingMethod == null) {
                    getPingMethod = entityPlayer.getClass().getMethod("getPing");
                }
                Object ping = getPingMethod.invoke(entityPlayer);
                if (ping instanceof Integer) return (Integer) ping;
            } catch (NoSuchMethodException ignored) {
                try {
                    java.lang.reflect.Field f = entityPlayer.getClass().getField("ping");
                    return f.getInt(entityPlayer);
                } catch (Exception ignored2) {}
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public static double getTps() {
        try {
            try {
                Method m = Bukkit.getServer().getClass().getMethod("getTPS");
                Object result = m.invoke(Bukkit.getServer());
                if (result instanceof double[] tpsArr && tpsArr.length > 0) {
                    return tpsArr[0];
                }
            } catch (NoSuchMethodException ignored) {
                try {
                    if (recentTpsMethod == null) {
                        recentTpsMethod = Bukkit.getServer().getClass().getMethod("recentTps");
                    }
                    Object result = recentTpsMethod.invoke(Bukkit.getServer());
                    if (result instanceof double[] tpsArr && tpsArr.length > 0) {
                        return tpsArr[0];
                    }
                } catch (Exception ignored2) {}
            }
        } catch (Exception ignored) {}
        return 20.0;
    }

    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isPrimaryThread() {
        return Bukkit.isPrimaryThread();
    }
}
