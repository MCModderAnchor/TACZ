package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class GunDisplayCaseHighlightEvent {
    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        BlockHitResult target = event.getTarget();
        BlockPos pos = target.getBlockPos();
        Entity cameraEntity = event.getCamera().getEntity();
        Level level = cameraEntity.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        GunDisplayCaseBlockEntity displayCase = blockEntity instanceof GunDisplayCaseBlockEntity value ? value : null;
        if (cameraEntity.isShiftKeyDown()) {
            renderOccupiedSlots(event, state, pos, displayCase);
            return;
        }
        renderAimedSlot(event, state, pos, target, displayCase);
    }

    private static void renderAimedSlot(RenderHighlightEvent.Block event, BlockState state, BlockPos pos, BlockHitResult target, GunDisplayCaseBlockEntity displayCase) {
        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, target.getLocation());
        if (slot < 0) {
            return;
        }
        boolean occupied = displayCase != null && !displayCase.getItem(slot).isEmpty();
        renderSlotBox(event, state, pos, slot, occupied ? 1.0F : 1.0F, occupied ? 0.0F : 1.0F, 0.0F);
    }

    private static void renderOccupiedSlots(RenderHighlightEvent.Block event, BlockState state, BlockPos pos, GunDisplayCaseBlockEntity displayCase) {
        if (displayCase == null) {
            return;
        }
        for (int slot = 0; slot < GunDisplayCaseBlockEntity.SLOT_COUNT; slot++) {
            if (!displayCase.getItem(slot).isEmpty()) {
                renderSlotBox(event, state, pos, slot, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    private static void renderSlotBox(RenderHighlightEvent.Block event, BlockState state, BlockPos pos, int slot, float red, float green, float blue) {
        Vec3 camera = event.getCamera().getPosition();
        AABB aabb = GunDisplayCaseSlotHelper.getSlotAabb(state, pos, slot).move(-camera.x, -camera.y, -camera.z);
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(DisplayCaseHighlightRenderType.getLines());
        LevelRenderer.renderLineBox(event.getPoseStack(), buffer, aabb, red, green, blue, 1.0F);
    }

    private static class DisplayCaseHighlightRenderType extends RenderStateShard {
        private static final RenderType LINES_NO_DEPTH = RenderType.create(
                "tacz_display_case_highlight",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(DEFAULT_LINE)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)
        );

        private DisplayCaseHighlightRenderType(String name, Runnable setupState, Runnable clearState) {
            super(name, setupState, clearState);
        }

        private static RenderType getLines() {
            return LINES_NO_DEPTH;
        }
    }
}
