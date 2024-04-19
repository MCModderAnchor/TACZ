package com.tac.guns.client.renderer.block;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.block.TargetBlock;
import com.tac.guns.block.entity.TargetBlockEntity;
import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TargetRenderer implements BlockEntityRenderer<TargetBlockEntity> {
    private static final ResourceLocation MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/block/target_geo.json");
    private static final ResourceLocation HEAD_MODEL_LOCATION = new ResourceLocation(GunMod.MOD_ID, "models/block/target_overlay_geo.json");
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(GunMod.MOD_ID, "textures/block/target.png");
    private static @Nullable BedrockModel MODEL;
    private static @Nullable BedrockModel HEAD_MODEL;

    public TargetRenderer(BlockEntityRendererProvider.Context context) {
        if (MODEL == null) {
            try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION).getInputStream()) {
                BedrockModelPOJO pojo = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                MODEL = new BedrockModel(pojo, BedrockVersion.NEW);
            } catch (IOException ioException) {
                ioException.fillInStackTrace();
            }
        }
        if (HEAD_MODEL == null) {
            try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(HEAD_MODEL_LOCATION).getInputStream()) {
                BedrockModelPOJO pojo = ClientGunPackLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
                HEAD_MODEL = new BedrockModel(pojo, BedrockVersion.NEW);
            } catch (IOException ioException) {
                ioException.fillInStackTrace();
            }
        }
    }

    @Nullable
    public static BedrockModel getModel() {
        return MODEL;
    }

    public static ResourceLocation getTextureLocation() {
        return TEXTURE_LOCATION;
    }

    @Override
    public void render(TargetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (MODEL == null) {
            return;
        }
        BlockState blockState = blockEntity.getBlockState();


        Direction facing = blockState.getValue(TargetBlock.FACING);
        poseStack.pushPose();
        {

            poseStack.translate(0.5, 0.225, 0.5);
            poseStack.mulPose(Vector3f.YN.rotationDegrees(facing.get2DDataValue() * 90));
            poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
            poseStack.mulPose(Vector3f.XN.rotationDegrees(
                    Mth.lerp(partialTick, blockEntity.oRot, blockEntity.rot))
            );
            poseStack.translate(0, -1.275, 0.0125);

            RenderType renderType = RenderType.entityTranslucent(TEXTURE_LOCATION);
            MODEL.render(poseStack, ItemTransforms.TransformType.NONE, renderType, combinedLightIn, combinedOverlayIn);

            if(blockEntity.getOwner()!=null){
                Minecraft minecraft = Minecraft.getInstance();

                var map = minecraft.getSkinManager().getInsecureSkinInformation(blockEntity.getOwner());
                ResourceLocation rl;
                if(map.containsKey(MinecraftProfileTexture.Type.SKIN)){
                    rl = minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                }else {
                    rl = DefaultPlayerSkin.getDefaultSkin(Player.createPlayerUUID(blockEntity.getOwner()));
                }

                RenderType renderType2 = RenderType.entityTranslucent(rl);
                if (HEAD_MODEL != null) {
                    HEAD_MODEL.render(poseStack, ItemTransforms.TransformType.NONE, renderType2, combinedLightIn, combinedOverlayIn);
                }
            }

        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TargetBlockEntity blockEntity) {
        return true;
    }
}