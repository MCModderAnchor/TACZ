package com.tac.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.tac.guns.GunMod;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.client.gui.components.refit.RefitSlotButton;
import com.tac.guns.inventory.GunRefitMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends AbstractContainerScreen<GunRefitMenu> {
    private static int selectedSlot = -1;
    private static float refitScreenTransformProgress = 1;
    private static long refitScreenTransformTimestamp = -1;
    private static AttachmentType oldTransformType = AttachmentType.NONE;
    private static AttachmentType currentTransformType = AttachmentType.NONE;
    private static float refitScreenOpeningProgress = 0;
    private static long refitScreenOpeningTimestamp = -1;
    private static final float REFIT_SCREEN_TRANSFORM_TIMES = 0.25f;

    public GunRefitScreen(GunRefitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        selectedSlot = -1;
        refitScreenTransformProgress = 1;
        refitScreenTransformTimestamp = System.currentTimeMillis();
        oldTransformType = AttachmentType.NONE;
        currentTransformType = AttachmentType.NONE;
    }

    @Override
    protected void init() {
        this.imageWidth = width;
        this.imageHeight = height;
        super.init();
        this.clearWidgets();
        int i = 0;
        // 添加配件槽位
        for(AttachmentType type : AttachmentType.values()){
            if (type == AttachmentType.NONE) {
                continue;
            }
            addRenderableWidget(getRefitSlotButton(type, i++));
        }
    }

    @NotNull
    private RefitSlotButton getRefitSlotButton(AttachmentType type, int index) {
        RefitSlotButton button = new RefitSlotButton(leftPos + width - 18 * (index + 1), topPos + 8, ItemStack.EMPTY, type, b -> {
            AttachmentType transformType = getTransformTypeFromIndex(index);
            if (changeRefitScreenView(transformType)) {
                sendButtonClick(index);
                selectedSlot = index;
                init();
            }
        });
        if (selectedSlot == index) {
            button.setSelected(true);
        }
        return button;
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
        }
    }

    @SubscribeEvent
    public static void tickInterpolation(TickEvent.RenderTickEvent event){
        // tick opening progress
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
        // tick transform progress
        if (refitScreenTransformTimestamp == -1) {
            refitScreenTransformTimestamp = System.currentTimeMillis();
        }
        refitScreenTransformProgress += (System.currentTimeMillis() - refitScreenTransformTimestamp) / (REFIT_SCREEN_TRANSFORM_TIMES * 1000);
        if (refitScreenTransformProgress > 1) {
            refitScreenTransformProgress = 1;
        }
        refitScreenTransformTimestamp = System.currentTimeMillis();
    }

    public static float getOpeningProgress(){
        return refitScreenOpeningProgress;
    }

    @Nonnull
    public static AttachmentType getOldTransformType(){
        return Objects.requireNonNullElse(oldTransformType, AttachmentType.NONE);
    }

    @Nonnull
    public static AttachmentType getCurrentTransformType(){
        return Objects.requireNonNullElse(currentTransformType, AttachmentType.NONE);
    }

    public static float getTransformProgress(){
        return refitScreenTransformProgress;
    }

    private static AttachmentType getTransformTypeFromIndex(int index){
        if (index < AttachmentType.NONE.ordinal()) {
            return AttachmentType.values()[index];
        }
        return AttachmentType.values()[index + 1];
    }

    private static boolean changeRefitScreenView(AttachmentType attachmentType){
        if (refitScreenTransformProgress != 1 || refitScreenOpeningProgress != 1) {
            return false;
        }
        oldTransformType = currentTransformType;
        currentTransformType = attachmentType;
        refitScreenTransformProgress = 0;
        refitScreenTransformTimestamp = System.currentTimeMillis();
        return true;
    }
}
