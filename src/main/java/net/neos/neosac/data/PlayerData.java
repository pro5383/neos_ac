package net.neos.neosac.data;

import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.physics.MinecraftPhysics;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final NeosAC plugin;
    private final UUID uuid;
    private final Player player;
    private final MinecraftPhysics physics;

    private final Map<String, Double> violations = new ConcurrentHashMap<>();
    private final Map<String, Long> lastViolationTime = new ConcurrentHashMap<>();

    private volatile Location currentLocation;
    private volatile Location lastLocation;
    private volatile Location lastSetbackLocation;
    private volatile long lastMoveTime;
    private volatile long lastTickPosition;

    private volatile float lastYaw;
    private volatile float lastPitch;
    private volatile float currentYaw;
    private volatile float currentPitch;
    private volatile long lastRotationTime;

    private volatile long lastPacketTime;
    private volatile long packetCounter;
    private volatile long lastFlyingPacket;

    private volatile long timerLastMillis;
    private volatile double timerBalance;
    private volatile long lastPositionPacket;
    private volatile long lastRotationPacket;
    private volatile long lastPositionRotationPacket;

    private volatile int packetsThisTick;
    private volatile long lastTickReset;
    private volatile boolean awaitingPosition;
    private volatile boolean awaitingRotation;
    private volatile int lastPacketId;

    private volatile long lastAttackTime;
    private volatile UUID lastAttackedEntity;
    private volatile double lastAttackDistance;
    private volatile float attackYaw;
    private volatile float attackPitch;
    private volatile long lastRotationBeforeAttack;

    private volatile long lastBlockBreak;
    private volatile long lastBlockPlace;
    private volatile int blocksBrokenThisTick;
    private volatile int blocksPlacedThisTick;
    private volatile Location lastBrokenBlock;
    private volatile Location lastPlacedBlock;

    private volatile boolean onGround;
    private volatile boolean wasOnGround;
    private volatile boolean inLiquid;
    private volatile boolean onClimbable;
    private volatile boolean inWeb;
    private volatile boolean sneaking;
    private volatile boolean sprinting;
    private volatile boolean flying;
    private volatile boolean gliding;
    private volatile boolean riptiding;

    private volatile double simulatedY;
    private volatile double lastSimulatedY;
    private volatile double deltaY;
    private volatile double lastDeltaY;
    private volatile int airTicks;
    private volatile int groundTicks;
    private volatile double totalFallDistance;

    private volatile int ticksExisted;
    private volatile long joinTime;
    private volatile boolean verbose;
    private volatile long lastSetbackTime;

    private volatile boolean setbackRequested;

    private volatile boolean wasInVehicle;
    private volatile long dismountTime;
    private volatile double lastVehicleY;
    private volatile int vehicleAirTicks;

    public PlayerData(@NotNull NeosAC plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.uuid = player.getUniqueId();
        this.player = player;
        this.physics = new MinecraftPhysics(plugin, this);
        this.currentLocation = player.getLocation().clone();
        this.lastLocation = player.getLocation().clone();
        this.lastSetbackLocation = player.getLocation().clone();
        this.joinTime = System.currentTimeMillis();
        this.lastPacketTime = System.currentTimeMillis();
    }

    public void addViolation(Check check, double amount) {
        String key = check.getName().toLowerCase();
        violations.merge(key, amount, Double::sum);
        lastViolationTime.put(key, System.currentTimeMillis());
    }

    public double getViolations(Check check) {
        return violations.getOrDefault(check.getName().toLowerCase(), 0.0);
    }

    public void resetViolations(Check check) {
        violations.remove(check.getName().toLowerCase());
    }

    public void resetAllViolations() {
        violations.clear();
        lastViolationTime.clear();
    }

    public void decayViolations() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastViolationTime.entrySet()) {
            long elapsed = now - entry.getValue();
            if (elapsed > 5000) {
                String check = entry.getKey();
                violations.computeIfPresent(check, (k, v) -> {
                    double decayed = v - 0.05;
                    return decayed <= 0 ? null : decayed;
                });
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public Player getPlayer() {
        return player;
    }

    public MinecraftPhysics getPhysics() {
        return physics;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.lastLocation = this.currentLocation;
        this.currentLocation = currentLocation.clone();
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public Location getLastSetbackLocation() {
        return lastSetbackLocation;
    }

    public void setLastSetbackLocation(Location lastSetbackLocation) {
        this.lastSetbackLocation = lastSetbackLocation;
    }

    public Map<String, Double> getViolationsMap() {
        return violations;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public void setCurrentYaw(float currentYaw) {
        this.currentYaw = currentYaw;
    }

    public float getCurrentPitch() {
        return currentPitch;
    }

    public void setCurrentPitch(float currentPitch) {
        this.currentPitch = currentPitch;
    }

    public long getLastRotationTime() {
        return lastRotationTime;
    }

    public void setLastRotationTime(long lastRotationTime) {
        this.lastRotationTime = lastRotationTime;
    }

    public long getLastFlyingPacket() {
        return lastFlyingPacket;
    }

    public void setLastFlyingPacket(long lastFlyingPacket) {
        this.lastFlyingPacket = lastFlyingPacket;
    }

    public long getLastPositionPacket() {
        return lastPositionPacket;
    }

    public void setLastPositionPacket(long lastPositionPacket) {
        this.lastPositionPacket = lastPositionPacket;
    }

    public long getLastRotationPacket() {
        return lastRotationPacket;
    }

    public void setLastRotationPacket(long lastRotationPacket) {
        this.lastRotationPacket = lastRotationPacket;
    }

    public long getLastPositionRotationPacket() {
        return lastPositionRotationPacket;
    }

    public void setLastPositionRotationPacket(long lastPositionRotationPacket) {
        this.lastPositionRotationPacket = lastPositionRotationPacket;
    }

    public int getPacketsThisTick() {
        return packetsThisTick;
    }

    public void incrementPacketsThisTick() {
        this.packetsThisTick++;
    }

    public void resetPacketsThisTick() {
        this.packetsThisTick = 0;
    }

    public int getLastPacketId() {
        return lastPacketId;
    }

    public void incrementLastPacketId() {
        this.lastPacketId++;
    }

    public boolean isAwaitingPosition() {
        return awaitingPosition;
    }

    public void setAwaitingPosition(boolean awaitingPosition) {
        this.awaitingPosition = awaitingPosition;
    }

    public boolean isAwaitingRotation() {
        return awaitingRotation;
    }

    public void setAwaitingRotation(boolean awaitingRotation) {
        this.awaitingRotation = awaitingRotation;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public UUID getLastAttackedEntity() {
        return lastAttackedEntity;
    }

    public void setLastAttackedEntity(UUID lastAttackedEntity) {
        this.lastAttackedEntity = lastAttackedEntity;
    }

    public double getLastAttackDistance() {
        return lastAttackDistance;
    }

    public void setLastAttackDistance(double lastAttackDistance) {
        this.lastAttackDistance = lastAttackDistance;
    }

    public float getAttackYaw() {
        return attackYaw;
    }

    public void setAttackYaw(float attackYaw) {
        this.attackYaw = attackYaw;
    }

    public float getAttackPitch() {
        return attackPitch;
    }

    public void setAttackPitch(float attackPitch) {
        this.attackPitch = attackPitch;
    }

    public long getLastRotationBeforeAttack() {
        return lastRotationBeforeAttack;
    }

    public void setLastRotationBeforeAttack(long lastRotationBeforeAttack) {
        this.lastRotationBeforeAttack = lastRotationBeforeAttack;
    }

    public long getLastBlockBreak() {
        return lastBlockBreak;
    }

    public void setLastBlockBreak(long lastBlockBreak) {
        this.lastBlockBreak = lastBlockBreak;
    }

    public long getLastBlockPlace() {
        return lastBlockPlace;
    }

    public void setLastBlockPlace(long lastBlockPlace) {
        this.lastBlockPlace = lastBlockPlace;
    }

    public int getBlocksBrokenThisTick() {
        return blocksBrokenThisTick;
    }

    public void incrementBlocksBrokenThisTick() {
        this.blocksBrokenThisTick++;
    }

    public void resetBlocksBrokenThisTick() {
        this.blocksBrokenThisTick = 0;
    }

    public int getBlocksPlacedThisTick() {
        return blocksPlacedThisTick;
    }

    public void incrementBlocksPlacedThisTick() {
        this.blocksPlacedThisTick++;
    }

    public void resetBlocksPlacedThisTick() {
        this.blocksPlacedThisTick = 0;
    }

    public Location getLastBrokenBlock() {
        return lastBrokenBlock;
    }

    public void setLastBrokenBlock(Location lastBrokenBlock) {
        this.lastBrokenBlock = lastBrokenBlock;
    }

    public Location getLastPlacedBlock() {
        return lastPlacedBlock;
    }

    public void setLastPlacedBlock(Location lastPlacedBlock) {
        this.lastPlacedBlock = lastPlacedBlock;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.wasOnGround = this.onGround;
        this.onGround = onGround;
    }

    public boolean wasOnGround() {
        return wasOnGround;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void setInLiquid(boolean inLiquid) {
        this.inLiquid = inLiquid;
    }

    public boolean isOnClimbable() {
        return onClimbable;
    }

    public void setOnClimbable(boolean onClimbable) {
        this.onClimbable = onClimbable;
    }

    public boolean isInWeb() {
        return inWeb;
    }

    public void setInWeb(boolean inWeb) {
        this.inWeb = inWeb;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isFlying() {
        return flying;
    }

    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isGliding() {
        return gliding;
    }

    public void setGliding(boolean gliding) {
        this.gliding = gliding;
    }

    public boolean isRiptiding() {
        return riptiding;
    }

    public void setRiptiding(boolean riptiding) {
        this.riptiding = riptiding;
    }

    public double getSimulatedY() {
        return simulatedY;
    }

    public void setSimulatedY(double simulatedY) {
        this.lastSimulatedY = this.simulatedY;
        this.simulatedY = simulatedY;
    }

    public double getLastSimulatedY() {
        return lastSimulatedY;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public void pushDeltaY(double deltaY) {
        this.lastDeltaY = this.deltaY;
        this.deltaY = deltaY;
    }

    public double getLastDeltaY() {
        return lastDeltaY;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public void setAirTicks(int airTicks) {
        this.airTicks = airTicks;
    }

    public void incrementAirTicks() {
        this.airTicks++;
    }

    public void resetAirTicks() {
        this.airTicks = 0;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public void setGroundTicks(int groundTicks) {
        this.groundTicks = groundTicks;
    }

    public void incrementGroundTicks() {
        this.groundTicks++;
    }

    public void resetGroundTicks() {
        this.groundTicks = 0;
    }

    public double getTotalFallDistance() {
        return totalFallDistance;
    }

    public void setTotalFallDistance(double totalFallDistance) {
        this.totalFallDistance = totalFallDistance;
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public void incrementTicksExisted() {
        this.ticksExisted++;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public long getLastSetbackTime() {
        return lastSetbackTime;
    }

    public void setLastSetbackTime(long lastSetbackTime) {
        this.lastSetbackTime = lastSetbackTime;
    }

    public void requestSetback() {
        this.setbackRequested = true;
    }

    public boolean consumeSetbackRequested() {
        boolean r = this.setbackRequested;
        this.setbackRequested = false;
        return r;
    }

    public void resetLocationTo(Location loc) {
        if (loc == null) return;
        this.lastLocation = loc.clone();
        this.currentLocation = loc.clone();
        this.deltaY = 0;
        this.lastDeltaY = 0;
    }

    public boolean wasInVehicle() {
        return wasInVehicle;
    }

    public void setWasInVehicle(boolean wasInVehicle) {
        this.wasInVehicle = wasInVehicle;
    }

    public long getDismountTime() {
        return dismountTime;
    }

    public void setDismountTime(long dismountTime) {
        this.dismountTime = dismountTime;
    }

    public double getLastVehicleY() {
        return lastVehicleY;
    }

    public void setLastVehicleY(double lastVehicleY) {
        this.lastVehicleY = lastVehicleY;
    }

    public int getVehicleAirTicks() {
        return vehicleAirTicks;
    }

    public void incrementVehicleAirTicks() {
        this.vehicleAirTicks++;
    }

    public void resetVehicleAirTicks() {
        this.vehicleAirTicks = 0;
    }

    public long getLastPacketTime() {
        return lastPacketTime;
    }

    public void setLastPacketTime(long lastPacketTime) {
        this.lastPacketTime = lastPacketTime;
    }

    public long getPacketCounter() {
        return packetCounter;
    }

    public void incrementPacketCounter() {
        this.packetCounter++;
    }

    public void resetPacketCounter() {
        this.packetCounter = 0;
    }

    public long getTimerLastMillis() {
        return timerLastMillis;
    }

    public void setTimerLastMillis(long timerLastMillis) {
        this.timerLastMillis = timerLastMillis;
    }

    public double getTimerBalance() {
        return timerBalance;
    }

    public void setTimerBalance(double timerBalance) {
        this.timerBalance = timerBalance;
    }
}
