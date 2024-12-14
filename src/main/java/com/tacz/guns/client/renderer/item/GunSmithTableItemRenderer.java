package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GunSmithTableItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotModel SLOT_BLOCK_MODEL = new SlotModel();
    public GunSmithTableItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        GunSmithTableRenderer.getIndex(stack).ifPresentOrElse(index -> {
            BedrockModel model = index.getModel();
            ResourceLocation texture = index.getTexture();
            if (model == null) {
                return;
            }
            poseStack.pushPose();

            ItemTransforms transforms = index.getTransforms();
            if (transforms != null) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                ItemTransform transform = transforms.getTransform(transformType);
                transform.apply(false, poseStack);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }

            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
            RenderType renderType = RenderType.entityTranslucent(texture);
            model.render(poseStack, transformType, renderType, pPackedLight, pPackedOverlay);
            poseStack.popPose();
        }, ()->{
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
            VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
            SLOT_BLOCK_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1, 1, 1, 1);
        });
    }
}
