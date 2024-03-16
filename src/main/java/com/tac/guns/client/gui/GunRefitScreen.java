package com.tac.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.client.gui.components.refit.RefitSlotButton;
import com.tac.guns.inventory.GunRefitMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends AbstractContainerScreen<GunRefitMenu> {
    private int selectedSlot = -1;
    private static float refitScreenTransformProgress = 1;
    private static long refitScreenTransformTimestamp = -1;
    private static AttachmentType oldTransformType = AttachmentType.NONE;
    private static AttachmentType currentTransformType = AttachmentType.NONE;
    private static float refitScreenOpeningProgress = 0;
    private static long refitScreenOpeningTimestamp = -1;
    private static final float REFIT_SCREEN_TRANSFORM_TIMES = 0.25f;

    public GunRefitScreen(GunRefitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        refitScreenTransformTimestamp = System.currentTimeMillis();
        refitScreenTransformProgress = 1;
    }

    @Override
    protected void init() {
        this.imageWidth = width;
        this.imageHeight = height;
        super.init();
        this.clearWidgets();
        // 改装窗口完全打开才渲染组件
        if (refitScreenOpeningProgress != 1) {
            return;
        }
        int rightMargin = 8;
        int i = 0;
        for(AttachmentType type : AttachmentType.values()){
            if (type == AttachmentType.NONE) {
                continue;
            }
            rightMargin += 18;
            int index = i;
            i++;
            addRenderableWidget(new RefitSlotButton(leftPos + width - rightMargin, topPos + 8, ItemStack.EMPTY, type, b -> {
                sendButtonClick(index);
            }));
        }
    }

    @Override
    protected void renderLabels(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(@Nonnull PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    }

    private void sendButtonClick(int buttonId) {
        MultiPlayerGameMode gameMode = this.getMinecraft().gameMode;
        if (gameMode != null) {
            gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            selectedSlot = buttonId;
        }
    }

    @SubscribeEvent
    public static void tickInterpolation(TickEvent.RenderTickEvent event){
        // tick refit screen opening progress
        if (refitScreenOpeningTimestamp == -1) {
            refitScreenOpeningTimestamp = System.currentTimeMillis();
        }
        if (Minecraft.getInstance().screen instanceof GunRefitScreen) {
            refitScreenOpeningProgress += (System.currentTimeMillis() - refitScreenOpeningTimestamp) / (REFIT_SCREEN_TRANSFORM_TIMES * 1000);
            if (refitScreenOpeningProgress > 1) {
                refitScreenOpeningProgress = 1;
            }
        } else {
            refitScreenOpeningProgress -= (System.currentTimeMillis() - refitScreenOpeningTimestamp) / (REFIT_SCREEN_TRANSFORM_TIMES * 1000);
            if (refitScreenOpeningProgress < 0){
                refitScreenOpeningProgress = 0;
            }
        }
        refitScreenOpeningTimestamp = System.currentTimeMillis();
    }

    public static float getRefitScreenOpeningProgress(){
        return refitScreenOpeningProgress;
    }
}
