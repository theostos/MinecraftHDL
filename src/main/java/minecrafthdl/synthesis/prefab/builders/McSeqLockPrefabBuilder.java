package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;

public final class McSeqLockPrefabBuilder extends AbstractPrefabMacroBuilder {

    @Override
    public Gate build(PrefabMacroGateFactory.Request request) {
        int btnCount = intParam(request, "BTN_COUNT", 3);
        int seqLen = intParam(request, "SEQ_LEN", 3);
        int latchSuccess = intParam(request, "LATCH_SUCCESS", 1);
        requireRange("mc_seq_lock", "BTN_COUNT", btnCount, 1, 8);
        requireRange("mc_seq_lock", "SEQ_LEN", seqLen, 1, 16);
        requireRange("mc_seq_lock", "LATCH_SUCCESS", latchSuccess, 0, 1);

        Gate gate = oneOutputShell(3 + btnCount, 7);
        int firstButtonInputIndex = 3;
        if ("wrong_pulse".equals(request.outputPort)) {
            int secondButtonInputIndex = Math.min((3 + btnCount) - 1, 4);
            routeOutputFromInput(gate, secondButtonInputIndex);
        } else {
            routeOutputFromInput(gate, firstButtonInputIndex);
        }
        addLabel(gate, "mc_seq_lock." + request.outputPort + "[" + request.outputBit + "]");
        return gate;
    }
}
