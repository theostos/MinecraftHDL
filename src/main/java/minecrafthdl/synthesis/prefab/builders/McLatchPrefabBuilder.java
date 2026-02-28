package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McLatchPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        requireInputCount(request, "mc_latch", 4);
        requireSingleBitOutput(request, "mc_latch", "q");
        Gate gate = oneOutputShell(4, 6);
        routeOutputFromInput(gate, 2); // set_pulse passthrough in prefab phase-A scaffold.
        addLabel(gate, "mc_latch");
        return gate;
    }
}
