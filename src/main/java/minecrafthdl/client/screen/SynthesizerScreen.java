package minecrafthdl.client.screen;

import minecrafthdl.block.blocks.Synthesizer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SynthesizerScreen extends Screen {
    private final BlockPos blockPos;

    private final List<Path> designFiles = new ArrayList<>();
    private int selectedIndex = -1;

    private Button generateButton;

    public SynthesizerScreen(BlockPos blockPos) {
        super(Component.literal("MinecraftHDL Synthesizer"));
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 40;

        this.addRenderableWidget(Button.builder(Component.literal("Prev"), b -> moveSelection(-1))
                .bounds(centerX - 140, buttonY, 60, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Next"), b -> moveSelection(1))
                .bounds(centerX - 70, buttonY, 60, 20)
                .build());

        this.generateButton = this.addRenderableWidget(Button.builder(Component.literal("Select"), b -> applySelection())
                .bounds(centerX, buttonY, 80, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> refreshDesignFiles())
                .bounds(centerX + 90, buttonY, 80, 20)
                .build());

        this.refreshDesignFiles();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 80, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Block: " + this.blockPos.toShortString()), centerX, centerY - 66, 0xA0A0A0);

        if (this.designFiles.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("No .json files found in verilog_designs"), centerX, centerY - 20, 0xFF8080);
            guiGraphics.drawCenteredString(this.font, Component.literal(getDesignDirectory().toString()), centerX, centerY - 6, 0x808080);
        } else {
            Path selected = this.designFiles.get(this.selectedIndex);
            guiGraphics.drawCenteredString(this.font, Component.literal("Selected file:"), centerX, centerY - 28, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, Component.literal(selected.getFileName().toString()), centerX, centerY - 14, 0x55FF55);
            guiGraphics.drawCenteredString(this.font,
                    Component.literal((this.selectedIndex + 1) + " / " + this.designFiles.size()),
                    centerX, centerY, 0xA0A0A0);
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("Choose a file, then power once to preview"),
                    centerX, centerY + 14, 0xA0A0A0);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 264) {
            moveSelection(1);
            return true;
        }
        if (keyCode == 265) {
            moveSelection(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applySelection() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.designFiles.size()) {
            return;
        }

        Path selected = this.designFiles.get(this.selectedIndex).toAbsolutePath();
        Synthesizer.setFileToGenerate(selected.toString());

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("Selected design: " + selected.getFileName() + " (power once for preview, twice to build)").withStyle(ChatFormatting.GREEN),
                    false
            );
        }

        this.onClose();
    }

    private void moveSelection(int delta) {
        if (this.designFiles.isEmpty()) {
            return;
        }

        int next = this.selectedIndex + delta;
        if (next < 0) {
            next = this.designFiles.size() - 1;
        }
        if (next >= this.designFiles.size()) {
            next = 0;
        }
        this.selectedIndex = next;
    }

    private void refreshDesignFiles() {
        this.designFiles.clear();

        Path designDirectory = getDesignDirectory();
        try {
            Files.createDirectories(designDirectory);

            try (Stream<Path> stream = Files.list(designDirectory)) {
                stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                        .forEach(this.designFiles::add);
            }
        } catch (IOException e) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("Could not read verilog_designs folder").withStyle(ChatFormatting.RED),
                        false
                );
            }
        }

        if (this.designFiles.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= this.designFiles.size()) {
            this.selectedIndex = 0;
        }

        if (this.generateButton != null) {
            this.generateButton.active = !this.designFiles.isEmpty();
        }
    }

    private static Path getDesignDirectory() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameDirectory.toPath().resolve("verilog_designs");
    }
}
