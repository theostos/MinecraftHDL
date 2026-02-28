package minecrafthdl.simulation.prefab;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cycle-accurate macro simulator used as the prefab acceptance oracle.
 * One call to {@link #tickOutput} represents one game tick.
 */
public final class PrefabMacroSimulator {

    private final int autoClockPeriodTicks;
    private long gameTick = 0L;

    public PrefabMacroSimulator(int autoClockPeriodTicks) {
        this.autoClockPeriodTicks = Math.max(1, autoClockPeriodTicks);
    }

    public boolean tickOutput(
            String macroName,
            String outputPort,
            int outputBit,
            Map<String, Long> params,
            boolean[] rawInputs,
            PrefabMacroModel.State state
    ) {
        boolean[] effectiveInputs = normalizedInputs(rawInputs);
        effectiveInputs[0] = internalClockLevel();

        PrefabMacroModel.step(macroName, params, effectiveInputs, state);
        boolean out = PrefabMacroModel.readOutput(macroName, outputPort, outputBit, state);

        this.gameTick++;
        return out;
    }

    public LinkedHashMap<String, Boolean> tickOutputs(
            String macroName,
            Map<String, Long> params,
            boolean[] rawInputs,
            PrefabMacroModel.State state,
            OutputRef... outputs
    ) {
        boolean[] effectiveInputs = normalizedInputs(rawInputs);
        effectiveInputs[0] = internalClockLevel();
        PrefabMacroModel.step(macroName, params, effectiveInputs, state);

        LinkedHashMap<String, Boolean> values = new LinkedHashMap<String, Boolean>();
        for (OutputRef output : outputs) {
            values.put(
                    output.label,
                    PrefabMacroModel.readOutput(macroName, output.port, output.bitIndex, state)
            );
        }

        this.gameTick++;
        return values;
    }

    private boolean[] normalizedInputs(boolean[] rawInputs) {
        if (rawInputs == null || rawInputs.length == 0) {
            return new boolean[]{false};
        }
        boolean[] copy = new boolean[rawInputs.length];
        System.arraycopy(rawInputs, 0, copy, 0, rawInputs.length);
        return copy;
    }

    private boolean internalClockLevel() {
        return this.gameTick % this.autoClockPeriodTicks == 0;
    }

    public static final class OutputRef {
        public final String label;
        public final String port;
        public final int bitIndex;

        public OutputRef(String label, String port, int bitIndex) {
            this.label = label;
            this.port = port;
            this.bitIndex = bitIndex;
        }

        public static OutputRef of(String label, String port, int bitIndex) {
            return new OutputRef(label, port, bitIndex);
        }
    }
}
