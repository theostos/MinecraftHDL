package MinecraftGraph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MacroVertex extends Function {
    private final String macroName;
    private final String outputPort;
    private final int outputBitIndex;
    private final HashMap<String, Long> params;

    public MacroVertex(int id, FunctionType type, int numInputs, String macroName, String outputPort, int outputBitIndex, Map<String, Long> params) {
        super(id, type, numInputs);
        this.macroName = macroName;
        this.outputPort = outputPort;
        this.outputBitIndex = outputBitIndex;
        this.params = new HashMap<String, Long>(params);
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

    public Map<String, Long> getParams() {
        return Collections.unmodifiableMap(this.params);
    }
}
