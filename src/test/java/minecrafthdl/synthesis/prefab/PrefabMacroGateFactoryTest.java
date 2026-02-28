package minecrafthdl.synthesis.prefab;

import minecrafthdl.MHDLException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrefabMacroGateFactoryTest {

    @Test
    void unknownMacroReturnsNull() {
        PrefabMacroGateFactory.Request request = new PrefabMacroGateFactory.Request(
                "u0",
                "mc_unknown",
                1,
                new LinkedHashMap<String, Long>(),
                "y",
                0
        );
        assertNull(PrefabMacroGateFactory.tryBuild(request));
    }

    @Test
    void builderValidatesTimerParameterRange() {
        LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("TICKS", 1201L);
        PrefabMacroGateFactory.Request request = new PrefabMacroGateFactory.Request(
                "u0",
                "mc_timer",
                3,
                params,
                "active",
                0
        );
        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(request));
    }
}
