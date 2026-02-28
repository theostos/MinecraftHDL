package minecrafthdl.block;

import minecrafthdl.MinecraftHDL;
import minecrafthdl.block.blocks.MacroRuntimeBlock;
import minecrafthdl.block.blocks.Synthesizer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MinecraftHDL.MODID);

    public static final RegistryObject<Block> SYNTHESIZER = BLOCKS.register("synthesizer", Synthesizer::new);
    public static final RegistryObject<Block> MACRO_RUNTIME = BLOCKS.register("macro_runtime", MacroRuntimeBlock::new);

    private ModBlocks() {
    }
}
