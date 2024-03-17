package com.tac.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.model.SlotModel;
import com.tac.guns.client.resource.index.ClientAttachmentSkinIndex;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class AttachmentItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotModel SLOT_ATTACHMENT_MODEL = new SlotModel();

    public AttachmentItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (stack.getItem() instanceof IAttachment iAttachment) {
            ResourceLocation attachmentId = iAttachment.getAttachmentId(stack);
            poseStack.pushPose();
            if (transformType == ItemTransforms.TransformType.GUI) {
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
            } else {
                // 渲染物品时有 (-0.5, -0.5, -0.5)的位移，需要将其逆转
                poseStack.translate(0.5, 0.5, 0.5);
                if (transformType != ItemTransforms.TransformType.NONE) {
                    // 基岩版模型渲染原点位于 (0, 24, 0)
                    poseStack.translate(0, 1.5, 0);
                    // 反转模型
                    poseStack.scale(-1, -1, 1);
                    if (transformType == ItemTransforms.TransformType.FIXED) {
                        poseStack.mulPose(Vector3f.YN.rotationDegrees(90f));
                    }
                } else { // TransformType.NONE 用于在枪上渲染配件
                    // 基岩版模型渲染原点位于 (0, 24, 0)
                    poseStack.translate(0, -1.5f, 0);
                }
            }
            TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresentOrElse(attachmentIndex -> {
                if (transformType == ItemTransforms.TransformType.GUI) {
                    VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(attachmentIndex.getSlotTexture()));
                    SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    ResourceLocation skinId = iAttachment.getSkinId(stack);
                    ClientAttachmentSkinIndex skinIndex = attachmentIndex.getSkinIndex(skinId);
                    if (skinIndex != null) { // 有皮肤则渲染皮肤
                        BedrockAttachmentModel model = skinIndex.getModel();
                        ResourceLocation texture = skinIndex.getTexture();
                        model.render(poseStack, transformType, texture, pPackedLight, pPackedOverlay);
                    }else { // 没有皮肤，渲染默认模型
                        BedrockAttachmentModel model = attachmentIndex.getAttachmentModel();
                        ResourceLocation texture = attachmentIndex.getModelTexture();
                        model.render(poseStack, transformType, texture, pPackedLight, pPackedOverlay);
                    }
                }
            }, () -> {
                // 没有这个 attachmentId，渲染黑紫材质以提醒
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                SLOT_ATTACHMENT_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            });
            poseStack.popPose();
        }
    }
}
