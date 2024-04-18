package com.tac.guns.client.gui;

import com.tac.guns.GunMod;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.gui.components.refit.GunAttachmentSlot;
import com.tac.guns.client.gui.components.refit.InventoryAttachmentSlot;
import com.tac.guns.client.gui.components.refit.RefitTurnPageButton;
import com.tac.guns.client.gui.components.refit.RefitUnloadButton;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessageRefitGun;
import com.tac.guns.network.message.ClientMessageUnloadAttachment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.Objects;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends Screen {
    public static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_slot.png");
    public static final ResourceLocation TURN_PAGE_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_turn_page.png");
    public static final ResourceLocation UNLOAD_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_unload.png");
    public static final ResourceLocation ICONS_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_slot_icons.png");
    public static final int ICON_SIZE = 32;
    private static final int INVENTORY_ATTACHMENT_SLOT_COUNT = 8;
    private int currentPage = 0;
    // 以下参数、变量用于改装窗口动画插值
    private static final float REFIT_SCREEN_TRANSFORM_TIMES = 0.25f;
    private static float refitScreenTransformProgress = 1;
    private static long refitScreenTransformTimestamp = -1;
    private static AttachmentType oldTransformType = AttachmentType.NONE;
    private static AttachmentType currentTransformType = AttachmentType.NONE;
    private static float refitScreenOpeningProgress = 0;
    private static long refitScreenOpeningTimestamp = -1;

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

    public static int getSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return -1;
        }
        if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return ICON_SIZE * 6;
        }
        switch (attachmentType) {
            case GRIP -> {
                return 0;
            }
            case LASER -> {
                return ICON_SIZE;
            }
            case MUZZLE -> {
                return ICON_SIZE * 2;
            }
            case SCOPE -> {
                return ICON_SIZE * 3;
            }
            case STOCK -> {
                return ICON_SIZE * 4;
            }
            case EXTENDED_MAG -> {
                return ICON_SIZE * 5;
            }
        }
        return -1;
    }

    public static int getSlotsTextureWidth() {
        return ICON_SIZE * 7;
    }

    @Override
    public void init() {
        super.init();
        this.clearWidgets();

        // 添加配件槽位
        this.addAttachmentTypeButtons();
        // 添加可选配件列表
        this.addInventoryAttachmentButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addInventoryAttachmentButtons() {
        LocalPlayer player = getMinecraft().player;
        if (currentTransformType == AttachmentType.NONE || player == null) {
            return;
        }
        int startX = this.width - 30;
        int startY = 50;
        int pageStart = currentPage * INVENTORY_ATTACHMENT_SLOT_COUNT;
        int count = 0;
        int currentY = startY;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            IAttachment attachment = IAttachment.getIAttachmentOrNull(inventoryItem);
            IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
            if (attachment != null && iGun != null && attachment.getType(inventoryItem) == currentTransformType) {
                if (!iGun.allowAttachment(player.getMainHandItem(), inventoryItem)) {
                    continue;
                }
                count++;
                if (count <= pageStart) {
                    continue;
                }
                if (count > pageStart + INVENTORY_ATTACHMENT_SLOT_COUNT) {
                    continue;
                }
                InventoryAttachmentSlot button = new InventoryAttachmentSlot(startX, currentY, i, inventory, b -> {
                    ClientMessageRefitGun message = new ClientMessageRefitGun(((InventoryAttachmentSlot) b).getSlotIndex(), inventory.selected, currentTransformType);
                    NetworkHandler.CHANNEL.sendToServer(message);
                });
                this.addRenderableWidget(button);
                currentY = currentY + 18;
            }
        }
        int totalPage = (count - 1) / INVENTORY_ATTACHMENT_SLOT_COUNT;
        RefitTurnPageButton turnPageButtonUp = new RefitTurnPageButton(startX, startY - 10, true, b -> {
            if (currentPage > 0) {
                currentPage--;
                init();
            }
        });
        RefitTurnPageButton turnPageButtonDown = new RefitTurnPageButton(startX, startY + 18 * INVENTORY_ATTACHMENT_SLOT_COUNT + 2, false, b -> {
            if (currentPage < totalPage) {
                currentPage++;
                init();
            }
        });
        if (currentPage < totalPage) {
            this.addRenderableWidget(turnPageButtonDown);
        }
        if (currentPage > 0) {
            this.addRenderableWidget(turnPageButtonUp);
        }
    }

    private void addAttachmentTypeButtons() {
        LocalPlayer player = getMinecraft().player;
        if (player == null) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
        if (iGun == null) {
            return;
        }
        int startX = this.width - 30;
        int startY = 10;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            Inventory inventory = player.getInventory();
            GunAttachmentSlot button = new GunAttachmentSlot(startX, startY, type, inventory.selected, inventory, b -> {
                AttachmentType buttonType = ((GunAttachmentSlot) b).getType();
                // 如果这个槽位不允许安装配件，则默认退回概览，不选中槽位。
                if (!((GunAttachmentSlot) b).isAllow()) {
                    if (changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 点击的是当前选中的槽位，则退回概览
                if (currentTransformType == buttonType && buttonType != AttachmentType.NONE) {
                    if (changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 切换选中的槽位。
                if (changeRefitScreenView(buttonType)) {
                    this.init();
                }
            });
            if (currentTransformType == type) {
                button.setSelected(true);
                // 添加拆卸配件按钮
                RefitUnloadButton unloadButton = new RefitUnloadButton(startX + 5, startY + 18 + 2, b -> {
                    ItemStack attachmentItem = button.getAttachmentItem();
                    if (!attachmentItem.isEmpty()) {
                        int freeSlot = inventory.getFreeSlot();
                        if (freeSlot != -1) {
                            ClientMessageUnloadAttachment message = new ClientMessageUnloadAttachment(inventory.selected, currentTransformType);
                            NetworkHandler.CHANNEL.sendToServer(message);
                        } else {
                            player.sendMessage(new TranslatableComponent("gui.tac.gun_refit.unload.no_space"), Util.NIL_UUID);
                        }
                    }
                });
                if (!button.getAttachmentItem().isEmpty()) {
                    this.addRenderableWidget(unloadButton);
                }
            }
            this.addRenderableWidget(button);
            startX = startX - 18;
        }
    }
}
