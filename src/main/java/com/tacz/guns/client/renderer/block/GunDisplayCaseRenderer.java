package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.client.renderer.item.DisplayItemRenderContext;
import com.tacz.guns.client.renderer.item.ItemPreviewRenderContext;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GunDisplayCaseRenderer implements BlockEntityRenderer<GunDisplayCaseBlockEntity> {
    private static final double ITEM_OFFSET = 0.4375D - 3.0D / 16.0D;
    private static final float PREVIEW_ALPHA = 0.65F;

    public GunDisplayCaseRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GunDisplayCaseBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = blockEntity.getBlockState().getValue(GunDisplayCaseBlock.FACING);
        for (int slot = 0; slot < GunDisplayCaseBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (DisplayGunMeshGroup.tryRender(blockEntity, slot, packedOverlay)) {
                continue;
            }
            int row = slot / GunDisplayCaseBlockEntity.GRID_SIZE;
            int col = slot % GunDisplayCaseBlockEntity.GRID_SIZE;
            double x = (col + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
            double y = (row + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
            renderCaseItem(stack, blockEntity.getLevel(), facing, x, y, blockEntity.getRotation(slot), poseStack, buffer, displayItemLight(), packedOverlay, 1.0F);
        }
        renderPlacementPreview(blockEntity, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderPlacementPreview(GunDisplayCaseBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getBlockState().getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = blockEntity.getLevel();
        if (player == null || level == null || player.isShiftKeyDown() || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        if (!hit.getBlockPos().equals(pos)) {
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.isEmpty() || !GunPegboardItemConfigRead.canPlace(held)) {
            return;
        }
        int slot = GunDisplayCaseSlotHelper.getSlot(blockEntity.getBlockState(), pos, hit.getLocation());
        if (slot < 0 || !blockEntity.getItem(slot).isEmpty()) {
            return;
        }
        int row = slot / GunDisplayCaseBlockEntity.GRID_SIZE;
        int col = slot % GunDisplayCaseBlockEntity.GRID_SIZE;
        double x = (col + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
        double y = (row + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
        Direction facing = blockEntity.getBlockState().getValue(GunDisplayCaseBlock.FACING);
        ItemStack preview = held.copy();
        preview.setCount(1);
        renderCaseItem(preview, level, facing, x, y, 0, poseStack, buffer, displayItemLight(), packedOverlay, PREVIEW_ALPHA);
    }

    private static void renderCaseItem(ItemStack stack, Level level, Direction facing, double x, double y, int rotation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();
        DisplayItemTransforms.apply(poseStack, facing, x, y, ITEM_OFFSET, rotation);
        if (alpha < 1.0F) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            ItemPreviewRenderContext.setAlpha(alpha);
        }
        DisplayItemRenderContext.render(() -> Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                alpha < 1.0F ? OverlayTexture.NO_OVERLAY : packedOverlay,
                poseStack,
                buffer,
                level,
                0
        ));
        if (alpha < 1.0F) {
            ItemPreviewRenderContext.setAlpha(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    private static int displayItemLight() {
        return LightTexture.pack(15, 15);
    }

    @Override
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(GunDisplayCaseBlockEntity blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(GunDisplayCaseBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, this.getViewDistance());
    }
}
