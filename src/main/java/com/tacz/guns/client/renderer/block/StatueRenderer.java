package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class StatueRenderer implements BlockEntityRenderer<StatueBlockEntity> {
    public StatueRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    @Override
    public void render(StatueBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        getModel().ifPresent(model -> {
            Level level = blockEntity.getLevel();
            if (level == null) {
                return;
            }

            poseStack.pushPose();
            {
                BlockState blockState = blockEntity.getBlockState();
                Direction facing = blockState.getValue(TargetBlock.FACING);

                poseStack.translate(0.5, 1.5, 0.5);

                poseStack.mulPose(Axis.YN.rotationDegrees((facing.get2DDataValue() + 2) % 4 * 90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));

                RenderType renderType = RenderType.entityTranslucent(getTextureLocation());
                model.render(poseStack, ItemDisplayContext.NONE, renderType, combinedLightIn, combinedOverlayIn);

                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.translate(0, -0.875, -1.2);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));

                double offset = Math.sin(Util.getMillis() / 500.0) * 0.1;
                poseStack.translate(0, offset, 0);

                ItemStack stack = blockEntity.getGunItem();

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,
                        LightTexture.pack(15, 15),
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        bufferIn,
                        level,
                        0
                );
            }
            poseStack.popPose();
        });
    }

    public static ResourceLocation getTextureLocation() {
        return InternalAssetLoader.STATUE_TEXTURE_LOCATION;
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(StatueBlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(StatueBlockEntity pBlockEntity, Vec3 pCameraPos) {
        return Vec3.atCenterOf(pBlockEntity.getBlockPos().above()).closerThan(pCameraPos, this.getViewDistance());
    }
}