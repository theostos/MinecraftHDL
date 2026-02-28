package minecrafthdl.block.blocks;

import GraphBuilder.GraphBuilder;
import com.mojang.logging.LogUtils;
import minecrafthdl.client.ClientAccess;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.IntermediateCircuit;
import minecrafthdl.synthesis.SynthesisOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.slf4j.Logger;

public class Synthesizer extends Block {
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile String fileToGenerate;

    public Synthesizer() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 10.0f).sound(SoundType.STONE));
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIGGERED, Boolean.FALSE));
    }

    public static void setFileToGenerate(String filePath) {
        fileToGenerate = filePath;
    }

    public static String getFileToGenerate() {
        return fileToGenerate;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.openSynthesizerScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        boolean powered = isReceivingPower(level, pos);
        boolean triggered = state.getValue(TRIGGERED);

        if (!triggered && powered) {
            level.setBlock(pos, state.setValue(TRIGGERED, Boolean.TRUE), Block.UPDATE_CLIENTS);

            if (fileToGenerate != null && !fileToGenerate.isBlank()) {
                synthGen(level, pos);
            }
        } else if (triggered && !powered) {
            level.setBlock(pos, state.setValue(TRIGGERED, Boolean.FALSE), Block.UPDATE_CLIENTS);
        }

        level.updateNeighborsAt(pos, this);
    }

    private static boolean isReceivingPower(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getSignal(pos.relative(direction), direction) > 0) {
                return true;
            }
        }
        return false;
    }

    private void synthGen(Level level, BlockPos pos) {
        try {
            IntermediateCircuit intermediateCircuit = new IntermediateCircuit(SynthesisOptions.fromConfig());
            intermediateCircuit.loadGraph(GraphBuilder.buildGraph(fileToGenerate));
            intermediateCircuit.buildGates();
            intermediateCircuit.routeChannels();

            Circuit circuit = intermediateCircuit.genCircuit();
            circuit.placeInWorld(level, pos, Direction.NORTH);
        } catch (Exception e) {
            LOGGER.error("Failed to generate circuit from {}", fileToGenerate, e);

            if (level instanceof ServerLevel serverLevel) {
                for (ServerPlayer nearby : serverLevel.players()) {
                    if (nearby.blockPosition().closerThan(pos, 64.0d)) {
                        nearby.displayClientMessage(Component.literal("MinecraftHDL: generation failed, check logs").withStyle(ChatFormatting.RED), false);
                    }
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }
}
