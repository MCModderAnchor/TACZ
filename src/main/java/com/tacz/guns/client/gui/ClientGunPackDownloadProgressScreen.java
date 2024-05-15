package com.tacz.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.ProgressListener;

import javax.annotation.Nullable;

public class ClientGunPackDownloadProgressScreen extends Screen implements ProgressListener {
    private @Nullable Component header;
    private @Nullable Component stage;
    private int progress;
    private boolean stop;

    public ClientGunPackDownloadProgressScreen() {
        super(NarratorChatListener.NO_TITLE);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new Button((width - 200) / 2, 120, 200, 20,
                new TranslatableComponent("gui.tacz.client_gun_pack_downloader.background_download"), b -> this.stop()));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.stop) {
            this.getMinecraft().setScreen(null);
        } else {
            this.renderBackground(poseStack);
            if (this.header != null) {
                drawCenteredString(poseStack, this.font, this.header, this.width / 2, 70, 16777215);
            }
            if (this.stage != null && this.progress > 0) {
                MutableComponent text = this.stage.copy().append(" " + this.progress + "%");
                drawCenteredString(poseStack, this.font, text, this.width / 2, 90, 16777215);
            }
            super.render(poseStack, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void progressStartNoAbort(Component component) {
        this.progressStart(component);
    }

    @Override
    public void progressStart(Component header) {
        this.header = new TranslatableComponent("gui.tacz.client_gun_pack_downloader.downloading");
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
