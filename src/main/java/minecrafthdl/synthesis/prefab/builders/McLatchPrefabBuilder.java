package minecrafthdl.synthesis.prefab.builders;

import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.In_output;
import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McLatchPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        requireInputCount(request, "mc_latch", 4);
        requireSingleBitOutput(request, "mc_latch", "q");
        Graph graph = new Graph();

        In_output clk = input("i0_clk_compat");
        In_output rst = input("i1_rst");
        In_output set = input("i2_set");
        In_output clear = input("i3_clear");

        Function clearTotal = new Function(0, FunctionType.OR, 2);
        Function notClear = new Function(1, FunctionType.INV, 1);
        Function data = new Function(2, FunctionType.AND, 2);
        Function enable = new Function(3, FunctionType.OR, 2);
        Function latch = new Function(4, FunctionType.D_LATCH, 2);

        In_output q = output("o0_q");

        graph.addVertex(clk); // compatibility input, intentionally unused.
        graph.addVertex(rst);
        graph.addVertex(set);
        graph.addVertex(clear);
        graph.addVertex(clearTotal);
        graph.addVertex(notClear);
        graph.addVertex(data);
        graph.addVertex(enable);
        graph.addVertex(latch);
        graph.addVertex(q);

        wire(graph, rst, clearTotal);
        wire(graph, clear, clearTotal);

        wire(graph, clearTotal, notClear);
        wire(graph, set, data);
        wire(graph, notClear, data);

        wire(graph, set, enable);
        wire(graph, clearTotal, enable);

        wire(graph, data, latch);
        wire(graph, enable, latch);
        wire(graph, latch, q);

        Gate gate = synthesizeSubgraphAsGate(graph, 4);
        addLabel(gate, "mc_latch");
        return gate;
    }
}
