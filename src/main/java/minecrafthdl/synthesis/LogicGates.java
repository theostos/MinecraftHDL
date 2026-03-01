package minecrafthdl.synthesis;

import minecrafthdl.Demo;
import minecrafthdl.MHDLException;
import minecrafthdl.Utils;
import minecrafthdl.synthesis.prefab.PrefabMacroGateFactory;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.HashMap;
import java.util.Map;

public class LogicGates {

    public static void main(String[] args) {
        IntermediateCircuit ic = new IntermediateCircuit();
        ic.loadGraph(Demo.create4bitmuxgraph());
        ic.printLayers();
    }

    public static Gate Input(String id) {
        Gate gate = new Gate(1, 3, 1, 1, 1, 0, 0, new int[]{0});
        gate.is_io = true;

        gate.setBlock(0, 0, 0, Blocks.WHITE_WOOL.defaultBlockState());
        gate.setBlock(0, 1, 0, Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH));
        if (id != null && !id.isBlank()) {
            gate.setBlock(0, 2, 0, Utils.standingSignRotation(8));
            gate.addSignPlacement(new Circuit.SignPlacement(0, 2, 0, id));
        }
        return gate;
    }

    public static Gate Output(String id) {
        Gate gate = new Gate(1, 2, 1, 1, 1, 0, 0, new int[]{0});
        gate.is_io = true;

        gate.setBlock(0, 0, 0, Blocks.REDSTONE_LAMP.defaultBlockState());
        if (id != null && !id.isBlank()) {
            gate.setBlock(0, 1, 0, Utils.standingSignRotation(0));
            gate.addSignPlacement(new Circuit.SignPlacement(0, 1, 0, id));
        }
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

    public static Gate MC_TIMER(int ticks, String outputPort, int outputBit) {
        return MC_TIMER(ticks, "", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_TIMER(int ticks, String outputPort, int outputBit, SynthesisOptions options) {
        return MC_TIMER(ticks, "", outputPort, outputBit, options);
    }

    public static Gate MC_TIMER(int ticks, String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("TICKS", (long) ticks);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_timer", 3, params, outputPort, outputBit);
    }

    public static Gate MC_PERIODIC(int period, String outputPort, int outputBit) {
        return MC_PERIODIC(period, "", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_PERIODIC(int period, String outputPort, int outputBit, SynthesisOptions options) {
        return MC_PERIODIC(period, "", outputPort, outputBit, options);
    }

    public static Gate MC_PERIODIC(int period, String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("PERIOD", (long) period);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_periodic", 3, params, outputPort, outputBit);
    }

    public static Gate MC_LATCH(String outputPort, int outputBit) {
        return MC_LATCH("", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_LATCH(String outputPort, int outputBit, SynthesisOptions options) {
        return MC_LATCH("", outputPort, outputBit, options);
    }

    public static Gate MC_LATCH(String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_latch", 4, params, outputPort, outputBit);
    }

    public static Gate MC_COUNTER(int width, String outputPort, int outputBit) {
        return MC_COUNTER(width, "", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_COUNTER(int width, String outputPort, int outputBit, SynthesisOptions options) {
        return MC_COUNTER(width, "", outputPort, outputBit, options);
    }

    public static Gate MC_COUNTER(int width, String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("WIDTH", (long) width);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_counter", 4, params, outputPort, outputBit);
    }

    public static Gate MC_SEQ_LOCK(int btnCount, int seqLen, int latchSuccess, long expectIdx, String outputPort, int outputBit) {
        return MC_SEQ_LOCK(btnCount, seqLen, latchSuccess, expectIdx, "", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_SEQ_LOCK(int btnCount, int seqLen, int latchSuccess, long expectIdx, String outputPort, int outputBit, SynthesisOptions options) {
        return MC_SEQ_LOCK(btnCount, seqLen, latchSuccess, expectIdx, "", outputPort, outputBit, options);
    }

    public static Gate MC_SEQ_LOCK(int btnCount, int seqLen, int latchSuccess, long expectIdx, String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        int inputs = 3 + btnCount;
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("BTN_COUNT", (long) btnCount);
        params.put("SEQ_LEN", (long) seqLen);
        params.put("LATCH_SUCCESS", (long) latchSuccess);
        params.put("EXPECT_IDX", expectIdx);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_seq_lock", inputs, params, outputPort, outputBit);
    }

    public static Gate MC_STATION_FSM(int departTicks, String outputPort, int outputBit) {
        return MC_STATION_FSM(departTicks, "", outputPort, outputBit, SynthesisOptions.defaults());
    }

    public static Gate MC_STATION_FSM(int departTicks, String outputPort, int outputBit, SynthesisOptions options) {
        return MC_STATION_FSM(departTicks, "", outputPort, outputBit, options);
    }

    public static Gate MC_STATION_FSM(int departTicks, String instanceName, String outputPort, int outputBit, SynthesisOptions options) {
        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("DEPART_TICKS", (long) departTicks);
        params.put("AUTO_CLK", 1L);
        params.put("AUTO_CLK_PERIOD_TICKS", (long) options.prefabAutoClockPeriodTicks());
        return macroPrefabOrThrow(instanceName, "mc_station_fsm", 5, params, outputPort, outputBit);
    }

    private static Gate macroPrefabOrThrow(
            String instanceName,
            String macroName,
            int inputCount,
            Map<String, Long> params,
            String outputPort,
            int outputBit
    ) {
        Gate prefab = PrefabMacroGateFactory.tryBuild(
                new PrefabMacroGateFactory.Request(
                        instanceName,
                        macroName,
                        inputCount,
                        params,
                        outputPort,
                        outputBit
                )
        );
        if (prefab == null) {
            throw new MHDLException("No prefab builder available for macro: " + macroName);
        }
        return prefab;
    }
}
