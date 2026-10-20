package com.tacz.guns.client.gui;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz.guns.client.gui.components.refit.*;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.*;
import com.tacz.guns.resource.index.CommonAttachmentVariantIndex;
import com.tacz.guns.resource.index.CommonSlotAdapterIndex;
import com.tacz.guns.network.message.ClientMessageLaserColor;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.SlotAdapterHelper;
import com.tacz.guns.util.VariantHelper;
import com.tacz.guns.util.VirtualOemAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class GunRefitScreen extends Screen {
    public static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "textures/gui/refit_slot.png");
    public static final ResourceLocation TURN_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "textures/gui/refit_turn_page.png");
    public static final ResourceLocation UNLOAD_TEXTURE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "textures/gui/refit_unload.png");
    public static final ResourceLocation ICONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "textures/gui/refit_slot_icons.png");

    public static final int ICON_UV_SIZE = 32;
    public static final int SLOT_SIZE = 18;
    private static final int VARIANT_BUTTON_WIDTH = 78;
    private static final int VARIANT_BUTTON_HEIGHT = 12;
    private static final int VARIANT_BUTTON_SPACING = 1;
    private static final int INVENTORY_ATTACHMENT_START_Y = 50;
    private static final int LIST_COLUMN_GAP = 4;
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
                if (IGun.mainHandHoldGun(player)) {
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
    protected void renderBlurredBackground(float partialTick) { }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(graphics, mouseX, mouseY, pPartialTick);

        if (!HIDE_GUN_PROPERTY_DIAGRAMS) {
            GunPropertyDiagrams.draw(graphics, font, 11, 11);
        }

        this.renderables.stream().filter(w -> w instanceof IComponentTooltip).forEach(w -> ((IComponentTooltip) w)
                .renderTooltip(component -> graphics.renderComponentTooltip(font, component, mouseX, mouseY)));
        this.renderables.stream().filter(w -> w instanceof IStackTooltip).forEach(w -> ((IStackTooltip) w)
                .renderTooltip(stack -> renderAttachmentTooltip(graphics, stack, mouseX, mouseY)));
    }

    private void renderAttachmentTooltip(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        if (!isBuiltinOemAttachment(stack)) {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
            return;
        }
        List<Component> tooltip = getTooltipFromItem(getMinecraft(), stack);
        if (!tooltip.isEmpty()) {
            tooltip.set(0, tooltip.get(0).copy().append(Component.translatable("gui.tacz.refit.builtin_suffix")));
        }
        graphics.renderTooltip(font, tooltip, stack.getTooltipImage(), stack, mouseX, mouseY);
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
        int startY = INVENTORY_ATTACHMENT_START_Y;
        int pageStart = currentPage * INVENTORY_ATTACHMENT_SLOT_COUNT;
        int count = 0;
        int currentY = startY;
        Inventory inventory = player.getInventory();
        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        AttachmentType attachmentType = RefitTransform.getCurrentTransformType();
        Set<ResourceLocation> displayedAttachmentIds = new HashSet<>();
        List<RefitAttachmentEntry> attachmentEntries = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            IAttachment attachment = IAttachment.getIAttachmentOrNull(inventoryItem);
            if (attachment != null && attachment.getType(inventoryItem) == attachmentType) {
                if (!iGun.allowAttachment(gunItem, inventoryItem)) {
                    continue;
                }
                displayedAttachmentIds.add(attachment.getAttachmentId(inventoryItem));
                attachmentEntries.add(new RefitAttachmentEntry(i, inventoryItem, false));
            }
        }

        getVirtualOemAttachments(gunItem, iGun, attachmentType, displayedAttachmentIds)
                .forEach(attachment -> attachmentEntries.add(new RefitAttachmentEntry(-1, attachment, true)));
        // 仅显式标注的内置原厂配件置顶，通用配件保持原有顺序。
        attachmentEntries.sort(Comparator.comparingInt(entry -> isBuiltinOemAttachment(entry.stack()) ? 0 : 1));

        for (RefitAttachmentEntry entry : attachmentEntries) {
            count++;
            if (count <= pageStart) {
                continue;
            }
            if (count > pageStart + INVENTORY_ATTACHMENT_SLOT_COUNT) {
                continue;
            }
            InventoryAttachmentSlot button;
            if (entry.virtualOem()) {
                button = new InventoryAttachmentSlot(startX, currentY, entry.stack(), inventory, b -> {
                    ItemStack attachmentStack = ((InventoryAttachmentSlot) b).getItemStack();
                    IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentStack);
                    if (attachment == null) {
                        return;
                    }
                    SoundPlayManager.playerRefitSound(attachmentStack, player, SoundManager.INSTALL_SOUND);
                    ClientMessageInstallVirtualAttachment message = new ClientMessageInstallVirtualAttachment(inventory.selected, attachmentType, attachment.getAttachmentId(attachmentStack));
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
                });
            } else {
                button = new InventoryAttachmentSlot(startX, currentY, entry.slotIndex(), inventory, b -> {
                    int slotIndex = ((InventoryAttachmentSlot) b).getSlotIndex();
                    SoundPlayManager.playerRefitSound(inventory.getItem(slotIndex), player, SoundManager.INSTALL_SOUND);
                    ClientMessageRefitGun message = new ClientMessageRefitGun(slotIndex, inventory.selected, attachmentType);
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
                });
            }
            this.addRenderableWidget(button);
            currentY = currentY + SLOT_SIZE;
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
        Inventory inventory = player.getInventory();
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                if (RefitTransform.getCurrentTransformType() == AttachmentType.NONE) {
                    TimelessAPI.getGunDisplay(player.getMainHandItem())
                            .map(GunDisplayInstance::getLaserConfig)
                            .ifPresent(laserConfig -> {
                                if (laserConfig.canEdit()) {
                                    // 添加镭射颜色选择器
                                    HSVSliderGroup hsvSliderGroup = new HSVSliderGroup(width-140, height-64, 120, 16, inventory, inventory.selected, AttachmentType.NONE);
                                    this.addRenderableWidget(hsvSliderGroup.getHueSlider());
                                    this.addRenderableWidget(hsvSliderGroup.getSaturationSlider());
                                }});
                }
                continue;
            }
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
                int adapterListX = this.width - 30 - VARIANT_BUTTON_WIDTH - LIST_COLUMN_GAP;
                boolean hasSlotAdapters = !SlotAdapterHelper.getAvailableSlotAdapters(player.getMainHandItem(), type).isEmpty();
                int variantListX = hasSlotAdapters
                        ? adapterListX - VARIANT_BUTTON_WIDTH - LIST_COLUMN_GAP
                        : startX + SLOT_SIZE - VARIANT_BUTTON_WIDTH;
                // 添加拆卸配件按钮
                RefitUnloadButton unloadButton = new RefitUnloadButton(startX + 5, startY + SLOT_SIZE + 2, b -> {
                    ItemStack attachmentItem = button.getAttachmentItem();
                    if (!attachmentItem.isEmpty()) {
                        int freeSlot = inventory.getFreeSlot();
                        if (freeSlot != -1 || VirtualOemAttachment.isMarked(attachmentItem)) {
                            SoundPlayManager.playerRefitSound(attachmentItem, player, SoundManager.UNINSTALL_SOUND);
                            ClientMessageUnloadAttachment message = new ClientMessageUnloadAttachment(inventory.selected, RefitTransform.getCurrentTransformType());
                            net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tacz.gun_refit.unload.no_space"));
                        }
                    }
                });
                if (!button.getAttachmentItem().isEmpty()) {
                    this.addRenderableWidget(unloadButton);
                    addVariantButtons(type, button.getAttachmentItem(), inventory,
                            variantListX, startY + SLOT_SIZE + 14);

                    if (button.getAttachmentItem().getItem() instanceof IAttachment iAttachment) {
                        TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(button.getAttachmentItem()))
                                .map(ClientAttachmentIndex::getLaserConfig)
                                .ifPresent(laserConfig -> {
                                    if (laserConfig.canEdit()) {
                                        // 添加镭射颜色选择器
                                        HSVSliderGroup hsvSliderGroup = new HSVSliderGroup(width-140, height-64, 120, 16, inventory, inventory.selected, type);
                                        this.addRenderableWidget(hsvSliderGroup.getHueSlider());
                                        this.addRenderableWidget(hsvSliderGroup.getSaturationSlider());
                                    }});
                    }
                }
                addSlotAdapterButtons(type, button.getAttachmentItem(), inventory,
                        adapterListX, INVENTORY_ATTACHMENT_START_Y);
            }
            this.addRenderableWidget(button);
            startX = startX - SLOT_SIZE;
        }
    }

    private void addVariantButtons(AttachmentType type, ItemStack attachmentItem, Inventory inventory, int x, int y) {
        if (!(attachmentItem.getItem() instanceof IAttachment attachment)) {
            return;
        }
        ResourceLocation attachmentId = attachment.getAttachmentId(attachmentItem);
        List<Map.Entry<ResourceLocation, CommonAttachmentVariantIndex>> variants = TimelessAPI.getAllCommonAttachmentVariantIndex().stream()
                .filter(entry -> entry.getValue().appliesTo(attachmentId))
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getSort()))
                .toList();
        if (variants.isEmpty()) {
            return;
        }
        ResourceLocation current = VariantHelper.getVariant(attachmentItem);
        Component standardLabel = current == null
                ? Component.literal("> ").append(Component.translatable("gui.tacz.refit.variant_default"))
                : Component.translatable("gui.tacz.refit.variant_default");
        this.addRenderableWidget(new FlatColorButton(x, y, VARIANT_BUTTON_WIDTH, VARIANT_BUTTON_HEIGHT, standardLabel, b -> {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ClientMessageSetAttachmentVariant(inventory.selected, type, null));
            this.init();
        }));
        int currentY = y + VARIANT_BUTTON_HEIGHT + VARIANT_BUTTON_SPACING;
        for (Map.Entry<ResourceLocation, CommonAttachmentVariantIndex> entry : variants) {
            ResourceLocation variantId = entry.getKey();
            Component name = Component.translatable(entry.getValue().getPojo().getName());
            Component label = variantId.equals(current) ? Component.literal("> ").append(name) : name;
            this.addRenderableWidget(new FlatColorButton(x, currentY, VARIANT_BUTTON_WIDTH, VARIANT_BUTTON_HEIGHT, label, b -> {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ClientMessageSetAttachmentVariant(inventory.selected, type, variantId));
                this.init();
            }));
            currentY += VARIANT_BUTTON_HEIGHT + VARIANT_BUTTON_SPACING;
        }
    }

    private void addSlotAdapterButtons(AttachmentType type, ItemStack attachmentItem, Inventory inventory, int x, int y) {
        ItemStack gunItem = inventory.getItem(inventory.selected);
        List<Map.Entry<ResourceLocation, CommonSlotAdapterIndex>> adapters = SlotAdapterHelper.getAvailableSlotAdapters(gunItem, type);
        if (adapters.isEmpty()) {
            return;
        }

        ResourceLocation attachmentId = null;
        if (!attachmentItem.isEmpty()) {
            IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentItem);
            if (attachment == null || attachment.getType(attachmentItem) != type) {
                return;
            }
            attachmentId = attachment.getAttachmentId(attachmentItem);
        }

        ResourceLocation current = SlotAdapterHelper.getSelectedAvailableSlotAdapter(gunItem, type);
        ResourceLocation currentAttachmentId = attachmentId;
        int currentY = y;

        boolean noneSelected = current == null;
        Component noneLabel = noneSelected
                ? Component.literal("> ").append(Component.translatable("gui.tacz.refit.adapter_none"))
                : Component.translatable("gui.tacz.refit.adapter_none");
        FlatColorButton noneButton = new FlatColorButton(x, currentY, VARIANT_BUTTON_WIDTH, VARIANT_BUTTON_HEIGHT, noneLabel, b -> {
            currentPage = 0;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ClientMessageSetSlotAdapter(inventory.selected, type, null));
            this.init();
        });
        if (currentAttachmentId != null && !SlotAdapterHelper.allowsDirectAttachment(gunItem, currentAttachmentId)) {
            noneButton.active = false;
            noneButton.setTooltips(Component.translatable("gui.tacz.refit.adapter_requires_unload"));
        }
        this.addRenderableWidget(noneButton);
        currentY += VARIANT_BUTTON_HEIGHT + VARIANT_BUTTON_SPACING;

        for (Map.Entry<ResourceLocation, CommonSlotAdapterIndex> entry : adapters) {
            ResourceLocation adapterId = entry.getKey();
            Component name = Component.translatable(entry.getValue().getPojo().getName());
            Component label = adapterId.equals(current) ? Component.literal("> ").append(name) : name;
            FlatColorButton adapterButton = new FlatColorButton(x, currentY, VARIANT_BUTTON_WIDTH, VARIANT_BUTTON_HEIGHT, label, b -> {
                currentPage = 0;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ClientMessageSetSlotAdapter(inventory.selected, type, adapterId));
                this.init();
            });
            if (currentAttachmentId != null && !entry.getValue().allowsAttachment(currentAttachmentId)) {
                adapterButton.active = false;
                adapterButton.setTooltips(Component.translatable("gui.tacz.refit.adapter_requires_unload"));
            }
            this.addRenderableWidget(adapterButton);
            currentY += VARIANT_BUTTON_HEIGHT + VARIANT_BUTTON_SPACING;
        }
    }

    @Override
    public void onClose() {
        // 关闭界面时，一次性上传所有的染色数据
        LocalPlayer player = getMinecraft().player;
        if (player != null) {
            ItemStack gun = player.getMainHandItem();
            if (player.getMainHandItem().getItem() instanceof IGun) {
                ClientMessageLaserColor message = new ClientMessageLaserColor(gun, player.getInventory().selected);
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
            }
        }
        super.onClose();
    }

    private void switchHideButton() {
        HIDE_GUN_PROPERTY_DIAGRAMS = !HIDE_GUN_PROPERTY_DIAGRAMS;
        this.init();
    }

    private static List<ItemStack> getVirtualOemAttachments(ItemStack gunItem, IGun iGun, AttachmentType type, Set<ResourceLocation> excludedAttachmentIds) {
        ResourceLocation currentAttachmentId = iGun.getAttachmentId(gunItem, type);
        return TimelessAPI.getAllCommonAttachmentIndex().stream()
                .filter(entry -> entry.getValue().getType() == type)
                .filter(entry -> VirtualOemAttachment.isAllowedVirtualOem(iGun.getGunId(gunItem), entry.getKey(), type))
                .filter(entry -> VirtualOemAttachment.shouldShowIcon(iGun.getGunId(gunItem), entry.getKey(), type))
                .filter(entry -> !excludedAttachmentIds.contains(entry.getKey()))
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getSort()))
                .map(entry -> Pair.of(entry.getKey(), AttachmentItemBuilder.create().setId(entry.getKey()).build()))
                .filter(pair -> !pair.getKey().equals(currentAttachmentId))
                .filter(pair -> iGun.allowAttachment(gunItem, pair.getValue()))
                .map(Pair::getValue)
                .toList();
    }

    private static boolean isBuiltinOemAttachment(ItemStack attachmentStack) {
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (attachment == null) {
            return false;
        }
        return VirtualOemAttachment.isBuiltinAttachment(attachment.getAttachmentId(attachmentStack));
    }

    private record RefitAttachmentEntry(int slotIndex, ItemStack stack, boolean virtualOem) {
    }
}
