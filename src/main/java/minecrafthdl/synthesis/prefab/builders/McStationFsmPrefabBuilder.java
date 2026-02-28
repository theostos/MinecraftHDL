package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McStationFsmPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int departTicks = intParam(request, "DEPART_TICKS", 20);
        requireRange("mc_station_fsm", "DEPART_TICKS", departTicks, 1, 1200);
        Gate gate = oneOutputShell(5, 7);
        if ("depart_now".equals(request.outputPort)) {
            routeOutputFromInput(gate, 4); // depart_pulse
        } else {
            routeOutputFromInput(gate, 3); // arrival_pulse
        }
        addLabel(gate, "mc_station_fsm." + request.outputPort);
        return gate;
    }
}
