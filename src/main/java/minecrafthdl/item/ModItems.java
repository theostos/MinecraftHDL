package minecrafthdl.item;

import minecrafthdl.MinecraftHDL;
import minecrafthdl.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MinecraftHDL.MODID);

    public static final RegistryObject<Item> SYNTHESIZER_ITEM = ITEMS.register("synthesizer",
            () -> new BlockItem(ModBlocks.SYNTHESIZER.get(), new Item.Properties()));

    private ModItems() {
    }
}
