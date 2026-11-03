package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.renderer.item.ItemPreviewRenderContext;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.util.RenderDistance;
import com.tacz.guns.util.RenderHelper;
import com.tacz.guns.util.SlotAdapterHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;

public class AttachmentRender implements IFunctionalRenderer {
    private final BedrockGunModel bedrockGunModel;
    private final AttachmentType type;

    public AttachmentRender(BedrockGunModel bedrockGunModel, AttachmentType type) {
        this.bedrockGunModel = bedrockGunModel;
        this.type = type;
    }

    /**
     * 旧入口在光影手部 pass 中选择 hand buffer；内部调用优先显式传递 bufferSource。
     */
    public static void renderAttachment(ItemStack attachmentItem, ItemStack gunItem, AttachmentType type, PoseStack poseStack, ItemDisplayContext transformType, int light, int overlay) {
        renderAttachment(attachmentItem, gunItem, type, poseStack, transformType, light, overlay, RenderHelper.getBufferSource(transformType));
    }

    public static void renderAttachment(ItemStack attachmentItem, ItemStack gunItem, AttachmentType type, PoseStack poseStack, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        if (attachmentItem.isEmpty()) {
            return;
        }
        poseStack.translate(0, -1.5, 0);
        if (attachmentItem.getItem() instanceof IAttachment iAttachment) {
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresentOrElse(attachmentIndex -> {
                BedrockAttachmentModel model = attachmentIndex.getAttachmentModel();
                ResourceLocation texture = attachmentIndex.getModelTexture();
                ClientAttachmentVariantIndex variant = TimelessAPI.getClientAttachmentVariantIndex(attachmentItem).orElse(null);
                if (variant != null && variant.isReplaceMode() && variant.getAttachmentModel() != null && variant.getModelTexture() != null) {
                    model = variant.getAttachmentModel();
                    texture = variant.getModelTexture();
                }
                ClientSlotAdapterIndex slotAdapter = getSlotAdapter(gunItem, type, attachmentId);
                if (slotAdapter != null) {
                    poseStack.pushPose();
                    renderSlotAdapter(slotAdapter, gunItem, poseStack, transformType, light, overlay, bufferSource);
                    poseStack.popPose();
                    applyMountOffset(poseStack, slotAdapter.getMountOffset());
                }
                if (variant != null && variant.isOverlayMode()) {
                    renderVariantOverlay(variant, attachmentItem, gunItem, poseStack, transformType, light, overlay, bufferSource);
                    applyMountOffset(poseStack, variant.getMountOffset());
                }
                // 这里是枪械里的配件渲染，没有模型材质就不渲染
                if (model != null && texture != null) {
                    // 调用低模
                    Pair<BedrockAttachmentModel, ResourceLocation> lodModel = attachmentIndex.getLodModel();
                    // 有低模、在高模渲染范围外、不是第一人称
                    if (lodModel != null && !RenderDistance.inRenderHighPolyModelDistance(poseStack) && !transformType.firstPerson()) {
                        model = lodModel.getLeft();
                        texture = lodModel.getRight();
                    }
                    float alpha = ItemPreviewRenderContext.alpha();
                    RenderType renderType = ItemPreviewRenderContext.isPreview() ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
                    model.render(attachmentItem, gunItem, poseStack, transformType, renderType, light, overlay, alpha, bufferSource);
                }
            }, () -> {
                // 没有对应的 attachmentIndex，渲染黑紫材质以提醒
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                AttachmentItemRenderer.SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, light, overlay, 1.0F, 1.0F, 1.0F, ItemPreviewRenderContext.alpha());
            });
        }
    }

    private static ClientSlotAdapterIndex getSlotAdapter(ItemStack gunItem, AttachmentType type, ResourceLocation attachmentId) {
        ResourceLocation adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(gunItem, type, attachmentId);
        return adapterId == null ? null : TimelessAPI.getClientSlotAdapterIndex(adapterId).orElse(null);
    }

    private static void renderSlotAdapter(ClientSlotAdapterIndex adapter, ItemStack gunItem, PoseStack poseStack, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        BedrockAttachmentModel model = adapter.getAdapterModel();
        ResourceLocation texture = adapter.getModelTexture();
        if (model == null || texture == null) {
            return;
        }
        float alpha = ItemPreviewRenderContext.alpha();
        RenderType renderType = ItemPreviewRenderContext.isPreview() ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
        model.render(ItemStack.EMPTY, gunItem, poseStack, transformType, renderType, light, overlay, alpha, bufferSource);
    }

    private static void applyMountOffset(PoseStack poseStack, Vector3f offset) {
        if (offset != null) {
            poseStack.translate(offset.x() / 16.0F, -offset.y() / 16.0F, offset.z() / 16.0F);
        }
    }

    private static void renderVariantOverlay(ClientAttachmentVariantIndex variant, ItemStack attachmentItem, ItemStack gunItem, PoseStack poseStack, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        BedrockAttachmentModel model = variant.getAttachmentModel();
        ResourceLocation texture = variant.getModelTexture();
        if (model == null || texture == null) {
            return;
        }
        float alpha = ItemPreviewRenderContext.alpha();
        RenderType renderType = ItemPreviewRenderContext.isPreview() ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
        model.render(attachmentItem, gunItem, poseStack, transformType, renderType, light, overlay, alpha, bufferSource);
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        render(poseStack, vertexBuffer, transformType, light, overlay, RenderHelper.getBufferSource(transformType));
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        EnumMap<AttachmentType, ItemStack> currentAttachmentItem = bedrockGunModel.getCurrentAttachmentItem();
        ItemStack attachmentItem = currentAttachmentItem.get(type);
        if (attachmentItem != null && !attachmentItem.isEmpty()) {
            Matrix3f normal = new Matrix3f(poseStack.last().normal());
            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
            bedrockGunModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                PoseStack poseStack2 = new PoseStack();
                poseStack2.last().normal().mul(normal);
                poseStack2.last().pose().mul(pose);
                // 渲染配件
                renderAttachment(attachmentItem, bedrockGunModel.getCurrentGunItem(), type, poseStack2, transformType, light, overlay, bufferSource);
            });
        }
    }
}
