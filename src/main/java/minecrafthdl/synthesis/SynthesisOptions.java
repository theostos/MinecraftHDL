package minecrafthdl.synthesis;

import minecrafthdl.config.MHDLCommonConfig;

public final class SynthesisOptions {

    private final boolean prefabMacrosEnabled;
    private final int prefabAutoClockPeriodTicks;
    private final int prefabMacroTotalBlockBudget;
    private final int prefabMacroPerInstanceBlockBudget;

    public SynthesisOptions(boolean prefabMacrosEnabled, int prefabAutoClockPeriodTicks, int prefabMacroTotalBlockBudget, int prefabMacroPerInstanceBlockBudget) {
        this.prefabMacrosEnabled = prefabMacrosEnabled;
        this.prefabAutoClockPeriodTicks = prefabAutoClockPeriodTicks;
        this.prefabMacroTotalBlockBudget = prefabMacroTotalBlockBudget;
        this.prefabMacroPerInstanceBlockBudget = prefabMacroPerInstanceBlockBudget;
    }

    public static SynthesisOptions fromConfig() {
        return new SynthesisOptions(
                MHDLCommonConfig.prefabMacrosEnabled(),
                MHDLCommonConfig.prefabAutoClockPeriodTicks(),
                MHDLCommonConfig.prefabMacroTotalBlockBudget(),
                MHDLCommonConfig.prefabMacroPerInstanceBlockBudget()
        );
    }

    public static SynthesisOptions defaults() {
        return new SynthesisOptions(false, 2, 10000, 2000);
    }

    public boolean prefabMacrosEnabled() {
        return this.prefabMacrosEnabled;
    }

    public int prefabAutoClockPeriodTicks() {
        return this.prefabAutoClockPeriodTicks;
    }

    public int prefabMacroTotalBlockBudget() {
        return this.prefabMacroTotalBlockBudget;
    }

    public int prefabMacroPerInstanceBlockBudget() {
        return this.prefabMacroPerInstanceBlockBudget;
    }
}
