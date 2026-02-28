package minecrafthdl.synthesis;

import minecrafthdl.block.entity.MacroRuntimeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Circuit {

    public static boolean TEST = false;

    private final ArrayList<ArrayList<ArrayList<BlockState>>> blocks;
    private final ArrayList<MacroPlacement> macroPlacements = new ArrayList<MacroPlacement>();

    public static final class MacroPlacement {
        public final int x;
        public final int y;
        public final int z;
        public final String macroName;
        public final String outputPort;
        public final int outputBit;
        public final int inputCount;
        public final int inputStride;
        public final LinkedHashMap<String, Long> params;

        public MacroPlacement(int x, int y, int z, String macroName, String outputPort, int outputBit, int inputCount, int inputStride, Map<String, Long> params) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.macroName = macroName;
            this.outputPort = outputPort;
            this.outputBit = outputBit;
            this.inputCount = inputCount;
            this.inputStride = inputStride;
            this.params = new LinkedHashMap<String, Long>(params);
        }

        public MacroPlacement offsetBy(int dx, int dy, int dz) {
            return new MacroPlacement(
                    this.x + dx,
                    this.y + dy,
                    this.z + dz,
                    this.macroName,
                    this.outputPort,
                    this.outputBit,
                    this.inputCount,
                    this.inputStride,
                    this.params
            );
        }
    }

    public Circuit(int sizeX, int sizeY, int sizeZ) {
        this.blocks = new ArrayList<>();
        for (int x = 0; x < sizeX; x++) {
            this.blocks.add(new ArrayList<>());
            for (int y = 0; y < sizeY; y++) {
                this.blocks.get(x).add(new ArrayList<>());
                for (int z = 0; z < sizeZ; z++) {
                    if (!Circuit.TEST) {
                        this.blocks.get(x).get(y).add(Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    public void setBlock(int x, int y, int z, BlockState blockstate) {
        if (TEST) {
            return;
        }
        this.blocks.get(x).get(y).set(z, blockstate);
    }

    public void placeInWorld(Level level, BlockPos pos, Direction direction) {
        int width = blocks.size();
        int height = blocks.get(0).size();
        int length = blocks.get(0).get(0).size();

        int startX = pos.getX();
        int startY = pos.getY();
        int startZ = pos.getZ();

        if (direction == Direction.NORTH) {
            startZ += 2;
        } else if (direction == Direction.SOUTH) {
            startZ -= length + 1;
        } else if (direction == Direction.EAST) {
            startX -= width + 1;
        } else if (direction == Direction.WEST) {
            startX -= width + 1;
        }

        int y = startY - 1;
        for (int z = startZ - 1; z < startZ + length + 1; z++) {
            for (int x = startX - 1; x < startX + width + 1; x++) {
                level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }

        HashMap<Vec3i, BlockState> torches = new HashMap<>();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < length; k++) {
                    BlockState state = this.getState(i, j, k);

                    if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
                        torches.put(new Vec3i(i, j, k), state);
                    } else {
                        BlockPos blockPos = new BlockPos(startX + i, startY + j, startZ + k);
                        level.setBlockAndUpdate(blockPos, state);
                    }
                }
            }
        }

        for (Map.Entry<Vec3i, BlockState> entry : torches.entrySet()) {
            BlockPos blockPos = new BlockPos(
                    startX + entry.getKey().getX(),
                    startY + entry.getKey().getY(),
                    startZ + entry.getKey().getZ()
            );
            level.setBlockAndUpdate(blockPos, entry.getValue());
        }

        for (MacroPlacement placement : this.macroPlacements) {
            BlockPos blockPos = new BlockPos(startX + placement.x, startY + placement.y, startZ + placement.z);
            if (level.getBlockEntity(blockPos) instanceof MacroRuntimeBlockEntity macroEntity) {
                macroEntity.configure(placement);
                BlockState state = level.getBlockState(blockPos);
                level.sendBlockUpdated(blockPos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public int getSizeX() {
        return this.blocks.size();
    }

    public int getSizeY() {
        return this.blocks.get(0).size();
    }

    public int getSizeZ() {
        return this.blocks.get(0).get(0).size();
    }

    public BlockState getState(int x, int y, int z) {
        return this.blocks.get(x).get(y).get(z);
    }

    public void insertCircuit(int xOffset, int yOffset, int zOffset, Circuit circuit) {
        for (int x = 0; x < circuit.getSizeX(); x++) {
            for (int y = 0; y < circuit.getSizeY(); y++) {
                for (int z = 0; z < circuit.getSizeZ(); z++) {
                    this.setBlock(x + xOffset, y + yOffset, z + zOffset, circuit.getState(x, y, z));
                }
            }
        }

        for (MacroPlacement placement : circuit.macroPlacements) {
            this.macroPlacements.add(placement.offsetBy(xOffset, yOffset, zOffset));
        }
    }

    public void addMacroPlacement(MacroPlacement placement) {
        this.macroPlacements.add(placement);
    }
}
