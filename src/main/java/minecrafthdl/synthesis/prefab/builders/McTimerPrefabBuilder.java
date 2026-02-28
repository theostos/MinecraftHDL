package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McTimerPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int ticks = intParam(request, "TICKS", 60);
        requireRange("mc_timer", "TICKS", ticks, 1, 1200);
        return null;
    }
}
