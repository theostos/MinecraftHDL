package minecrafthdl.synthesis.macro;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared runtime state store keyed by a macro instance identity.
 * This prevents multi-output macro instances from stepping once per output.
 */
public final class MacroRuntimeInstanceRegistry {

    public static final class SharedState {
        public final MacroRuntimeModel.State state = new MacroRuntimeModel.State();
        public long lastSteppedTick = Long.MIN_VALUE;
        public boolean initialized;
    }

    private final LinkedHashMap<String, SharedState> byKey = new LinkedHashMap<String, SharedState>();

    public SharedState getOrCreate(String key) {
        SharedState state = this.byKey.get(key);
        if (state == null) {
            state = new SharedState();
            this.byKey.put(key, state);
        }
        return state;
    }

    public void clear(String key) {
        this.byKey.remove(key);
    }

    public static boolean evaluate(
            SharedState shared,
            long gameTick,
            String macroName,
            String outputPort,
            int outputBit,
            Map<String, Long> params,
            boolean[] inputs,
            MacroRuntimeModel.State seed
    ) {
        if (!shared.initialized && seed != null) {
            copyState(seed, shared.state);
            shared.initialized = true;
        }

        if (shared.lastSteppedTick != gameTick) {
            MacroRuntimeModel.step(macroName, params, inputs, shared.state);
            shared.lastSteppedTick = gameTick;
            shared.initialized = true;
        }

        return MacroRuntimeModel.readOutput(macroName, outputPort, outputBit, shared.state);
    }

    public static void copyState(MacroRuntimeModel.State source, MacroRuntimeModel.State target) {
        if (source == null || target == null) {
            return;
        }
        target.prevClk = source.prevClk;
        target.prevInputMask = source.prevInputMask;
        target.autoClockTickCounter = source.autoClockTickCounter;
        target.timerRemaining = source.timerRemaining;
        target.periodicCounter = source.periodicCounter;
        target.periodicPulse = source.periodicPulse;
        target.latchQ = source.latchQ;
        target.counterValue = source.counterValue;
        target.seqProgress = source.seqProgress;
        target.seqUnlocked = source.seqUnlocked;
        target.seqCorrectPulse = source.seqCorrectPulse;
        target.seqWrongPulse = source.seqWrongPulse;
        target.stationState = source.stationState;
        target.stationTicksRemaining = source.stationTicksRemaining;
        target.stationDepartNow = source.stationDepartNow;
    }
}
