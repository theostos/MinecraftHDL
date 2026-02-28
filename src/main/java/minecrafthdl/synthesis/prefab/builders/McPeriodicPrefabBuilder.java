package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McPeriodicPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int period = intParam(request, "PERIOD", 20);
        requireRange("mc_periodic", "PERIOD", period, 1, 1200);
        Gate gate = oneOutputShell(3, 6);
        routeOutputFromInput(gate, 2); // enable passthrough in prefab phase-A scaffold.
        addLabel(gate, "mc_periodic");
        return gate;
    }
}
