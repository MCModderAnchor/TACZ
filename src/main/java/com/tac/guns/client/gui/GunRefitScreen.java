package com.tac.guns.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.client.gui.components.refit.RefitSlotButton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Objects;


@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends Screen {
    private static final float REFIT_SCREEN_TRANSFORM_TIMES = 0.25f;
    private static AttachmentType selectedType = AttachmentType.NONE;
    private static float refitScreenTransformProgress = 1;
    private static long refitScreenTransformTimestamp = -1;
    private static AttachmentType oldTransformType = AttachmentType.NONE;
    private static AttachmentType currentTransformType = AttachmentType.NONE;
    private static float refitScreenOpeningProgress = 0;
    private static long refitScreenOpeningTimestamp = -1;
    private final Int2ObjectMap<ItemStack> matchAttachments = new Int2ObjectOpenHashMap<>();

    public GunRefitScreen() {
        super(new TextComponent("Gun Refit Screen"));
        refitScreenTransformProgress = 1;
        refitScreenTransformTimestamp = System.currentTimeMillis();
        oldTransformType = AttachmentType.NONE;
        currentTransformType = AttachmentType.NONE;
    }

    @SubscribeEvent
    public static void tickInterpolation(TickEvent.RenderTickEvent event) {
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
            if (refitScreenOpeningProgress < 0) {
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

    public static float getOpeningProgress() {
        return refitScreenOpeningProgress;
    }

    @Nonnull
    public static AttachmentType getOldTransformType() {
        return Objects.requireNonNullElse(oldTransformType, AttachmentType.NONE);
    }

    @Nonnull
    public static AttachmentType getCurrentTransformType() {
        return Objects.requireNonNullElse(currentTransformType, AttachmentType.NONE);
    }

    public static float getTransformProgress() {
        return refitScreenTransformProgress;
    }

    private static AttachmentType getTransformTypeFromIndex(int index) {
        if (index < AttachmentType.NONE.ordinal()) {
            return AttachmentType.values()[index];
        }
        return AttachmentType.values()[index + 1];
    }

    private static boolean changeRefitScreenView(AttachmentType attachmentType) {
        if (refitScreenTransformProgress != 1 || refitScreenOpeningProgress != 1) {
            return false;
        }
        oldTransformType = currentTransformType;
        currentTransformType = attachmentType;
        refitScreenTransformProgress = 0;
        refitScreenTransformTimestamp = System.currentTimeMillis();
        return true;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.matchAttachments.clear();

        // 添加配件槽位
        int i = 0;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            addRenderableWidget(getRefitSlotButton(type, i++));
        }

        // 存入显示配件
        if (selectedType != AttachmentType.NONE && getMinecraft().player != null) {
            Inventory inventory = getMinecraft().player.getInventory();
            for (int j = 0; j < inventory.getContainerSize(); j++) {
                ItemStack inventoryItem = inventory.getItem(j);
                IAttachment attachment = IAttachment.getIAttachmentOrNull(inventoryItem);
                if (attachment != null && attachment.getType(inventoryItem) == selectedType) {
                    matchAttachments.put(j, inventoryItem);
                }
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int pMouseX, int pMouseY, float partialTick) {
        int i = 0;
        for (ItemStack itemStack : matchAttachments.values()) {
            int yOffset = 64 + 20 * i;
            itemRenderer.renderGuiItem(itemStack, this.width - 24, yOffset);
            i++;
        }
        super.render(poseStack, pMouseX, pMouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < 6; i++) {
                if (i >= matchAttachments.size()) {
                    break;
                }
                int xOffset = this.width - 24;
                int yOffset = 64 + 20 * i;
                int xEnd = xOffset + 16;
                int yEnd = yOffset + 16;
                boolean xInRange = xOffset <= mouseX && mouseX <= xEnd;
                boolean yInRange = yOffset <= mouseY && mouseY <= yEnd;
                if (xInRange && yInRange) {
                    // TODO：点击事件，发包到服务端
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @NotNull
    private RefitSlotButton getRefitSlotButton(AttachmentType type, int index) {
        RefitSlotButton button = new RefitSlotButton(width - 18 * (index + 1), 8, ItemStack.EMPTY, type, b -> {
            AttachmentType transformType = getTransformTypeFromIndex(index);
            if (changeRefitScreenView(transformType)) {
                selectedType = transformType;
                init();
            }
        });
        if (selectedType == type) {
            button.setSelected(true);
        }
        return button;
    }
}
