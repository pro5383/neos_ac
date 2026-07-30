package net.neos.neosac.checks.world;

import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class NukerCheck extends Check implements PacketAware {

    private static final int MAX_BLOCKS_PER_WINDOW = 2;
    private static final long WINDOW_MS = 200;

    private final java.util.Map<java.util.UUID, java.util.Deque<Long>> breakHistory =
            new java.util.concurrent.ConcurrentHashMap<>();

    public NukerCheck(NeosAC plugin) {
        super(plugin, "Nuker", CheckType.WORLD,
                "Детектор Nuker-чита (массовое разрушение блоков)");
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        var blockPos = dig.getBlockPosition();
        if (blockPos == null) return;

        DiggingAction action = dig.getAction();
        if (action != DiggingAction.START_DIGGING
                && action != DiggingAction.FINISHED_DIGGING) {
            return;
        }

        long now = System.currentTimeMillis();
        java.util.Deque<Long> history = breakHistory.computeIfAbsent(player.getUniqueId(), k -> new java.util.concurrent.ConcurrentLinkedDeque<>());

        while (!history.isEmpty() && now - history.peekFirst() > WINDOW_MS) {
            history.pollFirst();
        }

        history.addLast(now);

        if (history.size() > MAX_BLOCKS_PER_WINDOW) {
            fail(data, "Nuker: %d блоков за %dms (макс %d)",
                    history.size(), WINDOW_MS, MAX_BLOCKS_PER_WINDOW);
            history.clear();
        }

        if (data.getLastBrokenBlock() != null) {
            Location last = data.getLastBrokenBlock();
            Location current = new Location(player.getWorld(), blockPos.x, blockPos.y, blockPos.z);
            double dist = last.distance(current);

            if (dist > 4.0 && history.size() > 1) {
                long timeDelta = now - data.getLastBlockBreak();
                if (timeDelta < 100) {
                    fail(data, "Nuker: блоки далеко (%.2f) за %dms", dist, timeDelta);
                }
            }
        }
    }

    @Override
    protected double getViolationAmount() {
        return 2.0;
    }
}
