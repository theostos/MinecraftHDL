package minecrafthdl.synthesis.prefab;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.builders.McCounterPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.McLatchPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.McPeriodicPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.McSeqLockPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.McStationFsmPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.McTimerPrefabBuilder;
import minecrafthdl.synthesis.prefab.builders.PrefabMacroBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transition factory for mc_* macro gate synthesis.
 * Runtime path remains fallback while prefab implementations are rolled out.
 */
public final class PrefabMacroGateFactory {

    private static final LinkedHashMap<String, PrefabMacroBuilder> BUILDERS = new LinkedHashMap<String, PrefabMacroBuilder>();

    static {
        BUILDERS.put("mc_timer", new McTimerPrefabBuilder());
        BUILDERS.put("mc_periodic", new McPeriodicPrefabBuilder());
        BUILDERS.put("mc_latch", new McLatchPrefabBuilder());
        BUILDERS.put("mc_counter", new McCounterPrefabBuilder());
        BUILDERS.put("mc_seq_lock", new McSeqLockPrefabBuilder());
        BUILDERS.put("mc_station_fsm", new McStationFsmPrefabBuilder());
    }

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
        PrefabMacroBuilder builder = BUILDERS.get(request.macroName);
        if (builder == null) {
            return null;
        }
        return builder.build(request);
    }
}
