package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.pojo.display.gun.RangefinderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RangefinderImpactPredictionRender implements IFunctionalRenderer {
    private static final float MODEL_TEXT_SCALE = 2.0f / 300.0f;
    private static final float FALLBACK_MODEL_PROJECTION_HALF_HEIGHT = 80.0f;
    private static final float HALF_SIZE = 3.0f;
    private static final float LINE_WIDTH = 1.0f;
    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    private final BedrockModel bedrockModel;

    public RangefinderImpactPredictionRender(BedrockModel bedrockModel) {
        this.bedrockModel = bedrockModel;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        render(poseStack, vertexBuffer, transformType, light, overlay, Minecraft.getInstance().renderBuffers().bufferSource());
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource.BufferSource bufferSource) {
        if (!transformType.firstPerson() || !RangefinderManager.shouldRenderImpactPrediction()) {
            return;
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());

        bedrockModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);
            poseStack2.scale(MODEL_TEXT_SCALE, -MODEL_TEXT_SCALE, -MODEL_TEXT_SCALE);

            RenderType renderType = ImpactPredictionRenderState.getBox();
            VertexConsumer consumer = bufferSource.getBuffer(renderType);
            float yOffset = getCalibratedYOffset(poseStack2.last().pose());
            renderBox(consumer, poseStack2.last().pose(), yOffset, RangefinderManager.getImpactPredictionColor(), RangefinderManager.getImpactPredictionScale());
            bufferSource.endBatch(renderType);
        });
    }

    private static float getCalibratedYOffset(Matrix4f pose) {
        Minecraft mc = Minecraft.getInstance();
        int height = mc.getWindow().getGuiScaledHeight();
        float targetY = height / 2.0f + RangefinderManager.getImpactProjectionY() * height / 2.0f;
        float originY = projectGuiY(pose, 0.0f);
        float unitY = projectGuiY(pose, 1.0f);
        float pixelsPerLocalY = unitY - originY;
        if (!Float.isFinite(originY) || !Float.isFinite(unitY) || Math.abs(pixelsPerLocalY) < 1.0E-4f) {
            return RangefinderManager.getImpactProjectionY() * FALLBACK_MODEL_PROJECTION_HALF_HEIGHT;
        }
        return Mth.clamp((targetY - originY) / pixelsPerLocalY, -1000.0f, 1000.0f);
    }

    private static float projectGuiY(Matrix4f pose, float localY) {
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        Vector4f point = new Vector4f(0.0f, localY, 0.0f, 1.0f);
        point.mul(pose);
        point.mul(RenderSystem.getProjectionMatrix());
        if (point.w() <= 0.0f) {
            return Float.NaN;
        }
        float ndcY = point.y() / point.w();
        return (1.0f - ndcY) * height / 2.0f;
    }

    private static void renderBox(VertexConsumer consumer, Matrix4f pose, float yOffset, int color, float scale) {
        int a = color >>> 24 & 0xFF;
        int r = color >>> 16 & 0xFF;
        int g = color >>> 8 & 0xFF;
        int b = color & 0xFF;

        float halfSize = HALF_SIZE * scale;
        float left = -halfSize;
        float right = halfSize;
        float top = yOffset - halfSize;
        float bottom = yOffset + halfSize;

        quad(consumer, pose, left, top, right, top + LINE_WIDTH, r, g, b, a);
        quad(consumer, pose, left, bottom - LINE_WIDTH, right, bottom, r, g, b, a);
        quad(consumer, pose, left, top, left + LINE_WIDTH, bottom, r, g, b, a);
        quad(consumer, pose, right - LINE_WIDTH, top, right, bottom, r, g, b, a);
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose, float minX, float minY, float maxX, float maxY,
                             int r, int g, int b, int a) {
        consumer.vertex(pose, minX, minY, 0.0f).color(r, g, b, a).uv(0.0f, 0.0f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(pose, minX, maxY, 0.0f).color(r, g, b, a).uv(0.0f, 1.0f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(pose, maxX, maxY, 0.0f).color(r, g, b, a).uv(1.0f, 1.0f).uv2(FULL_BRIGHT).endVertex();
        consumer.vertex(pose, maxX, minY, 0.0f).color(r, g, b, a).uv(1.0f, 0.0f).uv2(FULL_BRIGHT).endVertex();
    }

    private static class ImpactPredictionRenderState extends RenderStateShard {
        private static final RenderType BOX = RenderType.create(
                "tacz_rangefinder_impact_prediction",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(BeamRenderer.LaserBeamRenderState.LIGHTNING_ADDITIVE_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setLightmapState(LIGHTMAP)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL)
                        .setTextureState(new TextureStateShard(BeamRenderer.LASER_BEAM_TEXTURE, false, false))
                        .createCompositeState(false));

        private ImpactPredictionRenderState(String name, Runnable setupState, Runnable clearState) {
            super(name, setupState, clearState);
        }

        private static RenderType getBox() {
            return BOX;
        }
    }
}
