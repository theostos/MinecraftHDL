package minecrafthdl.client;

import minecrafthdl.client.screen.SynthesizerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientAccess {
    private ClientAccess() {
    }

    public static void openSynthesizerScreen(BlockPos blockPos) {
        Minecraft.getInstance().setScreen(new SynthesizerScreen(blockPos));
    }
}
