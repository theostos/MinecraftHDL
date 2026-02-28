package MinecraftGraph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MacroVertex extends Function {
    private final String macroName;
    private final String outputPort;
    private final int outputBitIndex;
    private final HashMap<String, Integer> params;

    public MacroVertex(int id, FunctionType type, int numInputs, String macroName, String outputPort, int outputBitIndex, Map<String, Integer> params) {
        super(id, type, numInputs);
        this.macroName = macroName;
        this.outputPort = outputPort;
        this.outputBitIndex = outputBitIndex;
        this.params = new HashMap<String, Integer>(params);
    }

    public String getMacroName() {
        return this.macroName;
    }

    public String getOutputPort() {
        return this.outputPort;
    }

    public int getOutputBitIndex() {
        return this.outputBitIndex;
    }

    public Map<String, Integer> getParams() {
        return Collections.unmodifiableMap(this.params);
    }
}
