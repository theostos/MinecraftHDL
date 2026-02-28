package minecrafthdl.synthesis.prefab.builders;

import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;
import net.minecraft.world.level.block.Blocks;

/**
 * Shared helpers for prefab macro gate builders.
 */
public abstract class AbstractPrefabMacroBuilder implements PrefabMacroBuilder {

    protected static int intParam(PrefabMacroGateFactory.Request request, String key, int fallback) {
        if (request == null || request.params == null) {
            return fallback;
        }
        Long value = request.params.get(key);
        return value == null ? fallback : value.intValue();
    }

    protected static void requireRange(String macro, String key, int value, int minInclusive, int maxInclusive) {
        if (value < minInclusive || value > maxInclusive) {
            throw new MHDLException(
                    "Prefab macro parameter out of range for "
                            + macro
                            + "."
                            + key
                            + ": "
                            + value
                            + " (allowed "
                            + minInclusive
                            + ".."
                            + maxInclusive
                            + ")"
            );
        }
    }

    /**
     * Creates a simple shell gate with declared input pins and one output pin.
     * The shell is useful while implementing macro internals incrementally.
     */
    protected static Gate oneOutputShell(int inputCount, int sizeZ) {
        int safeInputs = Math.max(1, inputCount);
        int width = safeInputs == 1 ? 1 : ((safeInputs * 2) - 1);
        int spacing = safeInputs == 1 ? 0 : 1;

        Gate gate = new Gate(width, 2, Math.max(3, sizeZ), safeInputs, 1, spacing, 0, new int[]{0});

        for (int i = 0; i < safeInputs; i++) {
            int x = i * (1 + spacing);
            gate.setBlock(x, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(x, 1, 0, Blocks.REDSTONE_WIRE.defaultBlockState());
        }

        gate.setBlock(0, 0, gate.getSizeZ() - 1, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 1, gate.getSizeZ() - 1, Blocks.REDSTONE_WIRE.defaultBlockState());
        return gate;
    }

    protected static void addLabel(Gate gate, String text) {
        if (gate == null || text == null || text.isBlank()) {
            return;
        }
        gate.setBlock(0, 1, 1, Blocks.OAK_SIGN.defaultBlockState());
        gate.addSignPlacement(new Circuit.SignPlacement(0, 1, 1, text));
    }
}
