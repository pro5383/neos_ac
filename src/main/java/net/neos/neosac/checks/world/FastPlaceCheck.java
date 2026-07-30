package net.neos.neosac.checks.world;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.entity.Player;

public class FastPlaceCheck extends Check implements PacketAware {

    private static final int MAX_PLACES_PER_SECOND = 16;

    private static final long DUPLICATE_MS = 30;

    private final java.util.Map<java.util.UUID, java.util.Deque<Long>> placeHistory =
            new java.util.concurrent.ConcurrentHashMap<>();

    public FastPlaceCheck(NeosAC plugin) {
        super(plugin, "FastPlace", CheckType.WORLD,
                "Детектор FastPlace (слишком быстрая установка блоков, Scaffold)");
    }

    @Override
    public void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        long now = System.currentTimeMillis();

        java.util.Deque<Long> history = placeHistory.computeIfAbsent(
                player.getUniqueId(), k -> new java.util.concurrent.ConcurrentLinkedDeque<>());

        Long prev = history.peekLast();
        if (prev != null && (now - prev) < DUPLICATE_MS) {
            return;
        }

        while (!history.isEmpty() && now - history.peekFirst() > 1000) {
            history.pollFirst();
        }
        history.addLast(now);

        if (history.size() > MAX_PLACES_PER_SECOND) {
            fail(data, "FastPlace: %d установок за 1с (макс %d)",
                    history.size(), MAX_PLACES_PER_SECOND);
            history.clear();
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
