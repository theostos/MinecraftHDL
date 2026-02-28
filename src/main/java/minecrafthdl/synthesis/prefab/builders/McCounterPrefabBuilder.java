package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McCounterPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int width = intParam(request, "WIDTH", 8);
        requireRange("mc_counter", "WIDTH", width, 1, 16);
        return null;
    }
}
