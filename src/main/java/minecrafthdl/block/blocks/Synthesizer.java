package minecrafthdl.block.blocks;

import GraphBuilder.GraphBuilder;
import com.mojang.logging.LogUtils;
import minecrafthdl.client.ClientAccess;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.synthesis.IntermediateCircuit;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Synthesizer extends Block {
    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockState PREVIEW_BLOCK = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

    private static volatile String fileToGenerate;
    private static final Map<GlobalPos, PreviewData> PREVIEWS = new ConcurrentHashMap<>();

    private record PreviewData(String sourceFile, Circuit circuit, List<BlockPos> placedBlocks) {
    }

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

            String selectedFile = fileToGenerate;
            if (selectedFile == null || selectedFile.isBlank()) {
                clearPreview(level, pos);
            } else {
                handlePulse(level, pos, selectedFile);
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

    private void handlePulse(Level level, BlockPos pos, String selectedFile) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        PreviewData existingPreview = PREVIEWS.get(key);

        if (existingPreview != null && existingPreview.sourceFile().equals(selectedFile)) {
            clearPreview(level, key);
            placeCircuit(level, pos, existingPreview.circuit(), selectedFile);
            return;
        }

        clearPreview(level, key);

        try {
            Circuit circuit = buildCircuit(selectedFile);
            List<BlockPos> placedPreviewBlocks = placePreview(level, pos, circuit);
            PREVIEWS.put(key, new PreviewData(selectedFile, circuit, placedPreviewBlocks));

            notifyNearby(level, pos,
                    Component.literal("MinecraftHDL preview: " + circuit.getSizeX() + "x" + circuit.getSizeY() + "x" + circuit.getSizeZ() + " blocks. Power again to generate.")
                            .withStyle(ChatFormatting.AQUA));
        } catch (Exception e) {
            LOGGER.error("Failed to build preview from {}", selectedFile, e);
            notifyNearby(level, pos,
                    Component.literal("MinecraftHDL: preview failed, check logs").withStyle(ChatFormatting.RED));
        }
    }

    private static Circuit buildCircuit(String selectedFile) {
        IntermediateCircuit intermediateCircuit = new IntermediateCircuit();
        intermediateCircuit.loadGraph(GraphBuilder.buildGraph(selectedFile));
        intermediateCircuit.buildGates();
        intermediateCircuit.routeChannels();
        return intermediateCircuit.genCircuit();
    }

    private static List<BlockPos> placePreview(Level level, BlockPos pos, Circuit circuit) {
        Circuit.Placement placement = circuit.getPlacement(pos, Direction.NORTH);
        BlockPos min = placement.origin();
        BlockPos max = placement.maxCorner();
        List<BlockPos> placedBlocks = new ArrayList<>();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (!isPreviewBoundary(x, y, z, min, max)) {
                        continue;
                    }

                    BlockPos current = new BlockPos(x, y, z);
                    if (level.isEmptyBlock(current)) {
                        level.setBlock(current, PREVIEW_BLOCK, Block.UPDATE_CLIENTS);
                        placedBlocks.add(current.immutable());
                    }
                }
            }
        }

        return placedBlocks;
    }

    private static boolean isPreviewBoundary(int x, int y, int z, BlockPos min, BlockPos max) {
        return x == min.getX() || x == max.getX()
                || y == min.getY() || y == max.getY()
                || z == min.getZ() || z == max.getZ();
    }

    private static void clearPreview(Level level, BlockPos pos) {
        clearPreview(level, GlobalPos.of(level.dimension(), pos.immutable()));
    }

    private static void clearPreview(Level level, GlobalPos key) {
        PreviewData previewData = PREVIEWS.remove(key);
        if (previewData == null) {
            return;
        }

        for (BlockPos blockPos : previewData.placedBlocks()) {
            if (level.getBlockState(blockPos).is(PREVIEW_BLOCK.getBlock())) {
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void placeCircuit(Level level, BlockPos pos, Circuit circuit, String selectedFile) {
        try {
            circuit.placeInWorld(level, pos, Direction.NORTH);
        } catch (Exception e) {
            LOGGER.error("Failed to generate circuit from {}", selectedFile, e);
            notifyNearby(level, pos,
                    Component.literal("MinecraftHDL: generation failed, check logs").withStyle(ChatFormatting.RED));
        }
    }

    private static void notifyNearby(Level level, BlockPos pos, Component message) {
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer nearby : serverLevel.players()) {
                if (nearby.blockPosition().closerThan(pos, 64.0d)) {
                    nearby.displayClientMessage(message, false);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            clearPreview(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }
}
