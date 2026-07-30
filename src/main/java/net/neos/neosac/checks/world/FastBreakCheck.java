package net.neos.neosac.checks.world;

import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import net.neos.neosac.NeosAC;
import net.neos.neosac.check.Check;
import net.neos.neosac.check.CheckType;
import net.neos.neosac.data.PlayerData;
import net.neos.neosac.packet.PacketAware;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FastBreakCheck extends Check implements PacketAware {

    private static final class DigSession {
        final long x, y, z;
        final long startTime;
        final ItemStack toolAtStart;
        DigSession(long x, long y, long z, long startTime, ItemStack toolAtStart) {
            this.x = x; this.y = y; this.z = z;
            this.startTime = startTime;
            this.toolAtStart = toolAtStart;
        }
        boolean samePos(int bx, int by, int bz) {
            return x == bx && y == by && z == bz;
        }
    }

    private final java.util.Map<java.util.UUID, DigSession> sessions =
            new java.util.concurrent.ConcurrentHashMap<>();

    public FastBreakCheck(NeosAC plugin) {
        super(plugin, "FastBreak", CheckType.WORLD,
                "Детектор FastBreak (слишком быстрое разрушение блоков)");
    }

    @Override
    public void onBlockDig(Player player, PlayerData data, WrapperPlayClientPlayerDigging dig) {
        var blockPos = dig.getBlockPosition();
        if (blockPos == null) return;

        DiggingAction action = dig.getAction();

        if (action == DiggingAction.START_DIGGING) {
            DigSession cur = sessions.get(player.getUniqueId());
            if (cur == null || !cur.samePos(blockPos.x, blockPos.y, blockPos.z)) {
                ItemStack tool = player.getInventory().getItemInMainHand().clone();
                sessions.put(player.getUniqueId(), new DigSession(
                        blockPos.x, blockPos.y, blockPos.z,
                        System.currentTimeMillis(), tool));
            }
            return;
        }

        if (action != DiggingAction.FINISHED_DIGGING) return;

        if (player.getGameMode().toString().equals("CREATIVE")) return;

        DigSession session = sessions.remove(player.getUniqueId());
        if (session == null || !session.samePos(blockPos.x, blockPos.y, blockPos.z)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - session.startTime;

        Block block = player.getWorld().getBlockAt(blockPos.x, blockPos.y, blockPos.z);
        Material blockType = block.getType();
        ItemStack tool = session.toolAtStart;

        long minTime = calculateMinBreakTime(blockType, tool, player);

        if (minTime <= 50) return;

        if (elapsed < minTime - 50) {
            fail(data, "FastBreak: %s за %dms (минимум %dms), инструмент=%s",
                    blockType.name(), elapsed, minTime,
                    tool.getType().name());
        }
    }

    private long calculateMinBreakTime(Material blockType, ItemStack tool, Player player) {
        float hardness = getBlockHardness(blockType);

        if (hardness <= 0) return 50;

        double speed = 1.0;

        if (isCorrectTool(blockType, tool)) {
            speed *= 5.0;
        }

        if (tool.containsEnchantment(Enchantment.EFFICIENCY)) {
            int level = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
            speed += level * level + 1;
        }

        PotionEffect haste = player.getPotionEffect(PotionEffectType.HASTE);
        if (haste != null) {
            speed *= 1.0 + 0.2 * (haste.getAmplifier() + 1);
        }

        PotionEffect fatigue = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
        if (fatigue != null) {
            speed /= Math.pow(3, fatigue.getAmplifier() + 1);
        }

        if (player.isInWater() && !tool.containsEnchantment(Enchantment.AQUA_AFFINITY)) {
            speed /= 5.0;
        }

        if (!player.isOnGround()) {
            speed /= 5.0;
        }

        double ticks = hardness * 30.0 / speed;
        return (long) Math.max(50, ticks * 50);
    }

    private float getBlockHardness(Material type) {
        return switch (type.name()) {
            case "STONE", "COBBLESTONE", "COBBLED_DEEPSLATE", "DEEPSLATE" -> 1.5f;
            case "DIRT", "GRASS_BLOCK", "COARSE_DIRT", "ROOTED_DIRT" -> 0.5f;
            case "GRASS" -> 0.0f;
            case "SAND", "RED_SAND" -> 0.5f;
            case "GRAVEL" -> 0.6f;
            case "OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG",
                 "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG", "PALE_OAK_LOG" -> 2.0f;
            case "OAK_PLANKS", "BIRCH_PLANKS", "SPRUCE_PLANKS" -> 2.0f;
            case "IRON_ORE", "DEEPSLATE_IRON_ORE" -> 3.0f;
            case "GOLD_ORE", "DEEPSLATE_GOLD_ORE" -> 3.0f;
            case "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE" -> 3.0f;
            case "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE" -> 3.0f;
            case "COAL_ORE", "DEEPSLATE_COAL_ORE" -> 3.0f;
            case "NETHER_QUARTZ_ORE" -> 3.0f;
            case "OBSIDIAN" -> 50.0f;
            case "BEDROCK" -> -1.0f;
            case "WATER", "LAVA" -> 100.0f;
            case "SNOW", "SNOW_BLOCK" -> 0.2f;
            case "ICE", "PACKED_ICE", "BLUE_ICE" -> 0.5f;
            case "GLASS", "GLASS_PANE" -> 0.3f;
            case "NETHERRACK" -> 0.4f;
            case "END_STONE" -> 3.0f;
            default -> 1.0f;
        };
    }

    private boolean isCorrectTool(Material blockType, ItemStack tool) {
        String blockName = blockType.name();
        String toolName = tool.getType().name();

        if (blockName.contains("STONE") || blockName.contains("ORE") || blockName.contains("COBBLE")
                || blockName.equals("OBSIDIAN") || blockName.contains("DEEPSLATE")
                || blockName.contains("BRICKS") || blockName.equals("NETHER_BRICKS")
                || blockName.contains("TERRACOTTA") || blockName.equals("END_STONE")) {
            return toolName.contains("PICKAXE");
        }
        if (blockName.contains("LOG") || blockName.contains("PLANKS") || blockName.contains("WOOD")
                || blockName.contains("FENCE") || blockName.contains("DOOR")) {
            return toolName.contains("AXE");
        }
        if (blockName.equals("DIRT") || blockName.contains("SAND") || blockName.equals("GRAVEL")
                || blockName.equals("GRASS_BLOCK") || blockName.contains("SNOW")) {
            return toolName.contains("SHOVEL") || toolName.contains("SPADE");
        }
        if (blockName.equals("COBWEB") || blockName.equals("WEB")) {
            return toolName.contains("SWORD");
        }
        return false;
    }

    @Override
    protected double getViolationAmount() {
        return 1.0;
    }
}
