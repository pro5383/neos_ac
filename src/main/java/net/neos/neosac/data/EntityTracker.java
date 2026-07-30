package net.neos.neosac.data;

import net.neos.neosac.raytrace.AABB;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker {

    public static final class Snapshot {
        public final int id;
        public final UUID uuid;
        public final UUID worldId;
        public final double x, y, z;
        public final AABB box;
        public final boolean npc;
        public final String typeName;

        Snapshot(int id, UUID uuid, UUID worldId, double x, double y, double z,
                 AABB box, boolean npc, String typeName) {
            this.id = id;
            this.uuid = uuid;
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.box = box;
            this.npc = npc;
            this.typeName = typeName;
        }
    }

    private volatile Map<Integer, Snapshot> byId = new ConcurrentHashMap<>();

    public void update() {
        Map<Integer, Snapshot> fresh = new ConcurrentHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            UUID wid = world.getUID();
            for (Entity e : world.getEntities()) {
                if (!(e instanceof LivingEntity)) continue;
                try {
                    AABB box = AABB.fromBukkit(e.getBoundingBox());
                    var loc = e.getLocation();
                    fresh.put(e.getEntityId(), new Snapshot(
                            e.getEntityId(), e.getUniqueId(), wid,
                            loc.getX(), loc.getY(), loc.getZ(),
                            box, e.hasMetadata("NPC"), e.getType().name()));
                } catch (Exception ignored) {
                }
            }
        }
        this.byId = fresh;
    }

    public Snapshot get(int entityId) {
        return byId.get(entityId);
    }

    public void clear() {
        byId = new ConcurrentHashMap<>();
    }
}
