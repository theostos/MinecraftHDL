package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McTimerPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int ticks = intParam(request, "TICKS", 60);
        requireRange("mc_timer", "TICKS", ticks, 1, 1200);
        requireInputCount(request, "mc_timer", 3);
        requireSingleBitOutput(request, "mc_timer", "active");
        Gate gate = oneOutputShell(3, 6);
        routeOutputFromInput(gate, 2); // trigger_pulse passthrough in prefab phase-A scaffold.
        addLabel(gate, "mc_timer");
        return gate;
    }
}
