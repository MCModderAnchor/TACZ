package com.tac.guns.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.SlotGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.pojo.display.TransformScale;
import com.tac.guns.item.GunItem;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.*;

/**
 * 负责第一人称以外的枪械模型渲染。第一人称渲染参见 {@link com.tac.guns.client.event.FirstPersonRenderGunEvent}
 */
public class TileEntityItemStackGunRenderer extends BlockEntityWithoutLevelRenderer {
    private static final SlotGunModel SLOT_GUN_MODEL = new SlotGunModel();

    public TileEntityItemStackGunRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (IGun.isGun(stack)) {
            // 第一人称就不渲染了，交给别的地方
            if (transformType == FIRST_PERSON_LEFT_HAND || transformType == FIRST_PERSON_RIGHT_HAND) {
                return;
            }

            // 剩下的渲染
            ResourceLocation gunId = GunItem.getData(stack).getGunId();
            ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                if (transformType == GUI) {
                    poseStack.pushPose();
                    poseStack.translate(0.5, 1.5, 0.5);
                    poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
                    VertexConsumer buffer = pBuffer.getBuffer(gunIndex.getSlotRenderType());
                    SLOT_GUN_MODEL.renderToBuffer(poseStack, buffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    poseStack.popPose();
                    return;
                }

                BedrockGunModel gunModel = gunIndex.getGunModel();
                poseStack.pushPose();
                // 移动到模型原点
                poseStack.translate(0.5, 2, 0.5);
                // 反转模型
                poseStack.scale(-1, -1, 1);
                // 应用定位组的变换（位移和旋转，不包括缩放）
                applyPositioningTransform(transformType, gunIndex.getTransform().getScale(), gunModel, poseStack);
                // 应用 display 数据中的缩放
                applyScaleTransform(transformType, gunIndex.getTransform().getScale(), poseStack);
                gunModel.render(0, transformType, stack, null, poseStack, pBuffer, pPackedLight, pPackedOverlay);
                poseStack.popPose();
            });
        }
    }

    private void applyPositioningTransform(ItemTransforms.TransformType transformType, TransformScale scale, BedrockGunModel model, PoseStack poseStack) {
        switch (transformType) {
            case FIXED -> {
                applyPositioningNodeTransform(model.getFixedOriginPath(), poseStack, scale.getFixed());
            }
            case GROUND -> {
                applyPositioningNodeTransform(model.getGroundOriginPath(), poseStack, scale.getGround());
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                applyPositioningNodeTransform(model.getThirdPersonHandOriginPath(), poseStack, scale.getThirdPerson());
            }
        }
    }

    private void applyScaleTransform(ItemTransforms.TransformType transformType, TransformScale scale, PoseStack poseStack) {
        if (scale == null) {
            return;
        }
        Vector3f vector3f = null;
        switch (transformType) {
            case FIXED -> {
                vector3f = scale.getFixed();
            }
            case GROUND -> {
                vector3f = scale.getGround();
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                vector3f = scale.getThirdPerson();
            }
        }
        if (vector3f != null) {
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(vector3f.x(), vector3f.y(), vector3f.z());
            poseStack.translate(0, -1.5, 0);
        }
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack, Vector3f scale) {
        if (nodePath == null) return;
        if (scale == null) scale = new Vector3f(1, 1, 1);
        //应用定位组的反向位移、旋转，使定位组的位置就是渲染中心
        poseStack.translate(0, 1.5, 0);
        for (int f = nodePath.size() - 1; f >= 0; f--) {
            BedrockPart t = nodePath.get(f);
            poseStack.mulPose(Vector3f.XN.rotation(t.xRot));
            poseStack.mulPose(Vector3f.YN.rotation(t.yRot));
            poseStack.mulPose(Vector3f.ZN.rotation(t.zRot));
            if (t.getParent() != null)
                poseStack.translate(-t.x * scale.x() / 16.0F, -t.y * scale.y() / 16.0F, -t.z * scale.z() / 16.0F);
            else {
                poseStack.translate(-t.x * scale.x() / 16.0F, (1.5F - t.y / 16.0F) * scale.y(), -t.z * scale.z() / 16.0F);
            }
        }
        poseStack.translate(0, -1.5, 0);
    }
}
