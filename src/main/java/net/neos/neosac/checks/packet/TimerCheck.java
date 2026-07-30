package net.neos.neosac.checks.packet;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.entity.Player;

public class TimerCheck extends Check implements PacketAware {

    private static final int MAX_PACKETS_PER_SECOND = 25;

    private static final long WINDOW_MS = 1000;
    private static final long GRACE_MS = 1000;

    private final java.util.Map<java.util.UUID, java.util.Deque<Long>> history =
            new java.util.concurrent.ConcurrentHashMap<>();

    public TimerCheck(NeosAC plugin) {
        super(plugin, "Timer", CheckType.PACKET,
                "Проверка на Timer-чит (частота движений > 20/сек)");
    }

    @Override
    public void onFlying(Player player, PlayerData data, WrapperPlayClientPlayerFlying flying) {
        if (!flying.hasPositionChanged() && !flying.hasRotationChanged()) {
            return;
        }

        long now = System.currentTimeMillis();

        if ((now - data.getLastSetbackTime()) < GRACE_MS) {
            java.util.Deque<Long> h = history.get(player.getUniqueId());
            if (h != null) h.clear();
            return;
        }

        java.util.Deque<Long> window = history.computeIfAbsent(
                player.getUniqueId(), k -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        window.addLast(now);

        while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MS) {
            window.pollFirst();
        }

        int count = window.size();
        if (count > MAX_PACKETS_PER_SECOND) {
            double ratio = count / 20.0;
            fail(data, "Timer: %d движений/сек > %d (~x%.2f)",
                    count, MAX_PACKETS_PER_SECOND, ratio);
        }
    }

    @Override
    protected double getViolationAmount() {
        return 0.5;
    }
}
