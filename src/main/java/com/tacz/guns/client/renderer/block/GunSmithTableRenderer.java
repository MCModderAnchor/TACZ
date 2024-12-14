package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tacz.guns.block.GunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class GunSmithTableRenderer implements BlockEntityRenderer<GunSmithTableBlockEntity> {
    public GunSmithTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    public Optional<ClientBlockIndex> getIndex(GunSmithTableBlockEntity blockEntity) {
        ResourceLocation id = blockEntity.getId();
        if (id==null || id.equals(DefaultAssets.EMPTY_BLOCK_ID)) {
            return Optional.empty();
        }
        return TimelessAPI.getClientBlockIndex(id);
    }

    public static Optional<ClientBlockIndex> getIndex(ItemStack stack) {
        if (stack.getItem() instanceof IBlock iBlock) {
            ResourceLocation id = iBlock.getBlockId(stack);
            if (id.equals(DefaultAssets.EMPTY_BLOCK_ID)) {
                return Optional.empty();
            }
            return TimelessAPI.getClientBlockIndex(id);
        }
        return Optional.empty();
    }

    @Override
    public void render(GunSmithTableBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        getIndex(blockEntity).ifPresent(index -> {
            BedrockModel model = index.getModel();
            ResourceLocation texture = index.getTexture();
            if (model == null) {
                return;
            }
            BlockState blockState = blockEntity.getBlockState();
            if (blockState.getBlock() instanceof AbstractGunSmithTableBlock block) {
                if (!block.isRoot(blockState)) {
                    return;
                }
                Direction facing = blockState.getValue(AbstractGunSmithTableBlock.FACING);
                poseStack.pushPose();
                poseStack.translate(0.5, 1.5, 0.5);
                poseStack.mulPose(Vector3f.ZN.rotationDegrees(180));
                poseStack.mulPose(Vector3f.YN.rotationDegrees(block.parseRotation(facing)));
                RenderType renderType = RenderType.entityTranslucent(texture);
                model.render(poseStack, ItemTransforms.TransformType.NONE, renderType, combinedLightIn, combinedOverlayIn);
                poseStack.popPose();
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen(GunSmithTableBlockEntity blockEntity) {
        return true;
    }
}