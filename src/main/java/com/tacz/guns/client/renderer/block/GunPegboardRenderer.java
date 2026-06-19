package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class GunPegboardRenderer implements BlockEntityRenderer<GunPegboardBlockEntity> {
    private static final double ITEM_FRAME_ITEM_OFFSET = 0.4375D - 3.0D / 16.0D;

    public GunPegboardRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GunPegboardBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = blockEntity.getBlockState().getValue(GunPegboardBlock.FACING);
        for (int slot = 0; slot < GunPegboardBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int row = slot / GunPegboardBlockEntity.GRID_SIZE;
            int col = slot % GunPegboardBlockEntity.GRID_SIZE;
            double x = (col + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
            double y = (row + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;

            poseStack.pushPose();
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.get2DDataValue() * 90.0F));
            poseStack.translate(x, y, ITEM_FRAME_ITEM_OFFSET);
            poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getRotation(slot) * 45.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            int itemLight = isGlowing(blockEntity) ? LightTexture.pack(15, 15) : packedLight;
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    itemLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    blockEntity.getLevel(),
                    0
            );
            poseStack.popPose();
        }
    }

    private static boolean isGlowing(GunPegboardBlockEntity blockEntity) {
        return blockEntity.getBlockState().getBlock() instanceof GunPegboardBlock pegboard && pegboard.isGlowing();
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(GunPegboardBlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(GunPegboardBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, this.getViewDistance());
    }
}
