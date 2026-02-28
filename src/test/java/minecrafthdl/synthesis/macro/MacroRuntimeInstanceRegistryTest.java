package minecrafthdl.synthesis.macro;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MacroRuntimeInstanceRegistryTest {

    @Test
    void sharedInstanceStepsAtMostOncePerGameTick() {
        MacroRuntimeInstanceRegistry registry = new MacroRuntimeInstanceRegistry();
        MacroRuntimeInstanceRegistry.SharedState shared = registry.getOrCreate("origin|u0");

        Map<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("PERIOD", 2L);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", 2L);

        MacroRuntimeModel.State seed = new MacroRuntimeModel.State();
        boolean[] inputs = new boolean[]{false, false, true}; // clk/rst/enable (clk ignored with AUTO_CLK=1)

        for (int gameTick = 0; gameTick < 8; gameTick++) {
            boolean first = MacroRuntimeInstanceRegistry.evaluate(shared, gameTick, "mc_periodic", "pulse", 0, params, inputs, seed);
            boolean second = MacroRuntimeInstanceRegistry.evaluate(shared, gameTick, "mc_periodic", "pulse", 0, params, inputs, seed);

            assertEquals(first, second);
            assertEquals(gameTick + 1, shared.state.autoClockTickCounter);
        }
    }

    @Test
    void evaluateSeedsSharedStateFromBlockEntitySnapshot() {
        MacroRuntimeInstanceRegistry registry = new MacroRuntimeInstanceRegistry();
        MacroRuntimeInstanceRegistry.SharedState shared = registry.getOrCreate("origin|u1");

        MacroRuntimeModel.State seed = new MacroRuntimeModel.State();
        seed.counterValue = 6;
        seed.autoClockTickCounter = 12;

        Map<String, Long> params = new LinkedHashMap<String, Long>();
        params.put("WIDTH", 4L);
        params.put("AUTO_CLK", 0L);

        boolean out = MacroRuntimeInstanceRegistry.evaluate(
                shared,
                100,
                "mc_counter",
                "count",
                0,
                params,
                new boolean[]{false, false, false, false},
                seed
        );

        assertFalse(out);
        assertEquals(6, shared.state.counterValue);
        assertEquals(12, shared.state.autoClockTickCounter);
    }
}
