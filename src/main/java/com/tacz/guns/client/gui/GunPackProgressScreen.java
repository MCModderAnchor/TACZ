package com.tacz.guns.client.gui;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ProgressListener;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class GunPackProgressScreen extends Screen implements ProgressListener {
    private @Nullable Component header;
    private @Nullable Component stage;
    private int progress;
    private boolean stop;

    public GunPackProgressScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    protected void init() {
        Button button = Button.builder(
                Component.translatable("gui.tacz.client_gun_pack_downloader.background_download"), b -> this.stop()
        ).bounds((width - 200) / 2, 120, 200, 20).build();
        this.addRenderableWidget(button);
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (this.stop) {
            this.getMinecraft().setScreen(null);
        } else {
            this.renderBackground(gui);
            if (this.header != null) {
                gui.drawCenteredString(this.font, this.header, this.width / 2, 70, 16777215);
            }
            if (this.stage != null && this.progress > 0) {
                MutableComponent text = this.stage.copy().append(" " + this.progress + "%");
                gui.drawCenteredString(this.font, text, this.width / 2, 90, 16777215);
            }
            super.render(gui, mouseX, mouseY, partialTick);
        }
    }


    @Override
    public void progressStartNoAbort(Component component) {
        this.progressStart(component);
    }

    @Override
    public void progressStart(Component header) {
        this.header = Component.translatable("gui.tacz.client_gun_pack_downloader.downloading");
    }

    @Override
    public void progressStage(Component component) {
        this.stage = component;
        this.progressStagePercentage(0);
    }

    @Override
    public void progressStagePercentage(int progress) {
        this.progress = progress;
    }

    @Override
    public void stop() {
        this.stop = true;
    }
}
