package minecrafthdl.simulation.prefab;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefabMacroModelTest {

    @Test
    void timerHonorsTicksWindow() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("TICKS", 3L);

        // Trigger on first rising edge.
        assertTrue(tick("mc_timer", "active", 0, params, true, false, true, state));

        // Falling edge, output remains active.
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));

        // Next rising edges consume remaining ticks.
        assertTrue(tick("mc_timer", "active", 0, params, true, false, false, state));
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));
        assertTrue(tick("mc_timer", "active", 0, params, true, false, false, state));
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));
        assertFalse(tick("mc_timer", "active", 0, params, true, false, false, state));
    }

    @Test
    void periodicPulsesEveryPeriod() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("PERIOD", 2L);

        // First enabled rising edge: no pulse.
        assertFalse(tick("mc_periodic", "pulse", 0, params, true, false, true, state));
        assertFalse(tick("mc_periodic", "pulse", 0, params, false, false, true, state));

        // Second enabled rising edge: pulse.
        assertTrue(tick("mc_periodic", "pulse", 0, params, true, false, true, state));
    }

    @Test
    void counterIncrementsAndWrapsByWidth() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("WIDTH", 2L);

        // 4 increments in width=2 wraps to 0.
        for (int i = 0; i < 4; i++) {
            tick("mc_counter", "count", 0, params, true, false, true, false, state);
            tick("mc_counter", "count", 0, params, false, false, false, false, state);
        }

        assertFalse(tick("mc_counter", "count", 0, params, false, false, false, false, state));
        assertFalse(tick("mc_counter", "count", 1, params, false, false, false, false, state));
    }

    @Test
    void seqLockUsesExpectIdxAndEmitsCorrectPulse() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("BTN_COUNT", 3L);
        params.put("SEQ_LEN", 3L);
        params.put("LATCH_SUCCESS", 1L);
        // Steps: 0,1,2 with 2-bit packed LSB-first => 0 | (1<<2) | (2<<4)
        params.put("EXPECT_IDX", 36L);

        // Step 0
        tickSeqLockPulse(params, state, true, false, false, true, false, false);
        // Step 1
        tickSeqLockPulse(params, state, true, false, false, false, true, false);
        // Final step 2
        tickSeqLockPulse(params, state, true, false, false, false, false, true);

        assertTrue(PrefabMacroModel.tick("mc_seq_lock", "correct_pulse", 0, params,
                seqInputs(false, false, false, false, false, false), state));
        assertTrue(PrefabMacroModel.tick("mc_seq_lock", "unlocked", 0, params,
                seqInputs(false, false, false, false, false, false), state));
    }

    @Test
    void stationFsmHoldsDepartNowForConfiguredTicks() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = mapOf("DEPART_TICKS", 2L);

        // Arrival => OCCUPIED
        tick("mc_station_fsm", "occupied", 0, params, true, false, false, true, false, state);
        tick("mc_station_fsm", "occupied", 0, params, false, false, false, false, false, state);
        assertTrue(tick("mc_station_fsm", "occupied", 0, params, false, false, false, false, false, state));

        // Depart => DEPARTING
        assertTrue(tick("mc_station_fsm", "depart_now", 0, params, true, false, false, false, true, state));
        tick("mc_station_fsm", "depart_now", 0, params, false, false, false, false, false, state);

        // Hold one more cycle (2 ticks total), then clear.
        assertTrue(tick("mc_station_fsm", "depart_now", 0, params, true, false, false, false, false, state));
        tick("mc_station_fsm", "depart_now", 0, params, false, false, false, false, false, state);
        assertFalse(tick("mc_station_fsm", "depart_now", 0, params, true, false, false, false, false, state));
    }

    @Test
    void timerRunsWithAutoClockEvenWhenClkInputStaysLow() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("TICKS", 2L);
        params.put("AUTO_CLK", 1L);

        // clk input (i0) stays low throughout; AUTO_CLK drives macro updates.
        assertTrue(tick("mc_timer", "active", 0, params, false, false, true, state));
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));
        assertFalse(tick("mc_timer", "active", 0, params, false, false, false, state));
    }

    @Test
    void seqLockTreatsHeldButtonAsSinglePulse() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("BTN_COUNT", 3L);
        params.put("SEQ_LEN", 3L);
        params.put("LATCH_SUCCESS", 1L);
        params.put("EXPECT_IDX", 36L);
        params.put("AUTO_CLK", 1L);

        // Hold button A across two rising edges; should count only once.
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, true, false, false, state);  // rising
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, true, false, false, state);  // falling
        assertFalse(tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, true, false, false, state)); // rising, no second pulse

        // Release and press B.
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, false, false, false, state); // falling
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, false, true, false, state);  // rising
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, false, true, false, state);  // falling

        // Release then press C -> success pulse.
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, false, false, false, state); // rising
        tick("mc_seq_lock", "wrong_pulse", 0, params, false, false, false, false, false, false, state); // falling
        assertTrue(tick("mc_seq_lock", "correct_pulse", 0, params, false, false, false, false, false, true, state)); // rising
    }

    @Test
    void autoClockPeriodTicksControlsRisingEdgeCadence() {
        PrefabMacroModel.State state = new PrefabMacroModel.State();
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("TICKS", 1L);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", 4L);

        // Trigger timer once at tick 0 (rising), then it should stay active until next rising (tick 4).
        assertTrue(tick("mc_timer", "active", 0, params, false, false, true, state));   // t0 rising + trigger
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));  // t1
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));  // t2
        assertTrue(tick("mc_timer", "active", 0, params, false, false, false, state));  // t3
        assertFalse(tick("mc_timer", "active", 0, params, false, false, false, state)); // t4 rising decrements to 0
    }

    private static boolean tick(String macro, String outPort, int outBit, Map<String, Long> params,
                                boolean i0, boolean i1, boolean i2, PrefabMacroModel.State state) {
        return PrefabMacroModel.tick(macro, outPort, outBit, params, new boolean[]{i0, i1, i2}, state);
    }

    private static boolean tick(String macro, String outPort, int outBit, Map<String, Long> params,
                                boolean i0, boolean i1, boolean i2, boolean i3, PrefabMacroModel.State state) {
        return PrefabMacroModel.tick(macro, outPort, outBit, params, new boolean[]{i0, i1, i2, i3}, state);
    }

    private static boolean tick(String macro, String outPort, int outBit, Map<String, Long> params,
                                boolean i0, boolean i1, boolean i2, boolean i3, boolean i4, PrefabMacroModel.State state) {
        return PrefabMacroModel.tick(macro, outPort, outBit, params, new boolean[]{i0, i1, i2, i3, i4}, state);
    }

    private static boolean tick(String macro, String outPort, int outBit, Map<String, Long> params,
                                boolean i0, boolean i1, boolean i2, boolean i3, boolean i4, boolean i5, PrefabMacroModel.State state) {
        return PrefabMacroModel.tick(macro, outPort, outBit, params, new boolean[]{i0, i1, i2, i3, i4, i5}, state);
    }

    private static void tickSeqLockPulse(Map<String, Long> params, PrefabMacroModel.State state,
                                         boolean clkRise,
                                         boolean rst,
                                         boolean clear,
                                         boolean btn0,
                                         boolean btn1,
                                         boolean btn2) {
        PrefabMacroModel.tick("mc_seq_lock", "progress", 0, params, seqInputs(clkRise, rst, clear, btn0, btn1, btn2), state);
        PrefabMacroModel.tick("mc_seq_lock", "progress", 0, params, seqInputs(false, rst, clear, false, false, false), state);
    }

    private static boolean[] seqInputs(boolean clk, boolean rst, boolean clear, boolean btn0, boolean btn1, boolean btn2) {
        return new boolean[]{clk, rst, clear, btn0, btn1, btn2};
    }

    private static Map<String, Long> mapOf(String key, Long value) {
        HashMap<String, Long> map = new HashMap<String, Long>();
        map.put(key, value);
        return map;
    }
}
