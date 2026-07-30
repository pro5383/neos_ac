package net.neos.neosac.api;

import net.neos.neosac.check.Check;
import net.neos.neosac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CheckFailEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Check check;
    private final String detail;
    private final double violations;
    private boolean cancelled;

    public CheckFailEvent(@NotNull Player player, @NotNull Check check, @NotNull String detail, double violations) {
        super(true);
        this.player = player;
        this.check = check;
        this.detail = detail;
        this.violations = violations;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Check getCheck() {
        return check;
    }

    @NotNull
    public String getDetail() {
        return detail;
    }

    public double getViolations() {
        return violations;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
