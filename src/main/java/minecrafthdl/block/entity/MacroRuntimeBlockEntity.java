package minecrafthdl.block.entity;

import minecrafthdl.block.blocks.MacroRuntimeBlock;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.macro.MacroRuntimeModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

public class MacroRuntimeBlockEntity extends BlockEntity {

    private static final String TAG_MACRO_NAME = "macroName";
    private static final String TAG_OUTPUT_PORT = "outputPort";
    private static final String TAG_OUTPUT_BIT = "outputBit";
    private static final String TAG_INPUT_COUNT = "inputCount";
    private static final String TAG_INPUT_STRIDE = "inputStride";
    private static final String TAG_PARAMS = "params";

    private static final String TAG_STATE = "state";
    private static final String TAG_PREV_CLK = "prevClk";
    private static final String TAG_TIMER_REMAINING = "timerRemaining";
    private static final String TAG_PERIODIC_COUNTER = "periodicCounter";
    private static final String TAG_PERIODIC_PULSE = "periodicPulse";
    private static final String TAG_LATCH_Q = "latchQ";
    private static final String TAG_COUNTER_VALUE = "counterValue";
    private static final String TAG_SEQ_PROGRESS = "seqProgress";
    private static final String TAG_SEQ_UNLOCKED = "seqUnlocked";
    private static final String TAG_SEQ_CORRECT = "seqCorrectPulse";
    private static final String TAG_SEQ_WRONG = "seqWrongPulse";
    private static final String TAG_STATION_STATE = "stationState";
    private static final String TAG_STATION_TICKS = "stationTicksRemaining";
    private static final String TAG_STATION_DEPART = "stationDepartNow";

    private String macroName = "";
    private String outputPort = "";
    private int outputBit = 0;
    private int inputCount = 0;
    private int inputStride = 2;

    private final LinkedHashMap<String, Long> params = new LinkedHashMap<String, Long>();
    private final MacroRuntimeModel.State state = new MacroRuntimeModel.State();

    public MacroRuntimeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MACRO_RUNTIME.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, MacroRuntimeBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    public void configure(Circuit.MacroPlacement placement) {
        this.macroName = placement.macroName;
        this.outputPort = placement.outputPort;
        this.outputBit = placement.outputBit;
        this.inputCount = placement.inputCount;
        this.inputStride = placement.inputStride;

        this.params.clear();
        this.params.putAll(placement.params);

        this.state.resetAll();
        setPowered(false);
        setChanged();
    }

    private void tickServer() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        if (this.macroName.isBlank()) {
            return;
        }

        boolean[] inputs = new boolean[Math.max(0, this.inputCount)];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = readInput(i);
        }

        boolean out = MacroRuntimeModel.tick(this.macroName, this.outputPort, this.outputBit, this.params, inputs, this.state);
        setPowered(out);
        setChanged();
    }

    private boolean readInput(int index) {
        if (this.level == null) {
            return false;
        }

        BlockPos pinPos = this.worldPosition.offset(index * this.inputStride, 0, -2);
        int power = 0;

        BlockState pinState = this.level.getBlockState(pinPos);
        if (pinState.is(Blocks.REDSTONE_WIRE)) {
            power = Math.max(power, pinState.getValue(RedStoneWireBlock.POWER));
        }

        power = Math.max(power, this.level.getBestNeighborSignal(pinPos));

        for (Direction direction : Direction.values()) {
            power = Math.max(power, this.level.getSignal(pinPos.relative(direction), direction));
        }

        return power > 0;
    }

    private void setPowered(boolean powered) {
        if (this.level == null) {
            return;
        }

        BlockState current = this.getBlockState();
        if (!current.hasProperty(MacroRuntimeBlock.POWERED)) {
            return;
        }

        boolean previous = current.getValue(MacroRuntimeBlock.POWERED);
        if (previous == powered) {
            return;
        }

        BlockState updated = current.setValue(MacroRuntimeBlock.POWERED, powered);
        this.level.setBlock(this.worldPosition, updated, Block.UPDATE_CLIENTS);
        this.level.updateNeighborsAt(this.worldPosition, updated.getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString(TAG_MACRO_NAME, this.macroName);
        tag.putString(TAG_OUTPUT_PORT, this.outputPort);
        tag.putInt(TAG_OUTPUT_BIT, this.outputBit);
        tag.putInt(TAG_INPUT_COUNT, this.inputCount);
        tag.putInt(TAG_INPUT_STRIDE, this.inputStride);

        CompoundTag paramsTag = new CompoundTag();
        for (Map.Entry<String, Long> entry : this.params.entrySet()) {
            paramsTag.putLong(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_PARAMS, paramsTag);

        CompoundTag stateTag = new CompoundTag();
        stateTag.putBoolean(TAG_PREV_CLK, this.state.prevClk);
        stateTag.putInt(TAG_TIMER_REMAINING, this.state.timerRemaining);
        stateTag.putInt(TAG_PERIODIC_COUNTER, this.state.periodicCounter);
        stateTag.putBoolean(TAG_PERIODIC_PULSE, this.state.periodicPulse);
        stateTag.putBoolean(TAG_LATCH_Q, this.state.latchQ);
        stateTag.putInt(TAG_COUNTER_VALUE, this.state.counterValue);
        stateTag.putInt(TAG_SEQ_PROGRESS, this.state.seqProgress);
        stateTag.putBoolean(TAG_SEQ_UNLOCKED, this.state.seqUnlocked);
        stateTag.putBoolean(TAG_SEQ_CORRECT, this.state.seqCorrectPulse);
        stateTag.putBoolean(TAG_SEQ_WRONG, this.state.seqWrongPulse);
        stateTag.putInt(TAG_STATION_STATE, this.state.stationState);
        stateTag.putInt(TAG_STATION_TICKS, this.state.stationTicksRemaining);
        stateTag.putBoolean(TAG_STATION_DEPART, this.state.stationDepartNow);
        tag.put(TAG_STATE, stateTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.macroName = tag.getString(TAG_MACRO_NAME);
        this.outputPort = tag.getString(TAG_OUTPUT_PORT);
        this.outputBit = tag.getInt(TAG_OUTPUT_BIT);
        this.inputCount = tag.getInt(TAG_INPUT_COUNT);
        this.inputStride = Math.max(1, tag.getInt(TAG_INPUT_STRIDE));

        this.params.clear();
        CompoundTag paramsTag = tag.getCompound(TAG_PARAMS);
        for (String key : paramsTag.getAllKeys()) {
            this.params.put(key, paramsTag.getLong(key));
        }

        this.state.resetAll();
        if (tag.contains(TAG_STATE)) {
            CompoundTag stateTag = tag.getCompound(TAG_STATE);
            this.state.prevClk = stateTag.getBoolean(TAG_PREV_CLK);
            this.state.timerRemaining = stateTag.getInt(TAG_TIMER_REMAINING);
            this.state.periodicCounter = stateTag.getInt(TAG_PERIODIC_COUNTER);
            this.state.periodicPulse = stateTag.getBoolean(TAG_PERIODIC_PULSE);
            this.state.latchQ = stateTag.getBoolean(TAG_LATCH_Q);
            this.state.counterValue = stateTag.getInt(TAG_COUNTER_VALUE);
            this.state.seqProgress = stateTag.getInt(TAG_SEQ_PROGRESS);
            this.state.seqUnlocked = stateTag.getBoolean(TAG_SEQ_UNLOCKED);
            this.state.seqCorrectPulse = stateTag.getBoolean(TAG_SEQ_CORRECT);
            this.state.seqWrongPulse = stateTag.getBoolean(TAG_SEQ_WRONG);
            this.state.stationState = stateTag.getInt(TAG_STATION_STATE);
            this.state.stationTicksRemaining = stateTag.getInt(TAG_STATION_TICKS);
            this.state.stationDepartNow = stateTag.getBoolean(TAG_STATION_DEPART);
        }
    }
}
