package net.neos.neosac.setback;

import net.neos.neosac.data.PlayerData;

public class Setback {

    private final PlayerData data;
    private final String reason;
    private final long timestamp;

    public Setback(PlayerData data, String reason) {
        this.data = data;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public PlayerData getData() {
        return data;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
