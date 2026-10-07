package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
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

public class GunPegboardRenderer implements BlockEntityRenderer<GunPegboardBlockEntity> {
    private static final double ITEM_FRAME_ITEM_OFFSET = 0.4375D - 3.0D / 16.0D;
    private static final float PREVIEW_ALPHA = 0.65F;

    public GunPegboardRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GunPegboardBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < GunPegboardBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (DisplayGunMeshGroup.tryRender(blockEntity, slot, packedOverlay)) {
                continue;
            }
            Direction facing = GunPegboardSlotHelper.getSlotFacing(blockEntity.getBlockState(), slot);
            int row = (slot % GunPegboardBlockEntity.SLOTS_PER_FACE) / GunPegboardBlockEntity.GRID_SIZE;
            int col = slot % GunPegboardBlockEntity.GRID_SIZE;
            double x = (col + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
            double y = (row + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;

            int itemLight = isGlowing(blockEntity) ? LightTexture.pack(15, 15) : packedLight;
            renderPegboardItem(stack, blockEntity.getLevel(), facing, x, y, blockEntity.getRotation(slot), poseStack, buffer, itemLight, packedOverlay, 1.0F);
        }
        renderPlacementPreview(blockEntity, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderPlacementPreview(GunPegboardBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
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
        int slot = GunPegboardSlotHelper.getSlot(blockEntity.getBlockState(), pos, hit.getLocation());
        if (slot < 0 || !blockEntity.getItem(slot).isEmpty()) {
            return;
        }
        int row = (slot % GunPegboardBlockEntity.SLOTS_PER_FACE) / GunPegboardBlockEntity.GRID_SIZE;
        int col = slot % GunPegboardBlockEntity.GRID_SIZE;
        double x = (col + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
        double y = (row + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
        Direction facing = GunPegboardSlotHelper.getSlotFacing(blockEntity.getBlockState(), slot);
        int itemLight = isGlowing(blockEntity) ? LightTexture.pack(15, 15) : packedLight;
        ItemStack preview = held.copy();
        preview.setCount(1);
        renderPegboardItem(preview, level, facing, x, y, 0, poseStack, buffer, itemLight, packedOverlay, PREVIEW_ALPHA);
    }

    private static void renderPegboardItem(ItemStack stack, Level level, Direction facing, double x, double y, int rotation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();
        DisplayItemTransforms.apply(poseStack, facing, x, y, ITEM_FRAME_ITEM_OFFSET, rotation);
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
