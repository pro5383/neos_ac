package net.neos.neosac.util;

import org.jetbrains.annotations.NotNull;

public final class ColorUtil {

    private ColorUtil() {}

    public static String color(@NotNull String input) {
        if (input == null) return "";
        return input.replace("&", "§");
    }

    public static String strip(@NotNull String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
