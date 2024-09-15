package com.tacz.guns.client.gui;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz.guns.client.gui.components.refit.*;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends Screen {
    public static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_slot.png");
    public static final ResourceLocation TURN_PAGE_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_turn_page.png");
    public static final ResourceLocation UNLOAD_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_unload.png");
    public static final ResourceLocation ICONS_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/refit_slot_icons.png");

    public static final int ICON_UV_SIZE = 32;
    public static final int SLOT_SIZE = 18;
    private static final int INVENTORY_ATTACHMENT_SLOT_COUNT = 8;
    private static boolean HIDE_GUN_PROPERTY_DIAGRAMS = true;

    private int currentPage = 0;

    public GunRefitScreen() {
        super(Component.literal("Gun Refit Screen"));
        RefitTransform.init();
    }

    public static int getSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return -1;
        }
        if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return ICON_UV_SIZE * 6;
        }
        switch (attachmentType) {
            case GRIP -> {
                return 0;
            }
            case LASER -> {
                return ICON_UV_SIZE;
            }
            case MUZZLE -> {
                return ICON_UV_SIZE * 2;
            }
            case SCOPE -> {
                return ICON_UV_SIZE * 3;
            }
            case STOCK -> {
                return ICON_UV_SIZE * 4;
            }
            case EXTENDED_MAG -> {
                return ICON_UV_SIZE * 5;
            }
        }
        return -1;
    }

    public static int getSlotsTextureWidth() {
        return ICON_UV_SIZE * 7;
    }

    @Override
    public void init() {
        this.clearWidgets();
        // 添加配件槽位
        this.addAttachmentTypeButtons();
        // 添加可选配件列表
        this.addInventoryAttachmentButtons();
        // 添加属性图隐藏按钮
        if (HIDE_GUN_PROPERTY_DIAGRAMS) {
            this.addRenderableWidget(new FlatColorButton(11, 11, 288, 16,
                    Component.translatable("gui.tacz.gun_refit.property_diagrams.show"), b -> switchHideButton()));
        } else {
            this.addRenderableWidget(new FlatColorButton(14, 14, 12, 12, Component.literal("S"), b -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || player.isSpectator()) return;
                if (IGun.mainhandHoldGun(player)) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).fireSelect();
                    this.init();
                }
            }).setTooltips(Component.translatable("gui.tacz.gun_refit.property_diagrams.fire_mode.switch")));
            int buttonYOffset = GunPropertyDiagrams.getHidePropertyButtonYOffset();
            this.addRenderableWidget(new FlatColorButton(11, buttonYOffset, 288, 12,
                    Component.translatable("gui.tacz.gun_refit.property_diagrams.hide"), b -> switchHideButton()));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(graphics, mouseX, mouseY, pPartialTick);

        if (!HIDE_GUN_PROPERTY_DIAGRAMS) {
            GunPropertyDiagrams.draw(graphics, font, 11, 11);
        }

        this.renderables.stream().filter(w -> w instanceof IComponentTooltip).forEach(w -> ((IComponentTooltip) w)
                .renderTooltip(component -> graphics.renderComponentTooltip(font, component, mouseX, mouseY)));
        this.renderables.stream().filter(w -> w instanceof IStackTooltip).forEach(w -> ((IStackTooltip) w)
                .renderTooltip(stack -> graphics.renderTooltip(font, stack, mouseX, mouseY)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addInventoryAttachmentButtons() {
        LocalPlayer player = getMinecraft().player;
        if (RefitTransform.getCurrentTransformType() == AttachmentType.NONE || player == null) {
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
            if (attachment != null && iGun != null && attachment.getType(inventoryItem) == RefitTransform.getCurrentTransformType()) {
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
                    int slotIndex = ((InventoryAttachmentSlot) b).getSlotIndex();
                    SoundPlayManager.playerRefitSound(inventory.getItem(slotIndex), player, SoundManager.INSTALL_SOUND);
                    ClientMessageRefitGun message = new ClientMessageRefitGun(slotIndex, inventory.selected, RefitTransform.getCurrentTransformType());
                    NetworkHandler.CHANNEL.sendToServer(message);
                });
                this.addRenderableWidget(button);
                currentY = currentY + SLOT_SIZE;
            }
        }
        int totalPage = (count - 1) / INVENTORY_ATTACHMENT_SLOT_COUNT;
        RefitTurnPageButton turnPageButtonUp = new RefitTurnPageButton(startX, startY - 10, true, b -> {
            if (currentPage > 0) {
                currentPage--;
                init();
            }
        });
        RefitTurnPageButton turnPageButtonDown = new RefitTurnPageButton(startX, startY + SLOT_SIZE * INVENTORY_ATTACHMENT_SLOT_COUNT + 2, false, b -> {
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
                    if (RefitTransform.changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 点击的是当前选中的槽位，则退回概览
                if (RefitTransform.getCurrentTransformType() == buttonType && buttonType != AttachmentType.NONE) {
                    if (RefitTransform.changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 切换选中的槽位。
                if (RefitTransform.changeRefitScreenView(buttonType)) {
                    this.init();
                }
            });
            if (RefitTransform.getCurrentTransformType() == type) {
                button.setSelected(true);
                // 添加拆卸配件按钮
                RefitUnloadButton unloadButton = new RefitUnloadButton(startX + 5, startY + SLOT_SIZE + 2, b -> {
                    ItemStack attachmentItem = button.getAttachmentItem();
                    if (!attachmentItem.isEmpty()) {
                        int freeSlot = inventory.getFreeSlot();
                        if (freeSlot != -1) {
                            SoundPlayManager.playerRefitSound(attachmentItem, player, SoundManager.UNINSTALL_SOUND);
                            ClientMessageUnloadAttachment message = new ClientMessageUnloadAttachment(inventory.selected, RefitTransform.getCurrentTransformType());
                            NetworkHandler.CHANNEL.sendToServer(message);
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tacz.gun_refit.unload.no_space"));
                        }
                    }
                });
                if (!button.getAttachmentItem().isEmpty()) {
                    this.addRenderableWidget(unloadButton);
                }
            }
            this.addRenderableWidget(button);
            startX = startX - SLOT_SIZE;
        }
    }

    private void switchHideButton() {
        HIDE_GUN_PROPERTY_DIAGRAMS = !HIDE_GUN_PROPERTY_DIAGRAMS;
        this.init();
    }
}
