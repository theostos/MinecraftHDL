package minecrafthdl.synthesis.macro;

import java.util.Locale;
import java.util.Map;

public final class MacroRuntimeModel {

    private MacroRuntimeModel() {
    }

    public static final class State {
        public boolean prevClk;
        public long prevInputMask;
        public int autoClockTickCounter;

        public int timerRemaining;

        public int periodicCounter;
        public boolean periodicPulse;

        public boolean latchQ;

        public int counterValue;

        public int seqProgress;
        public boolean seqUnlocked;
        public boolean seqCorrectPulse;
        public boolean seqWrongPulse;

        public int stationState;
        public int stationTicksRemaining;
        public boolean stationDepartNow;

        public void resetAll() {
            prevClk = false;
            prevInputMask = 0L;
            autoClockTickCounter = 0;
            timerRemaining = 0;
            periodicCounter = 0;
            periodicPulse = false;
            latchQ = false;
            counterValue = 0;
            seqProgress = 0;
            seqUnlocked = false;
            seqCorrectPulse = false;
            seqWrongPulse = false;
            stationState = 0;
            stationTicksRemaining = 0;
            stationDepartNow = false;
        }
    }

    public static boolean tick(String macroName, String outputPort, int outputBit, Map<String, Long> params, boolean[] inputs, State state) {
        step(macroName, params, inputs, state);
        return readOutput(macroName, outputPort, outputBit, state);
    }

    /**
     * Advances one macro instance by one simulation/server tick.
     * Output querying should be done via {@link #readOutput(String, String, int, State)}.
     */
    public static void step(String macroName, Map<String, Long> params, boolean[] inputs, State state) {
        if (macroName == null || state == null) {
            return;
        }

        String normalized = macroName.toLowerCase(Locale.ROOT);
        long currentInputMask = inputMask(inputs);
        long previousInputMask = state.prevInputMask;
        boolean rising = risingEdge(params, inputs, state);

        switch (normalized) {
            case "mc_timer":
                stepTimer(rising, inputs, params, state, currentInputMask, previousInputMask);
                break;

            case "mc_periodic":
                stepPeriodic(rising, inputs, params, state);
                break;

            case "mc_latch":
                stepLatch(rising, inputs, state, currentInputMask, previousInputMask);
                break;

            case "mc_counter":
                stepCounter(rising, inputs, params, state, currentInputMask, previousInputMask);
                break;

            case "mc_seq_lock":
                stepSeqLock(rising, inputs, params, state, currentInputMask, previousInputMask);
                break;

            case "mc_station_fsm":
                stepStation(rising, inputs, params, state, currentInputMask, previousInputMask);
                break;

            default:
                break;
        }

        state.prevInputMask = currentInputMask;
    }

    public static boolean readOutput(String macroName, String outputPort, int outputBit, State state) {
        if (macroName == null || outputPort == null || state == null) {
            return false;
        }

        String normalized = macroName.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "mc_timer":
                return "active".equals(outputPort) && state.timerRemaining > 0;
            case "mc_periodic":
                return "pulse".equals(outputPort) && state.periodicPulse;
            case "mc_latch":
                return "q".equals(outputPort) && state.latchQ;
            case "mc_counter":
                return "count".equals(outputPort) && bit(state.counterValue, outputBit);
            case "mc_seq_lock":
                if ("unlocked".equals(outputPort)) {
                    return state.seqUnlocked;
                }
                if ("correct_pulse".equals(outputPort)) {
                    return state.seqCorrectPulse;
                }
                if ("wrong_pulse".equals(outputPort)) {
                    return state.seqWrongPulse;
                }
                return "progress".equals(outputPort) && bit(state.seqProgress, outputBit);
            case "mc_station_fsm":
                if ("occupied".equals(outputPort)) {
                    return state.stationState != 0;
                }
                return "depart_now".equals(outputPort) && state.stationDepartNow;
            default:
                return false;
        }
    }

    private static boolean risingEdge(Map<String, Long> params, boolean[] inputs, State state) {
        if (intParam(params, "AUTO_CLK", 0) != 0) {
            int periodTicks = Math.max(1, intParam(params, "AUTO_CLK_PERIOD_TICKS", 2));
            boolean clk = state.autoClockTickCounter % periodTicks == 0;
            state.autoClockTickCounter++;

            boolean rising = !state.prevClk && clk;
            state.prevClk = clk;
            return rising;
        }
        boolean clk = input(inputs, 0);
        boolean rising = !state.prevClk && clk;
        state.prevClk = clk;
        return rising;
    }

    private static void stepTimer(boolean rising, boolean[] inputs, Map<String, Long> params, State state, long currentInputMask, long previousInputMask) {
        if (!rising) {
            return;
        }

        int ticks = intParam(params, "TICKS", 60);
        boolean rst = input(inputs, 1);
        boolean triggerPulse = inputPulse(currentInputMask, previousInputMask, 2);

        if (rst) {
            state.timerRemaining = 0;
        } else if (triggerPulse) {
            state.timerRemaining = ticks;
        } else if (state.timerRemaining > 0) {
            state.timerRemaining--;
        }
    }

    private static void stepPeriodic(boolean rising, boolean[] inputs, Map<String, Long> params, State state) {
        if (!rising) {
            return;
        }

        int period = Math.max(1, intParam(params, "PERIOD", 20));
        boolean rst = input(inputs, 1);
        boolean enable = input(inputs, 2);

        if (rst) {
            state.periodicCounter = 0;
            state.periodicPulse = false;
            return;
        }

        if (!enable) {
            state.periodicPulse = false;
            return;
        }

        if (period == 1 || state.periodicCounter >= period - 1) {
            state.periodicCounter = 0;
            state.periodicPulse = true;
        } else {
            state.periodicCounter++;
            state.periodicPulse = false;
        }
    }

    private static void stepLatch(boolean rising, boolean[] inputs, State state, long currentInputMask, long previousInputMask) {
        if (!rising) {
            return;
        }

        boolean rst = input(inputs, 1);
        boolean setPulse = inputPulse(currentInputMask, previousInputMask, 2);
        boolean clearPulse = inputPulse(currentInputMask, previousInputMask, 3);

        if (rst || clearPulse) {
            state.latchQ = false;
        } else if (setPulse) {
            state.latchQ = true;
        }
    }

    private static void stepCounter(boolean rising, boolean[] inputs, Map<String, Long> params, State state, long currentInputMask, long previousInputMask) {
        if (!rising) {
            return;
        }

        int width = Math.max(1, intParam(params, "WIDTH", 8));
        int mask = width >= 31 ? -1 : ((1 << width) - 1);

        boolean rst = input(inputs, 1);
        boolean incPulse = inputPulse(currentInputMask, previousInputMask, 2);
        boolean clearPulse = inputPulse(currentInputMask, previousInputMask, 3);

        if (rst || clearPulse) {
            state.counterValue = 0;
        } else if (incPulse) {
            state.counterValue = (state.counterValue + 1) & mask;
        }
    }

    private static void stepSeqLock(boolean rising, boolean[] inputs, Map<String, Long> params, State state, long currentInputMask, long previousInputMask) {
        if (!rising) {
            return;
        }

        int btnCount = Math.max(1, intParam(params, "BTN_COUNT", 3));
        int seqLen = Math.max(1, intParam(params, "SEQ_LEN", 3));
        boolean latchSuccess = intParam(params, "LATCH_SUCCESS", 1) != 0;
        long expectIdx = longParam(params, "EXPECT_IDX", 0L);

        state.seqCorrectPulse = false;
        state.seqWrongPulse = false;
        if (!latchSuccess) {
            state.seqUnlocked = false;
        }

        boolean rst = input(inputs, 1);
        boolean clear = input(inputs, 2);
        if (rst || clear) {
            state.seqProgress = 0;
            state.seqUnlocked = false;
            return;
        }

        int pressed = decodeOneHotPulses(currentInputMask, previousInputMask, 3, btnCount);
        if (pressed == -1) {
            return;
        }

        if (pressed == -2) {
            state.seqWrongPulse = true;
            state.seqProgress = 0;
            return;
        }

        int expected = expectedIndex(expectIdx, btnCount, state.seqProgress);
        if (pressed == expected) {
            int nextProgress = state.seqProgress + 1;
            if (nextProgress >= seqLen) {
                state.seqCorrectPulse = true;
                state.seqUnlocked = true;
                state.seqProgress = 0;
            } else {
                state.seqProgress = nextProgress;
            }
        } else {
            state.seqWrongPulse = true;
            state.seqProgress = 0;
        }
    }

    private static void stepStation(boolean rising, boolean[] inputs, Map<String, Long> params, State state, long currentInputMask, long previousInputMask) {
        if (!rising) {
            return;
        }

        int departTicks = Math.max(1, intParam(params, "DEPART_TICKS", 20));

        boolean rst = input(inputs, 1);
        boolean clear = input(inputs, 2);
        boolean arrivalPulse = inputPulse(currentInputMask, previousInputMask, 3);
        boolean departPulse = inputPulse(currentInputMask, previousInputMask, 4);

        if (rst || clear) {
            state.stationState = 0;
            state.stationTicksRemaining = 0;
            state.stationDepartNow = false;
            return;
        }

        if (state.stationState == 0) {
            state.stationDepartNow = false;
            if (arrivalPulse) {
                state.stationState = 1;
            }
            return;
        }

        if (state.stationState == 1) {
            state.stationDepartNow = false;
            if (departPulse) {
                state.stationState = 2;
                state.stationTicksRemaining = departTicks;
                state.stationDepartNow = true;
            }
            return;
        }

        if (state.stationState == 2) {
            if (state.stationTicksRemaining > 0) {
                state.stationTicksRemaining--;
            }
            state.stationDepartNow = state.stationTicksRemaining > 0;
            if (state.stationTicksRemaining <= 0) {
                state.stationState = 0;
                state.stationDepartNow = false;
            }
        }
    }

    private static int decodeOneHotPulses(long currentInputMask, long previousInputMask, int start, int width) {
        int pressed = -1;
        for (int i = 0; i < width; i++) {
            if (!inputPulse(currentInputMask, previousInputMask, start + i)) {
                continue;
            }
            if (pressed != -1) {
                return -2;
            }
            pressed = i;
        }
        return pressed;
    }

    private static int expectedIndex(long expectIdx, int btnCount, int progress) {
        int bitsPerStep = ceilLog2(btnCount);
        int shift = progress * bitsPerStep;
        int mask = (1 << bitsPerStep) - 1;
        return (int) ((expectIdx >> shift) & mask);
    }

    private static int ceilLog2(int value) {
        int v = 0;
        int n = Math.max(1, value - 1);
        while (n > 0) {
            n >>= 1;
            v++;
        }
        return Math.max(1, v);
    }

    private static int intParam(Map<String, Long> params, String key, int fallback) {
        if (params == null) {
            return fallback;
        }
        Long value = params.get(key);
        return value == null ? fallback : value.intValue();
    }

    private static long longParam(Map<String, Long> params, String key, long fallback) {
        if (params == null) {
            return fallback;
        }
        Long value = params.get(key);
        return value == null ? fallback : value;
    }

    private static boolean input(boolean[] inputs, int index) {
        return index >= 0 && index < inputs.length && inputs[index];
    }

    private static long inputMask(boolean[] inputs) {
        long mask = 0L;
        int max = Math.min(63, inputs.length);
        for (int i = 0; i < max; i++) {
            if (inputs[i]) {
                mask |= (1L << i);
            }
        }
        return mask;
    }

    private static boolean inputPulse(long currentMask, long previousMask, int index) {
        if (index < 0 || index >= 63) {
            return false;
        }
        long bit = 1L << index;
        return (currentMask & bit) != 0L && (previousMask & bit) == 0L;
    }

    private static boolean bit(int value, int bitIndex) {
        if (bitIndex < 0 || bitIndex >= Integer.SIZE) {
            return false;
        }
        return ((value >>> bitIndex) & 1) == 1;
    }
}
