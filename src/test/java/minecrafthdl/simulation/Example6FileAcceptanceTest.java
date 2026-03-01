package minecrafthdl.simulation;

import GraphBuilder.GraphBuilder;
import MinecraftGraph.Graph;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Example6FileAcceptanceTest {

    @Test
    void sequenceLockWrongThenCorrectFromRealNetlist() throws Exception {
        Path netlist = Paths.get("verilog/linux/example6_wrong_code_triggers_trap.v.json");
        if (!Files.exists(netlist)) {
            throw new IllegalStateException("Missing expected test netlist: " + netlist.toAbsolutePath());
        }

        Graph graph = GraphBuilder.buildGraph(netlist.toString());
        GraphRedstoneSimulator sim = new GraphRedstoneSimulator(graph);

        tick(sim, false, false, false, false, false);

        Map<String, Boolean> wrong = pulse(sim, false, false, true, false);
        assertTrue(wrong.get("trap"));
        assertFalse(wrong.get("reward"));

        pulse(sim, true, false, false, false);  // A
        pulse(sim, false, true, false, false);  // B
        Map<String, Boolean> correct = pulse(sim, false, false, true, false); // C
        assertTrue(correct.get("reward"));
        assertFalse(correct.get("trap"));

        Map<String, Boolean> held = tick(sim, true, false, false, false, false);
        assertTrue(held.get("reward"));
        assertFalse(held.get("trap"));

        pulse(sim, false, false, false, true); // rst
        Map<String, Boolean> afterReset = tick(sim, false, false, false, false, false);
        assertFalse(afterReset.get("reward"));
        assertFalse(afterReset.get("trap"));
    }

    private static Map<String, Boolean> pulse(GraphRedstoneSimulator sim, boolean a, boolean b, boolean c, boolean rst) {
        tick(sim, false, false, false, false, rst);
        Map<String, Boolean> onRise = tick(sim, true, a, b, c, rst);
        tick(sim, false, false, false, false, false);
        return onRise;
    }

    private static Map<String, Boolean> tick(GraphRedstoneSimulator sim, boolean clk, boolean a, boolean b, boolean c, boolean rst) {
        return sim.tick(mapOf(
                "clk", clk,
                "rst", rst,
                "a_pressed", a,
                "b_pressed", b,
                "c_pressed", c
        ));
    }

    private static Map<String, Boolean> mapOf(Object... pairs) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Boolean) pairs[i + 1]);
        }
        return map;
    }
}
