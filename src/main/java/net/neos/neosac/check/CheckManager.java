package net.neos.neosac.check;

import net.neos.neosac.NeosAC;
import net.neos.neosac.checks.combat.AttackReach;
import net.neos.neosac.checks.combat.Criticals;
import net.neos.neosac.checks.combat.KillauraRotation;
import net.neos.neosac.checks.interaction.RaytraceInteractCheck;
import net.neos.neosac.checks.interaction.ReachCheck;
import net.neos.neosac.checks.packet.BadPacketsA;
import net.neos.neosac.checks.packet.PacketOrderCheck;
import net.neos.neosac.checks.packet.TimerCheck;
import net.neos.neosac.checks.simulation.NoFallCheck;
import net.neos.neosac.checks.simulation.SimulationCheck;
import net.neos.neosac.checks.simulation.VehicleCheck;
import net.neos.neosac.checks.world.FastBreakCheck;
import net.neos.neosac.checks.world.FastPlaceCheck;
import net.neos.neosac.checks.world.NukerCheck;
import net.neos.neosac.checks.world.WrongBlockCheck;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CheckManager {

    private final NeosAC plugin;
    private final Map<String, Check> checks = new LinkedHashMap<>();

    public CheckManager(@NotNull NeosAC plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        register(new SimulationCheck(plugin));
        register(new NoFallCheck(plugin));
        register(new VehicleCheck(plugin));

        register(new PacketOrderCheck(plugin));
        register(new TimerCheck(plugin));
        register(new BadPacketsA(plugin));

        register(new RaytraceInteractCheck(plugin));
        register(new ReachCheck(plugin));

        register(new AttackReach(plugin));
        register(new KillauraRotation(plugin));
        register(new Criticals(plugin));

        register(new NukerCheck(plugin));
        register(new FastBreakCheck(plugin));
        register(new FastPlaceCheck(plugin));
        register(new WrongBlockCheck(plugin));
    }

    public void register(@NotNull Check check) {
        checks.put(check.getName().toLowerCase(), check);
    }

    public Check getCheck(@NotNull String name) {
        return checks.get(name.toLowerCase());
    }

    public Collection<Check> getChecks() {
        return checks.values();
    }

    public void reloadAll() {
        checks.values().forEach(Check::reload);
    }
}
