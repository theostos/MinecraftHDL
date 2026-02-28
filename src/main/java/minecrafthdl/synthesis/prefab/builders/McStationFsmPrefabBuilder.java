package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McStationFsmPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int departTicks = intParam(request, "DEPART_TICKS", 20);
        requireRange("mc_station_fsm", "DEPART_TICKS", departTicks, 1, 1200);
        return null;
    }
}
