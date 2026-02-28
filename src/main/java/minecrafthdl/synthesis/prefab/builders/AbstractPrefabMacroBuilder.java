package minecrafthdl.synthesis.prefab.builders;

import MinecraftGraph.Graph;
import MinecraftGraph.In_output;
import MinecraftGraph.Vertex;
import MinecraftGraph.VertexType;
import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.IntermediateCircuit;
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

    protected static void requireInputCount(PrefabMacroGateFactory.Request request, String macro, int expected) {
        if (request.inputCount != expected) {
            throw new MHDLException(
                    "Prefab macro input count mismatch for "
                            + macro
                            + ": expected "
                            + expected
                            + " got "
                            + request.inputCount
            );
        }
    }

    protected static void requireSingleBitOutput(PrefabMacroGateFactory.Request request, String macro, String port) {
        if (!port.equals(request.outputPort) || request.outputBit != 0) {
            throw new MHDLException(
                    "Unsupported output selection for "
                            + macro
                            + ": "
                            + request.outputPort
                            + "["
                            + request.outputBit
                            + "]"
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

    protected static void routeOutputFromInput(Gate gate, int inputIndex) {
        if (gate == null || gate.num_inputs <= 0) {
            return;
        }
        int clamped = Math.max(0, Math.min(gate.num_inputs - 1, inputIndex));
        int inputX = clamped * (1 + gate.input_spacing);
        int outputZ = gate.getSizeZ() - 1;

        for (int z = 0; z <= outputZ; z++) {
            gate.setBlock(inputX, 1, z, Blocks.REDSTONE_WIRE.defaultBlockState());
            gate.setBlock(inputX, 0, z, Blocks.WHITE_WOOL.defaultBlockState());
        }

        int step = inputX <= 0 ? -1 : 1;
        for (int x = inputX; x != 0; x -= step) {
            gate.setBlock(x, 1, outputZ, Blocks.REDSTONE_WIRE.defaultBlockState());
            gate.setBlock(x, 0, outputZ, Blocks.WHITE_WOOL.defaultBlockState());
        }

        gate.setBlock(0, 1, outputZ, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(0, 0, outputZ, Blocks.WHITE_WOOL.defaultBlockState());
    }

    protected static void addLabel(Gate gate, String text) {
        if (gate == null || text == null || text.isBlank()) {
            return;
        }
        gate.setBlock(0, 1, 1, Blocks.OAK_SIGN.defaultBlockState());
        gate.addSignPlacement(new Circuit.SignPlacement(0, 1, 1, text));
    }

    protected static In_output input(String name) {
        return new In_output(1, VertexType.INPUT, name);
    }

    protected static In_output output(String name) {
        return new In_output(1, VertexType.OUTPUT, name);
    }

    protected static void wire(Graph graph, Vertex from, Vertex to) {
        graph.addEdge(from, to);
    }

    protected static Gate synthesizeSubgraphAsGate(Graph graph, int inputCount) {
        boolean previousTestMode = Circuit.TEST;
        Circuit.TEST = false;
        try {
            IntermediateCircuit intermediate = new IntermediateCircuit();
            intermediate.loadGraph(graph);
            intermediate.buildGates();
            intermediate.routeChannels();
            Circuit circuit = intermediate.genCircuit();

            int spacing = inputCount <= 1 ? 0 : 1;
            Gate gate = new Gate(
                    circuit.getSizeX(),
                    circuit.getSizeY(),
                    circuit.getSizeZ(),
                    Math.max(1, inputCount),
                    1,
                    spacing,
                    0,
                    new int[]{0}
            );
            gate.insertCircuit(0, 0, 0, circuit);
            return gate;
        } finally {
            Circuit.TEST = previousTestMode;
        }
    }
}
