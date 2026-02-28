package minecrafthdl.block.entity;

import minecrafthdl.MinecraftHDL;
import minecrafthdl.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MinecraftHDL.MODID);

    public static final RegistryObject<BlockEntityType<MacroRuntimeBlockEntity>> MACRO_RUNTIME = BLOCK_ENTITIES.register(
            "macro_runtime",
            () -> BlockEntityType.Builder.of(MacroRuntimeBlockEntity::new, ModBlocks.MACRO_RUNTIME.get()).build(null)
    );

    private ModBlockEntities() {
    }
}
