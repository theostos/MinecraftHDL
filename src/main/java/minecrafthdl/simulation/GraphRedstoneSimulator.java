package minecrafthdl.simulation;

import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.In_output;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.MuxVertex;
import MinecraftGraph.Vertex;
import MinecraftGraph.VertexType;
import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.macro.MacroInstanceGroups;
import minecrafthdl.synthesis.macro.MacroRuntimeModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Standalone simulator for a GraphBuilder graph.
 * This allows fast logic/macro validation without launching Minecraft.
 */
public final class GraphRedstoneSimulator {

    private final ArrayList<Vertex> evalOrder;
    private final LinkedHashMap<String, In_output> inputsByName = new LinkedHashMap<String, In_output>();
    private final LinkedHashMap<String, In_output> outputsByName = new LinkedHashMap<String, In_output>();

    private final LinkedHashMap<String, LinkedHashMap<String, Long>> macroParamsByInstance = new LinkedHashMap<String, LinkedHashMap<String, Long>>();
    private final LinkedHashMap<String, MacroRuntimeModel.State> macroStateByInstance = new LinkedHashMap<String, MacroRuntimeModel.State>();
    private final IdentityHashMap<MacroVertex, String> macroInstanceByVertex = new IdentityHashMap<MacroVertex, String>();

    private final IdentityHashMap<Vertex, Boolean> lastVertexValues = new IdentityHashMap<Vertex, Boolean>();
    private final LinkedHashMap<String, Boolean> lastOutputs = new LinkedHashMap<String, Boolean>();

    public GraphRedstoneSimulator(Graph graph) {
        this(graph, false);
    }

    public GraphRedstoneSimulator(Graph graph, boolean injectAutoClock) {
        if (graph == null) {
            throw new MHDLException("Simulator requires non-null graph");
        }

        this.evalOrder = topologicalOrder(graph);
        indexIoVertices();
        indexMacros(graph, injectAutoClock);
        reset();
    }

    public Set<String> getInputNames() {
        return Collections.unmodifiableSet(this.inputsByName.keySet());
    }

    public Set<String> getOutputNames() {
        return Collections.unmodifiableSet(this.outputsByName.keySet());
    }

    public Map<String, Boolean> tick(Map<String, Boolean> inputs) {
        this.lastVertexValues.clear();
        HashSet<String> steppedInstances = new HashSet<String>();

        for (Vertex vertex : this.evalOrder) {
            boolean value;
            if (vertex.getType() == VertexType.INPUT) {
                value = readInput(inputs, vertex.getID());
            } else if (vertex.getType() == VertexType.OUTPUT) {
                value = readAnyBefore(vertex);
            } else if (vertex.getType() == VertexType.FUNCTION) {
                value = evalFunction((Function) vertex, steppedInstances);
            } else {
                value = false;
            }
            this.lastVertexValues.put(vertex, value);
        }

        for (Map.Entry<String, In_output> entry : this.outputsByName.entrySet()) {
            Boolean value = this.lastVertexValues.get(entry.getValue());
            this.lastOutputs.put(entry.getKey(), value != null && value);
        }

        return new LinkedHashMap<String, Boolean>(this.lastOutputs);
    }

    public boolean getOutput(String outputName) {
        Boolean value = this.lastOutputs.get(outputName);
        return value != null && value;
    }

    public Map<String, Boolean> getOutputs() {
        return Collections.unmodifiableMap(this.lastOutputs);
    }

    public void reset() {
        for (MacroRuntimeModel.State state : this.macroStateByInstance.values()) {
            state.resetAll();
        }
        this.lastVertexValues.clear();
        this.lastOutputs.clear();
        for (String output : this.outputsByName.keySet()) {
            this.lastOutputs.put(output, false);
        }
    }

    private void indexIoVertices() {
        for (Vertex vertex : this.evalOrder) {
            if (!(vertex instanceof In_output)) {
                continue;
            }
            In_output io = (In_output) vertex;
            if (io.getType() == VertexType.INPUT) {
                this.inputsByName.putIfAbsent(io.getID(), io);
            } else if (io.getType() == VertexType.OUTPUT) {
                this.outputsByName.putIfAbsent(io.getID(), io);
            }
        }
    }

    private void indexMacros(Graph graph, boolean injectAutoClock) {
        LinkedHashMap<String, MacroInstanceGroups.Group> groups = MacroInstanceGroups.fromVertices(graph.getVertices());
        for (Map.Entry<String, MacroInstanceGroups.Group> entry : groups.entrySet()) {
            String instanceName = entry.getKey();
            MacroInstanceGroups.Group group = entry.getValue();

            LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>(group.getParams());
            if (injectAutoClock && !params.containsKey("AUTO_CLK")) {
                params.put("AUTO_CLK", 1L);
            }

            this.macroParamsByInstance.put(instanceName, params);
            this.macroStateByInstance.put(instanceName, new MacroRuntimeModel.State());

            for (MacroVertex member : group.getMembers()) {
                this.macroInstanceByVertex.put(member, instanceName);
            }
        }
    }

    private boolean evalFunction(Function function, HashSet<String> steppedInstances) {
        FunctionType type = function.getFunc_Type();
        if (type == FunctionType.AND) {
            return evalAnd(function);
        }
        if (type == FunctionType.OR) {
            return evalOr(function);
        }
        if (type == FunctionType.XOR) {
            return evalXor(function);
        }
        if (type == FunctionType.INV) {
            return !readFirstBefore(function);
        }
        if (type == FunctionType.RELAY) {
            return readFirstBefore(function);
        }
        if (type == FunctionType.MUX) {
            return evalMux(function);
        }
        if (type == FunctionType.HIGH) {
            return true;
        }
        if (type == FunctionType.LOW) {
            return false;
        }
        if (type == FunctionType.MC_TIMER
                || type == FunctionType.MC_PERIODIC
                || type == FunctionType.MC_LATCH
                || type == FunctionType.MC_COUNTER
                || type == FunctionType.MC_SEQ_LOCK
                || type == FunctionType.MC_STATION_FSM) {
            return evalMacro(function, steppedInstances);
        }

        throw new MHDLException("Unsupported function type in simulator: " + type);
    }

    private boolean evalAnd(Function function) {
        if (function.getBefore().isEmpty()) {
            return false;
        }
        for (Vertex before : function.getBefore()) {
            if (!valueOf(before)) {
                return false;
            }
        }
        return true;
    }

    private boolean evalOr(Function function) {
        for (Vertex before : function.getBefore()) {
            if (valueOf(before)) {
                return true;
            }
        }
        return false;
    }

    private boolean evalXor(Function function) {
        boolean value = false;
        for (Vertex before : function.getBefore()) {
            value ^= valueOf(before);
        }
        return value;
    }

    private boolean evalMux(Function function) {
        if (function instanceof MuxVertex) {
            MuxVertex mux = (MuxVertex) function;
            boolean select = valueOf(mux.s_vertex);
            return select ? valueOf(mux.b_vertex) : valueOf(mux.a_vertex);
        }
        if (function.getBefore().size() < 3) {
            return false;
        }
        boolean a = valueOf(function.getBefore().get(0));
        boolean b = valueOf(function.getBefore().get(1));
        boolean select = valueOf(function.getBefore().get(2));
        return select ? b : a;
    }

    private boolean evalMacro(Function function, HashSet<String> steppedInstances) {
        if (!(function instanceof MacroVertex)) {
            throw new MHDLException("Macro function without MacroVertex metadata: " + function.getID());
        }
        MacroVertex macroVertex = (MacroVertex) function;

        String instanceName = this.macroInstanceByVertex.get(macroVertex);
        if (instanceName == null) {
            throw new MHDLException("Macro instance not indexed for vertex: " + macroVertex.getID());
        }

        MacroRuntimeModel.State state = this.macroStateByInstance.get(instanceName);
        if (state == null) {
            throw new MHDLException("Macro state missing for instance: " + instanceName);
        }

        if (!steppedInstances.contains(instanceName)) {
            boolean[] macroInputs = resolveMacroInputs(macroVertex);
            Map<String, Long> params = this.macroParamsByInstance.get(instanceName);
            MacroRuntimeModel.step(macroVertex.getMacroName(), params, macroInputs, state);
            steppedInstances.add(instanceName);
        }

        return MacroRuntimeModel.readOutput(
                macroVertex.getMacroName(),
                macroVertex.getOutputPort(),
                macroVertex.getOutputBitIndex(),
                state
        );
    }

    private boolean[] resolveMacroInputs(MacroVertex macroVertex) {
        boolean[] inputs = new boolean[macroVertex.getOrderedInputNets().size()];
        for (Map.Entry<Vertex, ArrayList<Integer>> entry : macroVertex.getSourceToPinIndices().entrySet()) {
            boolean sourceValue = valueOf(entry.getKey());
            for (Integer pinIndex : entry.getValue()) {
                if (pinIndex == null || pinIndex < 0 || pinIndex >= inputs.length) {
                    continue;
                }
                inputs[pinIndex] = sourceValue;
            }
        }
        return inputs;
    }

    private boolean readInput(Map<String, Boolean> inputs, String name) {
        if (inputs == null) {
            return false;
        }
        Boolean value = inputs.get(name);
        return value != null && value;
    }

    private boolean readFirstBefore(Vertex vertex) {
        if (vertex == null || vertex.getBefore().isEmpty()) {
            return false;
        }
        return valueOf(vertex.getBefore().get(0));
    }

    private boolean readAnyBefore(Vertex vertex) {
        if (vertex == null) {
            return false;
        }
        for (Vertex before : vertex.getBefore()) {
            if (valueOf(before)) {
                return true;
            }
        }
        return false;
    }

    private boolean valueOf(Vertex vertex) {
        if (vertex == null) {
            return false;
        }
        Boolean value = this.lastVertexValues.get(vertex);
        return value != null && value;
    }

    private static ArrayList<Vertex> topologicalOrder(Graph graph) {
        ArrayList<Vertex> vertices = graph.getVertices();
        IdentityHashMap<Vertex, Integer> indegree = new IdentityHashMap<Vertex, Integer>();
        for (Vertex vertex : vertices) {
            indegree.put(vertex, vertex.getBefore().size());
        }

        ArrayDeque<Vertex> queue = new ArrayDeque<Vertex>();
        for (Vertex vertex : vertices) {
            Integer value = indegree.get(vertex);
            if (value != null && value == 0) {
                queue.addLast(vertex);
            }
        }

        ArrayList<Vertex> ordered = new ArrayList<Vertex>(vertices.size());
        while (!queue.isEmpty()) {
            Vertex vertex = queue.removeFirst();
            ordered.add(vertex);

            for (Vertex next : vertex.getNext()) {
                Integer current = indegree.get(next);
                if (current == null) {
                    continue;
                }
                int updated = current - 1;
                indegree.put(next, updated);
                if (updated == 0) {
                    queue.addLast(next);
                }
            }
        }

        if (ordered.size() != vertices.size()) {
            throw new MHDLException("Graph is not acyclic; simulator requires DAG-style graph");
        }
        return ordered;
    }
}
