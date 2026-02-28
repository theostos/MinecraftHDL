package minecrafthdl.synthesis;

import minecrafthdl.Demo;
import minecrafthdl.MHDLException;
import minecrafthdl.Utils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public class LogicGates {

    public static void main(String[] args) {
        IntermediateCircuit ic = new IntermediateCircuit();
        ic.loadGraph(Demo.create4bitmuxgraph());
        ic.printLayers();
    }

    public static Gate Input(String id) {
        Gate gate = new Gate(1, 2, 1, 1, 1, 0, 0, new int[]{0});
        gate.is_io = true;

        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 1, 0, Utils.standingSignRotation(8));
        return gate;
    }

    public static Gate Output(String id) {
        Gate gate = new Gate(1, 2, 1, 1, 1, 0, 0, new int[]{0});
        gate.is_io = true;

        gate.setBlock(0, 0, 0, Blocks.REDSTONE_LAMP.defaultBlockState());
        gate.setBlock(0, 1, 0, Utils.standingSignRotation(0));
        return gate;
    }

    public static Gate NOT() {
        Gate gate = new Gate(1, 1, 3, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 1, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(0, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        return gate;
    }

    public static Gate RELAY() {
        Gate gate = new Gate(1, 1, 3, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(0, 0, 1, Utils.repeater(Direction.NORTH));
        gate.setBlock(0, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        return gate;
    }

    public static Gate AND(int inputs) {
        if (inputs == 0) {
            throw new MHDLException("Gate cannot have 0 inputs");
        }

        int width;
        if (inputs == 1) {
            width = 1;
        } else {
            width = (inputs * 2) - 1;
        }

        Gate gate = new Gate(width, 2, 4, inputs, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 2, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(0, 0, 3, Blocks.REDSTONE_WIRE.defaultBlockState());

        for (int i = 0; i < width; i += 2) {
            gate.setBlock(i, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(i, 0, 1, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(i, 1, 0, Blocks.REDSTONE_TORCH.defaultBlockState());
            gate.setBlock(i, 1, 1, Blocks.REDSTONE_WIRE.defaultBlockState());

            if (i != width - 1) {
                gate.setBlock(i + 1, 0, 1, Blocks.WHITE_WOOL.defaultBlockState());
                if (i == 14) {
                    gate.setBlock(i + 1, 1, 1, Utils.repeater(Direction.EAST));
                } else {
                    gate.setBlock(i + 1, 1, 1, Blocks.REDSTONE_WIRE.defaultBlockState());
                }
            }
        }

        return gate;
    }

    public static Gate OR(int inputs) {
        if (inputs == 0) {
            throw new MHDLException("Gate cannot have 0 inputs");
        }

        int width;
        if (inputs == 1) {
            width = 1;
        } else {
            width = (inputs * 2) - 1;
        }

        Gate gate = new Gate(width, 2, 4, inputs, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 3, Blocks.REDSTONE_WIRE.defaultBlockState());

        for (int i = 0; i < width; i += 2) {
            gate.setBlock(i, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(i, 0, 1, Utils.repeater(Direction.NORTH));
            gate.setBlock(i, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
            if (i != width - 1) {
                if (i == 14) {
                    gate.setBlock(i + 1, 0, 2, Utils.repeater(Direction.EAST));
                } else {
                    gate.setBlock(i + 1, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
                }
            }
        }
        return gate;
    }

    public static Gate XOR() {
        Gate gate = new Gate(3, 2, 7, 2, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 1, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(0, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(0, 0, 3, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 4, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(0, 0, 5, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(0, 1, 0, Blocks.REDSTONE_TORCH.defaultBlockState());
        gate.setBlock(0, 1, 3, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(1, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(1, 1, 0, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(1, 0, 1, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(1, 1, 1, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(1, 0, 2, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(1, 0, 4, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(1, 0, 5, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(2, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(2, 0, 1, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(2, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(2, 0, 3, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(2, 0, 4, Utils.wallTorch(Direction.SOUTH));

        gate.setBlock(2, 1, 0, Blocks.REDSTONE_TORCH.defaultBlockState());
        gate.setBlock(2, 1, 3, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(0, 0, 6, Utils.repeater(Direction.NORTH));

        return gate;
    }

    public static Gate MUX() {
        Gate gate = new Gate(5, 2, 6, 3, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 1, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 2, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 3, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(0, 0, 4, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(0, 0, 5, Utils.repeater(Direction.NORTH));

        gate.setBlock(1, 0, 2, Utils.repeater(Direction.EAST));
        gate.setBlock(1, 0, 4, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(2, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(2, 0, 1, Utils.repeater(Direction.NORTH));
        gate.setBlock(2, 0, 2, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(2, 0, 4, Blocks.REDSTONE_WIRE.defaultBlockState());

        gate.setBlock(3, 0, 2, Utils.wallTorch(Direction.EAST));
        gate.setBlock(3, 0, 4, Utils.wallTorch(Direction.WEST));

        gate.setBlock(4, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(4, 0, 1, Utils.wallTorch(Direction.SOUTH));
        gate.setBlock(4, 0, 2, Utils.wireWithPower(10));
        gate.setBlock(4, 0, 3, Utils.wireWithPower(10));
        gate.setBlock(4, 0, 4, Blocks.WHITE_WOOL.defaultBlockState());

        gate.setBlock(0, 1, 0, Blocks.REDSTONE_TORCH.defaultBlockState());
        gate.setBlock(0, 1, 1, Utils.wireWithPower(10));
        gate.setBlock(0, 1, 2, Utils.wireWithPower(10));

        return gate;
    }

    public static Gate LOW() {
        Gate gate = new Gate(1, 1, 1, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        return gate;
    }

    public static Gate HIGH() {
        Gate gate = new Gate(1, 1, 1, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, Blocks.REDSTONE_TORCH.defaultBlockState());
        return gate;
    }

    public static Gate D_LATCH() {
        Gate gate = new Gate(3, 1, 4, 2, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 0, 1, Utils.repeater(Direction.NORTH));
        gate.setBlock(0, 0, 2, Utils.repeater(Direction.NORTH));
        gate.setBlock(0, 0, 3, Blocks.WHITE_WOOL.defaultBlockState());

        gate.setBlock(1, 0, 2, Utils.repeater(Direction.EAST));

        gate.setBlock(2, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(2, 0, 1, Utils.repeater(Direction.NORTH));
        gate.setBlock(2, 0, 2, Blocks.REDSTONE_WIRE.defaultBlockState());

        return gate;
    }

    public static Gate MC_TIMER(int ticks, int outputBit) {
        return macroModule(3, 2, "TIMER", ticks, outputBit);
    }

    public static Gate MC_PERIODIC(int period, int outputBit) {
        return macroModule(3, 2, "PERIODIC", period, outputBit);
    }

    public static Gate MC_LATCH(int outputBit) {
        return macroModule(4, 2, "LATCH", 0, outputBit);
    }

    public static Gate MC_COUNTER(int width, int outputBit) {
        return macroModule(4, 2, "COUNTER", width, outputBit);
    }

    public static Gate MC_SEQ_LOCK(int btnCount, int seqLen, int latchSuccess, int outputBit) {
        int inputs = 3 + btnCount;
        int tagValue = (seqLen << 1) | (latchSuccess & 1);
        return macroModule(inputs, 2, "SEQLOCK", tagValue, outputBit);
    }

    public static Gate MC_STATION_FSM(int departTicks, int outputBit) {
        return macroModule(5, 2, "STATION", departTicks, outputBit);
    }

    private static Gate macroModule(int inputs, int passthroughInput, String tag, int paramValue, int outputBit) {
        if (inputs < 1) {
            throw new MHDLException("Macro module requires at least one input");
        }

        int width = inputs == 1 ? 1 : (inputs * 2) - 1;
        int inputSpacing = inputs == 1 ? 0 : 1;
        Gate gate = new Gate(width, 2, 4, inputs, 1, inputSpacing, 0, new int[]{0});

        int selectedInput = Math.max(0, Math.min(inputs - 1, passthroughInput));
        int selectedX = selectedInput * (1 + inputSpacing);

        for (int i = 0; i < inputs; i++) {
            int x = i * (1 + inputSpacing);
            gate.setBlock(x, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(x, 0, 1, Blocks.WHITE_WOOL.defaultBlockState());
            gate.setBlock(x, 1, 1, Blocks.REDSTONE_WIRE.defaultBlockState());
        }

        gate.setBlock(selectedX, 0, 2, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(selectedX, 1, 2, Blocks.REDSTONE_WIRE.defaultBlockState());
        gate.setBlock(selectedX, 0, 3, Utils.repeater(Direction.NORTH));

        int markerX = width - 1;
        gate.setBlock(markerX, 0, 2, Blocks.GRAY_WOOL.defaultBlockState());
        if (((paramValue + outputBit + tag.length()) & 1) == 0) {
            gate.setBlock(markerX, 1, 2, Blocks.REDSTONE_TORCH.defaultBlockState());
        } else {
            gate.setBlock(markerX, 1, 2, Blocks.AIR.defaultBlockState());
        }

        return gate;
    }
}
