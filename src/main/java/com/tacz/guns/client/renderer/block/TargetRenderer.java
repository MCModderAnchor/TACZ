package com.tacz.guns.client.renderer.block;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class TargetRenderer implements BlockEntityRenderer<TargetBlockEntity> {
    private static final String UPPER_NAME = "target_upper";
    private static final String HEAD_NAME = "head";

    public TargetRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MODEL_LOCATION);
    }

    @Override
    public void render(TargetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        getModel().ifPresent(model -> {
            BlockState blockState = blockEntity.getBlockState();
            Direction facing = blockState.getValue(TargetBlock.FACING);
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart upperModel = model.getNode(UPPER_NAME);
            float deg = -Mth.lerp(partialTick, blockEntity.oRot, blockEntity.rot);
            upperModel.xRot = (float) Math.toRadians(deg);
            headModel.visible = false;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.225, 0.5);
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.get2DDataValue() * 90));
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(0, -1.275, 0.0125);
            RenderType renderType = RenderType.entityTranslucent(InternalAssetLoader.TARGET_TEXTURE_LOCATION);
            model.render(poseStack, ItemDisplayContext.NONE, renderType, combinedLightIn, combinedOverlayIn);
            if (blockEntity.getOwner() != null) {
                poseStack.translate(0, 1.25, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(deg));
                Minecraft minecraft = Minecraft.getInstance();
                var map = minecraft.getSkinManager().getInsecureSkinInformation(blockEntity.getOwner());
                ResourceLocation skin;
                if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    skin = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                } else {
                    skin = DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(blockEntity.getOwner()));
                }
                headModel.visible = true;
                RenderType skullRenderType = RenderType.entityTranslucentCull(skin);
                headModel.render(poseStack, ItemDisplayContext.NONE, bufferIn.getBuffer(skullRenderType), combinedLightIn, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        });
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(TargetBlockEntity blockEntity) {
        return true;
    }
}