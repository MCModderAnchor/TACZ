package com.tacz.guns.client.tooltip;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.RecoilModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ClientAttachmentItemTooltip implements ClientTooltipComponent {
    private static final Cache<ResourceLocation, List<ItemStack>> CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build();
    private final ResourceLocation attachmentId;
    private final List<Component> components = Lists.newArrayList();
    private final MutableComponent tips = new TranslatableComponent("tooltip.tacz.attachment.yaw.shift");
    private final MutableComponent support = new TranslatableComponent("tooltip.tacz.attachment.yaw.support");
    private @Nullable MutableComponent packInfo;
    private List<ItemStack> showGuns = Lists.newArrayList();

    public ClientAttachmentItemTooltip(AttachmentItemTooltip tooltip) {
        this.attachmentId = tooltip.getAttachmentId();
        this.addText(tooltip.getType());
        this.getShowGuns();
        this.addPackInfo();
    }

    private void addPackInfo() {
        PackInfo packInfoObject = ClientAssetManager.INSTANCE.getPackInfo(attachmentId);
        if (packInfoObject != null) {
            packInfo = new TranslatableComponent(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
        }
    }

    private static List<ItemStack> getAllAllowGuns(List<ItemStack> output, ResourceLocation attachmentId) {
        ItemStack attachment = AttachmentItemBuilder.create().setId(attachmentId).build();
        TimelessAPI.getAllCommonGunIndex().forEach(entry -> {
            ResourceLocation gunId = entry.getKey();
            ItemStack gun = GunItemBuilder.create().setId(gunId).build();
            if (!(gun.getItem() instanceof IGun iGun)) {
                return;
            }
            if (iGun.allowAttachment(gun, attachment)) {
                output.add(gun);
            }
        });
        return output;
    }

    @Override
    public int getHeight() {
        if (!Screen.hasShiftDown()) {
            return components.size() * 10 + 28;
        }
        return (showGuns.size() - 1) / 16 * 18 + 50 + components.size() * 10;
    }

    @Override
    public int getWidth(Font font) {
        int[] width = new int[]{0};
        if (packInfo != null) {
            width[0] = Math.max(width[0], font.width(packInfo) + 4);
        }
        components.forEach(c -> width[0] = Math.max(width[0], font.width(c)));
        if (!Screen.hasShiftDown()) {
            return Math.max(width[0], font.width(tips) + 4);
        } else {
            width[0] = Math.max(width[0], font.width(support) + 4);
        }
        if (showGuns.size() > 15) {
            return Math.max(width[0], 260);
        }
        return Math.max(width[0], showGuns.size() * 16 + 4);
    }

    @Override
    public void renderText(Font font, int pX, int pY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource) {
        int yOffset = pY;
        for (Component component : this.components) {
            font.drawInBatch(component, pX, yOffset, 0xffaa00, false, matrix4f, bufferSource, false, 0, 0xF000F0);
            yOffset += 10;
        }
        if (!Screen.hasShiftDown()) {
            font.drawInBatch(tips, pX, pY + 5 + this.components.size() * 10, 0x9e9e9e, false, matrix4f, bufferSource, false, 0, 0xF000F0);
            yOffset += 10;
        } else {
            yOffset += (showGuns.size() - 1) / 16 * 18 + 32;
        }
        // 枪包名
        if (packInfo != null) {
            font.drawInBatch(this.packInfo, pX, yOffset + 8, 0xffffff, false, matrix4f, bufferSource, false, 0, 0xF000F0);
        }
    }

    @Override
    public void renderImage(Font font, int mouseX, int mouseY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
        if (!Screen.hasShiftDown()) {
            return;
        }
        int minY = components.size() * 10 + 3;
        int maxX = getWidth(font);
        Screen.fill(poseStack, mouseX, mouseY + minY, mouseX + maxX, mouseY + minY + 11, 0x8F00b0ff);
        poseStack.pushPose();
        poseStack.translate(0, 0, blitOffset);
        font.draw(poseStack, support, mouseX + 2, mouseY + minY + 2, 0xe3f2fd);
        poseStack.popPose();
        for (int i = 0; i < showGuns.size(); i++) {
            ItemStack stack = showGuns.get(i);
            int x = i % 16 * 16 + 2;
            int y = i / 16 * 18 + minY + 15;
            itemRenderer.renderGuiItem(stack, mouseX + x, mouseY + y);
        }
    }

    private void getShowGuns() {
        try {
            this.showGuns = CACHE.get(attachmentId, () -> getAllAllowGuns(Lists.newArrayList(), attachmentId));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void addText(AttachmentType type) {
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            AttachmentData data = index.getData();
            if (type == AttachmentType.SCOPE) {
                float[] zoom = index.getZoom();
                if (zoom != null) {
                    String[] zoomText = new String[zoom.length];
                    for (int i = 0; i < zoom.length; i++) {
                        zoomText[i] = "x" + zoom[i];
                    }
                    String zoomJoinText = StringUtils.join(zoomText, ", ");
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.zoom", zoomJoinText).withStyle(ChatFormatting.GOLD));
                }
            }

            if (type == AttachmentType.EXTENDED_MAG) {
                int magLevel = data.getExtendedMagLevel();
                if (magLevel == 1) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_1").withStyle(ChatFormatting.GRAY));
                } else if (magLevel == 2) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_2").withStyle(ChatFormatting.BLUE));
                } else if (magLevel == 3) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_3").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }

            float adsAddendTime = data.getAdsAddendTime();
            if (adsAddendTime > 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.ads.increase").withStyle(ChatFormatting.RED));
            } else if (adsAddendTime < 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.ads.decrease").withStyle(ChatFormatting.GREEN));
            }

            float inaccuracyAddend = data.getInaccuracyAddend();
            if (inaccuracyAddend > 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.inaccuracy.increase").withStyle(ChatFormatting.RED));
            } else if (inaccuracyAddend < 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.inaccuracy.decrease").withStyle(ChatFormatting.GREEN));
            }

            RecoilModifier recoilModifier = data.getRecoilModifier();
            if (recoilModifier != null) {
                float pitch = recoilModifier.getPitch();
                if (pitch > 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.pitch.increase").withStyle(ChatFormatting.RED));
                } else if (pitch < 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.pitch.decrease").withStyle(ChatFormatting.GREEN));
                }

                float yaw = recoilModifier.getYaw();
                if (yaw > 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.yaw.increase").withStyle(ChatFormatting.RED));
                } else if (yaw < 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.yaw.decrease").withStyle(ChatFormatting.GREEN));
                }
            }
        });
    }
}
