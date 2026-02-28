package minecrafthdl.synthesis.prefab;

import minecrafthdl.synthesis.Gate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transition factory for mc_* macro gate synthesis.
 * Runtime path remains fallback while prefab implementations are rolled out.
 */
public final class PrefabMacroGateFactory {

    public static final class Request {
        public final String instanceName;
        public final String macroName;
        public final int inputCount;
        public final Map<String, Long> params;
        public final String outputPort;
        public final int outputBit;

        public Request(
                String instanceName,
                String macroName,
                int inputCount,
                Map<String, Long> params,
                String outputPort,
                int outputBit
        ) {
            this.instanceName = instanceName == null ? "" : instanceName;
            this.macroName = macroName;
            this.inputCount = inputCount;
            this.params = new LinkedHashMap<String, Long>(params);
            this.outputPort = outputPort;
            this.outputBit = outputBit;
        }
    }

    private PrefabMacroGateFactory() {
    }

    public static Gate tryBuild(Request request) {
        if (request == null || request.macroName == null) {
            return null;
        }
        // Placeholder for true-prefab builders per macro.
        // Returning null keeps runtime fallback active during migration.
        return null;
    }
}
