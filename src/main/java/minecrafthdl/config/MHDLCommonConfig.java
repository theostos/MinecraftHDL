package minecrafthdl.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MHDLCommonConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue PREFAB_AUTO_CLOCK_PERIOD_TICKS;
    private static final ForgeConfigSpec.IntValue PREFAB_MACRO_TOTAL_BLOCK_BUDGET;
    private static final ForgeConfigSpec.IntValue PREFAB_MACRO_PER_INSTANCE_BLOCK_BUDGET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("prefabMacros");

        PREFAB_AUTO_CLOCK_PERIOD_TICKS = builder
                .comment("Internal prefab macro clock period in game ticks.")
                .defineInRange("autoClockPeriodTicks", 2, 1, 20);

        PREFAB_MACRO_TOTAL_BLOCK_BUDGET = builder
                .comment("Hard synthesis block budget (total circuit volume estimate) when prefab macros are enabled.")
                .defineInRange("totalBlockBudget", 10000, 100, 200000);

        PREFAB_MACRO_PER_INSTANCE_BLOCK_BUDGET = builder
                .comment("Hard synthesis block budget per macro instance when prefab macros are enabled.")
                .defineInRange("perInstanceBlockBudget", 2000, 50, 50000);

        builder.pop();
        SPEC = builder.build();
    }

    private MHDLCommonConfig() {
    }

    public static int prefabAutoClockPeriodTicks() {
        return PREFAB_AUTO_CLOCK_PERIOD_TICKS.get();
    }

    public static int prefabMacroTotalBlockBudget() {
        return PREFAB_MACRO_TOTAL_BLOCK_BUDGET.get();
    }

    public static int prefabMacroPerInstanceBlockBudget() {
        return PREFAB_MACRO_PER_INSTANCE_BLOCK_BUDGET.get();
    }
}
