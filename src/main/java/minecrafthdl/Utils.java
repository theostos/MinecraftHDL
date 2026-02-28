package minecrafthdl;

import minecrafthdl.synthesis.CircuitTest;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public final class Utils {

    private Utils() {
    }

    public static Property<?> getPropertyByName(Block block, String name) {
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    public static BlockState repeater(Direction direction) {
        return Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, direction);
    }

    public static BlockState wallTorch(Direction direction) {
        return Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, direction);
    }

    public static BlockState wireWithPower(int power) {
        return Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.POWER, power);
    }

    public static BlockState standingSignRotation(int rotation) {
        IntegerProperty rotationProperty = (IntegerProperty) getPropertyByName(Blocks.OAK_SIGN, "rotation");
        if (rotationProperty == null) {
            return Blocks.OAK_SIGN.defaultBlockState();
        }
        return Blocks.OAK_SIGN.defaultBlockState().setValue(rotationProperty, rotation);
    }

    public static void printProperties(Block block) {
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            System.out.println(property.getName());
            System.out.println(property.getPossibleValues());
        }
    }

    public static void printCircuit(CircuitTest circuit) {
        for (int y = 0; y < circuit.getSizeY(); y++) {
            for (int x = 0; x < circuit.getSizeX(); x++) {
                for (int z = 0; z < circuit.getSizeZ(); z++) {
                    System.out.print(circuit.getState(x, y, z));
                }
                System.out.print("\n");
            }
            System.out.print("\n\n");
        }
    }
}
