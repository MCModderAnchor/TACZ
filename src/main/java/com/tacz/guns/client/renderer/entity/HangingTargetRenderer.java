package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.entity.HangingTargetEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class HangingTargetRenderer extends EntityRenderer<HangingTargetEntity> {

    public HangingTargetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HangingTargetEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        Vec3 pivot = entity.getPivotPos();
        if (pivot == null) {
            pivot = entity.position().add(0, 1.5, 0);
        }

        // 帧平滑插值
        double lerpX = Mth.lerp((double)partialTicks, entity.xOld, entity.getX());
        double lerpY = Mth.lerp((double)partialTicks, entity.yOld, entity.getY());
        double lerpZ = Mth.lerp((double)partialTicks, entity.zOld, entity.getZ());
        Vec3 lerpPos = new Vec3(lerpX, lerpY, lerpZ);

        // ✨ 核心修正 2：只计算绕 X 轴（前后摆动）的角度，旋转轴彻底固定，再也不会乱转了
        Vec3 fromPivot = lerpPos.subtract(pivot);
        double degreesX = Math.toDegrees(Math.atan2(fromPivot.z, -fromPivot.y));

        poseStack.pushPose();

        // 1. 将渲染坐标系移到头顶的挂钩点
        poseStack.translate(0, 0.5, 0);//旋转轴的

        // 2. 绕着绝对固定的 X 轴旋转（前后摇摆）
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -degreesX));

        // 3. 移回实体原本的身体高度（向下 1.5 格）
        poseStack.translate(0, 0.3, 0);//这里吗，数字变小就是往下

        // -------------------------------------------------------------
        // ✨ 活板门扁平片改造部分：
        // 铁活板门默认是平躺在底部的。我们在这里把它绕 X 轴旋转 90 度，让它垂直垂下来。
        // 旋转后微调平移，使其在 X 轴和 Z 轴上完美居中对齐碰撞箱。
        // -------------------------------------------------------------
        poseStack.mulPose(Axis.XP.rotationDegrees(90F)); // 把它立起来垂直挂着
        poseStack.translate(-0.5D, 0.0D, -0.09375D); // X轴和厚度居中对齐
        // -------------------------------------------------------------

        // 4. 渲染铁活板门
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(
                Blocks.IRON_TRAPDOOR.defaultBlockState(), // ✨ 成功用铁活板门代替
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                net.minecraftforge.client.model.data.ModelData.EMPTY,
                null
        );

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HangingTargetEntity entity) {
        // 原版方块自带纹理，这里不影响
        return new ResourceLocation("minecraft", "textures/block/iron_trapdoor.png");
    }
}