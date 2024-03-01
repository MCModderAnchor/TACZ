package com.tac.guns.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TileEntityItemStackGunRenderer extends BlockEntityWithoutLevelRenderer {
    public TileEntityItemStackGunRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemTransforms.TransformType transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (stack.is(ModItems.GUN.get())) {
            // Fixme：硬编码，应该改成配置文件可以调节
            ResourceLocation gunId = GunItem.getData(stack).getGunId();
            ClientGunIndex gunIndex = ClientGunLoader.getGunIndex(gunId);
            BedrockGunModel gunModel = gunIndex.getGunModel();
            poseStack.pushPose();
            poseStack.translate(0.425, 1.25, 0.375);
            poseStack.scale(-0.55f, -0.55f, 0.55f);
            gunModel.render(0, transformType, stack, null, poseStack, pBuffer, pPackedLight, pPackedOverlay);
            poseStack.popPose();
        }
    }
}
