package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McPeriodicPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int period = intParam(request, "PERIOD", 20);
        requireRange("mc_periodic", "PERIOD", period, 1, 1200);
        return null;
    }
}
