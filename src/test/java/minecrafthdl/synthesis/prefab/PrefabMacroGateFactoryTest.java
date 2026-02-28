package minecrafthdl.synthesis.prefab;

import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Gate;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void timerRejectsWrongOutputPortOrBit() {
        LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("TICKS", 10L);

        PrefabMacroGateFactory.Request wrongPort = new PrefabMacroGateFactory.Request(
                "u0", "mc_timer", 3, params, "pulse", 0
        );
        PrefabMacroGateFactory.Request wrongBit = new PrefabMacroGateFactory.Request(
                "u0", "mc_timer", 3, params, "active", 1
        );

        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(wrongPort));
        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(wrongBit));
    }

    @Test
    void counterRejectsOutOfRangeBitIndex() {
        LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("WIDTH", 2L);

        PrefabMacroGateFactory.Request invalid = new PrefabMacroGateFactory.Request(
                "u0", "mc_counter", 4, params, "count", 2
        );
        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(invalid));
    }

    @Test
    void seqLockRejectsUnsupportedOutputPort() {
        LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("BTN_COUNT", 3L);
        params.put("SEQ_LEN", 3L);
        params.put("LATCH_SUCCESS", 1L);
        params.put("EXPECT_IDX", 36L);

        PrefabMacroGateFactory.Request invalid = new PrefabMacroGateFactory.Request(
                "u0", "mc_seq_lock", 6, params, "unknown", 0
        );
        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(invalid));
    }

    @Test
    void stationRejectsWrongInputCount() {
        LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("DEPART_TICKS", 10L);

        PrefabMacroGateFactory.Request invalid = new PrefabMacroGateFactory.Request(
                "u0", "mc_station_fsm", 4, params, "occupied", 0
        );
        assertThrows(MHDLException.class, () -> PrefabMacroGateFactory.tryBuild(invalid));
    }

    @Test
    void latchBuilderGeneratesCompositeGateNotSimpleShell() {
        bootstrapMinecraft();
        PrefabMacroGateFactory.Request request = new PrefabMacroGateFactory.Request(
                "u0", "mc_latch", 4, new LinkedHashMap<String, Long>(), "q", 0
        );

        Gate gate = PrefabMacroGateFactory.tryBuild(request);
        assertTrue(gate != null);
        assertTrue(gate.getSizeZ() > 6);
        assertTrue(gate.getSizeX() > 7);
    }

    private static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
