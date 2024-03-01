package com.tac.guns.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.pojo.display.TransformScale;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import com.tac.guns.util.math.MathUtil;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class TileEntityItemStackGunRenderer extends BlockEntityWithoutLevelRenderer {
    public TileEntityItemStackGunRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    /**
     * 负责第一人称以外的枪械模型渲染。第一人称渲染参见 {@link com.tac.guns.client.event.FirstPersonRenderGunEvent}
     */
    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (stack.is(ModItems.GUN.get())) {
            ResourceLocation gunId = GunItem.getData(stack).getGunId();
            ClientGunIndex gunIndex = ClientGunLoader.getGunIndex(gunId);
            BedrockGunModel gunModel = gunIndex.getGunModel();
            poseStack.pushPose();
            // 移动到模型原点
            poseStack.translate(0.5, 2, 0.5);
            // 反转模型
            poseStack.scale(-1, -1, 1);
            // 应用定位组的变换（位移和旋转，不包括缩放）
            applyPositioningTransform(transformType, gunModel, poseStack);
            // 在应用定位组变换之后应用 display 数据中的缩放，以保证缩放的三轴与模型文件对应。
            applyScaleTransform(transformType, gunIndex.getTransform().getScale(), poseStack);
            gunModel.render(0, transformType, stack, null, poseStack, pBuffer, pPackedLight, pPackedOverlay);
            poseStack.popPose();
        }
    }

    private void applyPositioningTransform(ItemTransforms.TransformType transformType, BedrockGunModel model, PoseStack poseStack){
        switch (transformType){
            case FIXED -> {
                applyPositioningNodeTransform(model.getFixedOriginPath(), poseStack);
            }
            case GROUND -> {
                applyPositioningNodeTransform(model.getGroundOriginPath(), poseStack);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                applyPositioningNodeTransform(model.getThirdPersonHandOriginPath(), poseStack);
            }
        }
    }

    private void applyScaleTransform(ItemTransforms.TransformType transformType, TransformScale scale, PoseStack poseStack){
        if(scale == null){
            return;
        }
        Vector3f vector3f = null;
        switch (transformType){
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
        if(vector3f != null){
            poseStack.translate(0, 1.5, 0);
            poseStack.scale(vector3f.x(), vector3f.y(), vector3f.z());
            poseStack.translate(0, -1.5, 0);
        }
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack){
        if(nodePath == null) return;
        //应用定位组的反向位移、旋转，使定位组的位置就是渲染中心
        poseStack.translate(0, 1.5, 0);
        for (int f = nodePath.size() - 1; f >= 0; f--) {
            BedrockPart t = nodePath.get(f);
            float[] q = MathUtil.toQuaternion(-t.xRot, -t.yRot, -t.zRot);
            poseStack.mulPose(new Quaternion(q[0], q[1], q[2], q[3]));
            if (t.getParent() != null)
                poseStack.translate(-t.x / 16.0F, -t.y / 16.0F, -t.z / 16.0F);
            else {
                poseStack.translate(-t.x / 16.0F, 1.5F - t.y / 16.0F, -t.z / 16.0F);
            }
        }
        poseStack.translate(0, -1.5, 0);
    }
}
