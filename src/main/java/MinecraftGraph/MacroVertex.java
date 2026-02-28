package MinecraftGraph;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MacroVertex extends Function {
    private final String instanceName;
    private final String macroName;
    private final String outputPort;
    private final int outputBitIndex;
    private final HashMap<String, Long> params;
    private final ArrayList<Integer> orderedInputNets;
    private final ArrayList<Vertex> orderedInputSources;
    private final LinkedHashMap<Vertex, ArrayList<Integer>> sourceToPinIndices = new LinkedHashMap<Vertex, ArrayList<Integer>>();

    public MacroVertex(int id, FunctionType type, int numInputs, String instanceName, String macroName, String outputPort, int outputBitIndex, Map<String, Long> params, List<Integer> orderedInputNets) {
        super(id, type, numInputs);
        this.instanceName = instanceName;
        this.macroName = macroName;
        this.outputPort = outputPort;
        this.outputBitIndex = outputBitIndex;
        this.params = new HashMap<String, Long>(params);
        this.orderedInputNets = new ArrayList<Integer>(orderedInputNets);
        this.orderedInputSources = new ArrayList<Vertex>(orderedInputNets.size());
        for (int i = 0; i < orderedInputNets.size(); i++) {
            this.orderedInputSources.add(null);
        }
    }

    public String getMacroName() {
        return this.macroName;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public String getOutputPort() {
        return this.outputPort;
    }

    public int getOutputBitIndex() {
        return this.outputBitIndex;
    }

    public Map<String, Long> getParams() {
        return Collections.unmodifiableMap(this.params);
    }

    public List<Integer> getOrderedInputNets() {
        return Collections.unmodifiableList(this.orderedInputNets);
    }

    public void registerInputSource(int inputNet, Vertex source) {
        for (int i = 0; i < this.orderedInputNets.size(); i++) {
            if (!matchesInput(this.orderedInputNets.get(i), inputNet, source)) {
                continue;
            }
            if (this.orderedInputSources.get(i) != null) {
                continue;
            }
            this.orderedInputSources.set(i, source);
            this.sourceToPinIndices.computeIfAbsent(source, unused -> new ArrayList<Integer>()).add(i);
            return;
        }
    }

    private static boolean matchesInput(int expectedNet, int actualNet, Vertex source) {
        if (expectedNet == actualNet) {
            return true;
        }

        if (source != null && source.getType() == VertexType.FUNCTION) {
            Function function = (Function) source;
            if (expectedNet == 0 && function.getFunc_Type() == FunctionType.LOW) {
                return true;
            }
            if (expectedNet == 1 && function.getFunc_Type() == FunctionType.HIGH) {
                return true;
            }
        }

        return false;
    }

    public Map<Vertex, ArrayList<Integer>> getSourceToPinIndices() {
        return Collections.unmodifiableMap(this.sourceToPinIndices);
    }

    @Override
    public void handleRelay(Vertex v, Vertex relay) {
        super.handleRelay(v, relay);
        ArrayList<Integer> indices = this.sourceToPinIndices.remove(v);
        if (indices == null || indices.isEmpty()) {
            return;
        }
        for (Integer idx : indices) {
            if (idx == null || idx < 0 || idx >= this.orderedInputSources.size()) {
                continue;
            }
            this.orderedInputSources.set(idx, relay);
        }
        this.sourceToPinIndices.computeIfAbsent(relay, unused -> new ArrayList<Integer>()).addAll(indices);
    }
}
