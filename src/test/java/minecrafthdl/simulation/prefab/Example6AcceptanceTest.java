package minecrafthdl.simulation.prefab;

import GraphBuilder.GraphBuilder;
import MinecraftGraph.Graph;
import minecrafthdl.simulation.GraphRedstoneSimulator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Example6AcceptanceTest {

    @Test
    void sequenceLockExampleBehavesAsExpected() {
        Path netlist = Path.of("verilog/linux/example6_wrong_code_triggers_trap.v.json").toAbsolutePath();
        Graph graph = GraphBuilder.buildGraph(netlist.toString());
        GraphRedstoneSimulator sim = new GraphRedstoneSimulator(graph, true);

        // Wrong-first press (B) should trigger trap.
        tick(sim, false, false, false, false, false);
        tick(sim, false, false, false, false, false);
        Map<String, Boolean> wrong = tick(sim, false, false, false, true, false);
        assertTrue(wrong.get("trap"));
        assertFalse(wrong.get("reward"));
        tick(sim, false, false, false, false, false);

        // Reset before testing correct sequence.
        tick(sim, false, true, false, false, false);
        tick(sim, false, false, false, false, false);

        // Correct sequence A -> B -> C should unlock reward.
        tick(sim, false, false, true, false, false);
        tick(sim, false, false, false, false, false);
        tick(sim, false, false, false, true, false);
        tick(sim, false, false, false, false, false);
        Map<String, Boolean> correct = tick(sim, false, false, false, false, true);
        assertTrue(correct.get("reward"));
        assertFalse(correct.get("trap"));
        tick(sim, false, false, false, false, false);

        // Reset should clear reward latch.
        Map<String, Boolean> reset = tick(sim, false, true, false, false, false);
        assertFalse(reset.get("reward"));
    }

    private static Map<String, Boolean> tick(
            GraphRedstoneSimulator sim,
            boolean clk,
            boolean rst,
            boolean a,
            boolean b,
            boolean c
    ) {
        LinkedHashMap<String, Boolean> inputs = new LinkedHashMap<String, Boolean>();
        inputs.put("clk", clk);
        inputs.put("rst", rst);
        inputs.put("a_pressed", a);
        inputs.put("b_pressed", b);
        inputs.put("c_pressed", c);
        return sim.tick(inputs);
    }
}
