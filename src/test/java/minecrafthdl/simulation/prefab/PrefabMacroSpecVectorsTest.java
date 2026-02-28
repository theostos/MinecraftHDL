package minecrafthdl.simulation.prefab;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefabMacroSpecVectorsTest {

    @Test
    void timerVectorMatchesSpec() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("TICKS", 3L);

        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, true), state));
        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
        assertTrue(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
        assertFalse(sim.tickOutput("mc_timer", "active", 0, params, in3(false, false, false), state));
    }

    @Test
    void periodicVectorMatchesSpecWithPulseWidthOneAutoPeriod() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("PERIOD", 2L);

        assertFalse(sim.tickOutput("mc_periodic", "pulse", 0, params, in3(false, false, true), state));
        assertFalse(sim.tickOutput("mc_periodic", "pulse", 0, params, in3(false, false, true), state));
        assertTrue(sim.tickOutput("mc_periodic", "pulse", 0, params, in3(false, false, true), state));
        assertTrue(sim.tickOutput("mc_periodic", "pulse", 0, params, in3(false, false, true), state));
        assertFalse(sim.tickOutput("mc_periodic", "pulse", 0, params, in3(false, false, true), state));
    }

    @Test
    void latchVectorMatchesSetClearResetPriority() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();

        assertFalse(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, false), state));
        assertFalse(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, false), state));
        assertTrue(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, true, false), state));
        assertTrue(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, false), state));
        assertTrue(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, false), state));
        assertTrue(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, false), state));
        assertFalse(sim.tickOutput("mc_latch", "q", 0, params, in4(false, false, false, true), state));
        assertFalse(sim.tickOutput("mc_latch", "q", 0, params, in4(false, true, true, false), state));
    }

    @Test
    void counterVectorMatchesModuloAndClear() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("WIDTH", 2L);

        sim.tickOutputs("mc_counter", params, in4(false, false, true, false), state,
                out("c0", "count", 0), out("c1", "count", 1));
        sim.tickOutputs("mc_counter", params, in4(false, false, false, false), state,
                out("c0", "count", 0), out("c1", "count", 1));
        LinkedHashMap<String, Boolean> v2 = sim.tickOutputs("mc_counter", params, in4(false, false, true, false), state,
                out("c0", "count", 0), out("c1", "count", 1));
        assertFalse(v2.get("c0"));
        assertTrue(v2.get("c1"));

        sim.tickOutputs("mc_counter", params, in4(false, false, false, false), state,
                out("c0", "count", 0), out("c1", "count", 1));
        LinkedHashMap<String, Boolean> cleared = sim.tickOutputs("mc_counter", params, in4(false, false, false, true), state,
                out("c0", "count", 0), out("c1", "count", 1));
        assertFalse(cleared.get("c0"));
        assertFalse(cleared.get("c1"));
    }

    @Test
    void seqLockVectorMatchesCorrectWrongAndProgress() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("BTN_COUNT", 3L);
        params.put("SEQ_LEN", 3L);
        params.put("LATCH_SUCCESS", 1L);
        params.put("EXPECT_IDX", 36L);

        LinkedHashMap<String, Boolean> a = sim.tickOutputs("mc_seq_lock", params, in6(false, false, false, true, false, false), state,
                out("u", "unlocked", 0), out("ok", "correct_pulse", 0), out("bad", "wrong_pulse", 0), out("p0", "progress", 0), out("p1", "progress", 1));
        assertFalse(a.get("ok"));
        assertFalse(a.get("bad"));
        assertTrue(a.get("p0"));
        assertFalse(a.get("p1"));

        sim.tickOutputs("mc_seq_lock", params, in6(false, false, false, false, false, false), state,
                out("u", "unlocked", 0), out("ok", "correct_pulse", 0), out("bad", "wrong_pulse", 0));
        LinkedHashMap<String, Boolean> b = sim.tickOutputs("mc_seq_lock", params, in6(false, false, false, false, true, false), state,
                out("u", "unlocked", 0), out("ok", "correct_pulse", 0), out("bad", "wrong_pulse", 0), out("p0", "progress", 0), out("p1", "progress", 1));
        assertFalse(b.get("ok"));
        assertFalse(b.get("bad"));
        assertFalse(b.get("p0"));
        assertTrue(b.get("p1"));

        sim.tickOutputs("mc_seq_lock", params, in6(false, false, false, false, false, false), state,
                out("u", "unlocked", 0), out("ok", "correct_pulse", 0), out("bad", "wrong_pulse", 0));
        LinkedHashMap<String, Boolean> c = sim.tickOutputs("mc_seq_lock", params, in6(false, false, false, false, false, true), state,
                out("u", "unlocked", 0), out("ok", "correct_pulse", 0), out("bad", "wrong_pulse", 0));
        assertTrue(c.get("ok"));
        assertFalse(c.get("bad"));
        assertTrue(c.get("u"));
    }

    @Test
    void stationVectorMatchesDepartWindow() {
        PrefabMacroSimulator sim = new PrefabMacroSimulator(2);
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("DEPART_TICKS", 2L);

        LinkedHashMap<String, Boolean> arrival = sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, true, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        assertTrue(arrival.get("occ"));
        assertFalse(arrival.get("dep"));

        sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        LinkedHashMap<String, Boolean> depart = sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, true), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        assertTrue(depart.get("occ"));
        assertTrue(depart.get("dep"));

        sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        LinkedHashMap<String, Boolean> done = sim.tickOutputs("mc_station_fsm", params, in5(false, false, false, false, false), state,
                out("occ", "occupied", 0), out("dep", "depart_now", 0));
        assertFalse(done.get("dep"));
    }

    private static PrefabMacroSimulator.OutputRef out(String label, String port, int bit) {
        return PrefabMacroSimulator.OutputRef.of(label, port, bit);
    }

    private static boolean[] in3(boolean clk, boolean rst, boolean i2) {
        return new boolean[]{clk, rst, i2};
    }

    private static boolean[] in4(boolean clk, boolean rst, boolean i2, boolean i3) {
        return new boolean[]{clk, rst, i2, i3};
    }

    private static boolean[] in5(boolean clk, boolean rst, boolean clear, boolean i3, boolean i4) {
        return new boolean[]{clk, rst, clear, i3, i4};
    }

    private static boolean[] in6(boolean clk, boolean rst, boolean clear, boolean b0, boolean b1, boolean b2) {
        return new boolean[]{clk, rst, clear, b0, b1, b2};
    }

    private static Map<String, Long> mapOf(String key, Long value) {
        HashMap<String, Long> map = new HashMap<String, Long>();
        map.put(key, value);
        return map;
    }
}
