package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McCounterPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int width = intParam(request, "WIDTH", 8);
        requireRange("mc_counter", "WIDTH", width, 1, 16);
        requireInputCount(request, "mc_counter", 4);
        if (!"count".equals(request.outputPort) || request.outputBit < 0 || request.outputBit >= width) {
            throw new MHDLException(
                    "Unsupported output selection for mc_counter: "
                            + request.outputPort
                            + "["
                            + request.outputBit
                            + "]"
            );
        }
        Gate gate = oneOutputShell(4, 6);
        routeOutputFromInput(gate, 2); // inc_pulse passthrough in prefab phase-B scaffold.
        addLabel(gate, "mc_counter[" + request.outputBit + "]");
        return gate;
    }
}
