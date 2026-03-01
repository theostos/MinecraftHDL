package minecrafthdl.synthesis.routing.pins;

import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.Vertex;
import MinecraftGraph.VertexType;
import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Gate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MacroPins extends GatePins {

    private final HashMap<Vertex, ArrayDeque<Integer>> sourceToPinIndices = new HashMap<Vertex, ArrayDeque<Integer>>();

    public MacroPins(Gate gate, Vertex vertex, int offset, boolean top) {
        super(gate, vertex, offset, top);
        if (top) {
            return;
        }
        if (vertex.type != VertexType.FUNCTION || ((Function) vertex).func_type == null || !(((Function) vertex) instanceof MacroVertex)) {
            throw new MHDLException("CANT MAKE MACRO PINS WITH NON_MACRO VERTEX");
        }
        Function function = (Function) vertex;
        if (function.func_type != FunctionType.MC_TIMER
                && function.func_type != FunctionType.MC_PERIODIC
                && function.func_type != FunctionType.MC_LATCH
                && function.func_type != FunctionType.MC_COUNTER
                && function.func_type != FunctionType.MC_SEQ_LOCK
                && function.func_type != FunctionType.MC_STATION_FSM) {
            throw new MHDLException("CANT MAKE MACRO PINS WITH NON_MC_* FUNCTION");
        }

        MacroVertex macro = (MacroVertex) function;
        for (Map.Entry<Vertex, ArrayList<Integer>> entry : macro.getSourceToPinIndices().entrySet()) {
            ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
            ArrayList<Integer> sorted = new ArrayList<Integer>(entry.getValue());
            Collections.sort(sorted);
            for (Integer index : sorted) {
                if (index == null) {
                    continue;
                }
                queue.addLast(index);
            }
            this.sourceToPinIndices.put(entry.getKey(), queue);
        }
    }

    @Override
    public Pin getNextPin(Vertex sourceVertex) {
        ArrayDeque<Integer> queue = this.sourceToPinIndices.get(sourceVertex);
        if (queue == null || queue.isEmpty()) {
            throw new MHDLException("Macro input pin requested by unexpected source vertex " + sourceVertex.getID());
        }

        Integer pinIndex = queue.removeFirst();
        if (pinIndex == null || pinIndex < 0 || pinIndex >= this.pins.size()) {
            throw new MHDLException("Macro pin index out of range: " + pinIndex);
        }

        this.next_free_input_pin += 1;
        return this.pins.get(pinIndex);
    }
}
