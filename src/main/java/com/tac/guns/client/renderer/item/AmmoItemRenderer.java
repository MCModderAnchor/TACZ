package com.tac.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.client.model.SlotModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
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
import java.util.List;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.*;

public class AmmoItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotModel SLOT_AMMO_MODEL = new SlotModel();

    public AmmoItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack) {
        if (nodePath == null) {
            return;
        }
        // 应用定位组的反向位移、旋转，使定位组的位置就是渲染中心
        poseStack.translate(0, 1.5, 0);
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            BedrockPart t = nodePath.get(i);
            poseStack.mulPose(Vector3f.XN.rotation(t.xRot));
            poseStack.mulPose(Vector3f.YN.rotation(t.yRot));
            poseStack.mulPose(Vector3f.ZN.rotation(t.zRot));
            if (t.getParent() != null) {
                poseStack.translate(-t.x / 16.0F, -t.y / 16.0F, -t.z / 16.0F);
            } else {
                poseStack.translate(-t.x / 16.0F, (1.5F - t.y / 16.0F), -t.z / 16.0F);
            }
        }
        poseStack.translate(0, -1.5, 0);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (stack.getItem() instanceof IAmmo iAmmo) {
            ResourceLocation ammoId = iAmmo.getAmmoId(stack);
            poseStack.pushPose();
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.scale(-1, -1, 1);
            TimelessAPI.getClientAmmoIndex(ammoId).ifPresentOrElse(ammoIndex -> {
                BedrockAmmoModel ammoModel = ammoIndex.getAmmoModel();
                ResourceLocation modelTexture = ammoIndex.getModelTextureLocation();
                if (transformType == GUI) {
                    VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(ammoIndex.getSlotTextureLocation()));
                    SLOT_AMMO_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                } else if (ammoModel != null && modelTexture != null) {
                    // 再移动 0.5 格到中心
                    poseStack.translate(0, -0.5, 0);
                    if (transformType == FIXED) {
                        applyPositioningNodeTransform(ammoModel.getFixedOriginPath(), poseStack);
                    } else if (transformType == GROUND) {
                        applyPositioningNodeTransform(ammoModel.getGroundOriginPath(), poseStack);
                    } else if (transformType == THIRD_PERSON_LEFT_HAND || transformType == THIRD_PERSON_RIGHT_HAND) {
                        applyPositioningNodeTransform(ammoModel.getThirdPersonHandPath(), poseStack);
                    }
                    ammoModel.render(poseStack, transformType, RenderType.entityTranslucent(modelTexture), pPackedLight, pPackedOverlay);
                } else {
                    VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(ammoIndex.getSlotTextureLocation()));
                    SLOT_AMMO_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }, () -> {
                // 没有这个 ammoID，渲染个错误材质提醒别人
                VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation()));
                SLOT_AMMO_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            });
            poseStack.popPose();
        }
    }
}
