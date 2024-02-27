package com.tac.guns.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TileEntityItemStackGunRenderer extends BlockEntityWithoutLevelRenderer {
    public TileEntityItemStackGunRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack pStack, @Nonnull ItemTransforms.TransformType pTransformType, @Nonnull PoseStack pPoseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {

    }
}
