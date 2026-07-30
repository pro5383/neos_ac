package net.neos.neosac.checks.world;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WrongBlockCheck extends Check implements PacketAware {

    private static final double MAX_DISTANCE_DIFF = 0.5;

    public WrongBlockCheck(NeosAC plugin) {
        super(plugin, "WrongBlock", CheckType.WORLD,
                "Проверка блока, с которым взаимодействует игрок (Scaffold, Tower)");
    }

    @Override
    public void onBlockPlace(Player player, PlayerData data, WrapperPlayClientPlayerBlockPlacement place) {
        var blockPos = place.getBlockPosition();
        if (blockPos == null) return;

        if (System.currentTimeMillis() - data.getJoinTime() < 2000) return;

        var rayTraceBlocks = player.rayTraceBlocks(5.0);
        if (rayTraceBlocks == null) {
            Location eye = player.getEyeLocation();
            Location target = new Location(player.getWorld(), blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
            double distance = eye.distance(target);
            if (distance > 4.5) {
                fail(data, "WrongBlock: placement в воздухе на дистанции %.2f", distance);
            }
            return;
        }

        org.bukkit.block.Block targetBlock = rayTraceBlocks.getHitBlock();
        if (targetBlock == null) return;

        if (targetBlock.getX() != blockPos.x || targetBlock.getY() != blockPos.y
                || targetBlock.getZ() != blockPos.z) {
            Location eye = player.getEyeLocation();
            Location sentTarget = new Location(player.getWorld(),
                    blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
            Location realTarget = targetBlock.getLocation().add(0.5, 0.5, 0.5);

            double distDiff = eye.distance(sentTarget) - eye.distance(realTarget);

            if (Math.abs(distDiff) > MAX_DISTANCE_DIFF) {
                fail(data, "WrongBlock: ожидался [%d,%d,%d], пришёл [%d,%d,%d], distDiff=%.2f",
                        targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(),
                        blockPos.x, blockPos.y, blockPos.z, distDiff);
            }
        }
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        var blockPos = dig.getBlockPosition();
        if (blockPos == null) return;

        if (System.currentTimeMillis() - data.getJoinTime() < 2000) return;

        var rayTraceBlocks = player.rayTraceBlocks(5.0);
        if (rayTraceBlocks == null) {
            fail(data, "WrongBlock: dig без взгляда на блок [%d,%d,%d]",
                    blockPos.x, blockPos.y, blockPos.z);
            return;
        }

        org.bukkit.block.Block targetBlock = rayTraceBlocks.getHitBlock();
        if (targetBlock == null) return;

        if (targetBlock.getX() != blockPos.x || targetBlock.getY() != blockPos.y
                || targetBlock.getZ() != blockPos.z) {
            Location eye = player.getEyeLocation();
            Location sentTarget = new Location(player.getWorld(),
                    blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
            double distance = eye.distance(sentTarget);

            if (distance > 4.5) {
                fail(data, "WrongBlock: dig вне reach [%d,%d,%d], dist=%.2f",
                        blockPos.x, blockPos.y, blockPos.z, distance);
            }
        }
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
