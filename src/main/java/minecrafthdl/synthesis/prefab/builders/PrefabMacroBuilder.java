package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public interface PrefabMacroBuilder {
    Gate build(PrefabMacroGateFactory.Request request);
}
