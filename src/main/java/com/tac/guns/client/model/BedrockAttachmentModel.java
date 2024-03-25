package com.tac.guns.client.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Vector3f;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String LENS_NODE = "lens";
    private static final String OCULAR_NODE = "ocular";
    @Nullable
    protected List<BedrockPart> scopeViewPath;
    @Nullable
    protected List<BedrockPart> scopeBodyPath;
    @Nullable
    protected List<BedrockPart> ocularRingPath;
    @Nullable
    protected List<BedrockPart> ocularNodePath;
    @Nullable
    protected List<BedrockPart> lensNodePath;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPath = getPath(modelMap.get(SCOPE_VIEW_NODE));
        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
        ocularNodePath = getPath(modelMap.get(OCULAR_NODE));
        lensNodePath = getPath(modelMap.get(LENS_NODE));
    }

    private static void enableStencilTest(){
        RenderSystem.assertOnRenderThread();
        int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (depthTextureId != GL30.GL_NONE && stencilTextureId == GL30.GL_NONE) {
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthTextureId);
            int dataType = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_DEPTH_TYPE);
            if (dataType == GL30.GL_UNSIGNED_NORMALIZED) {
                int width = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_WIDTH);
                int height = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_HEIGHT);
                GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, null);
                GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, 3553, depthTextureId, 0);
            }
        }
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath() {
        return scopeViewPath;
    }

    public void render(PoseStack matrixStack, ItemTransforms.TransformType transformType, RenderType renderType, int light, int overlay) {
        enableStencilTest();
        if (ocularRingPath != null) {
            // 渲染目镜外环
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
        if (ocularNodePath != null) {
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            // 清空模板缓冲区、准备绘制模板缓冲
            RenderSystem.clearStencil(0);
            RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.stencilMask(0xFF);
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            // 绘制目镜
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePath);
            // 恢复渲染状态
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
        }
        if (scopeBodyPath != null) {
            RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        float width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        float rad = Math.min(width, height) / 10;
        Vector3f ocularCenter = getBedrockPartCenter(matrixStack, ocularNodePath);
        float centerX = ocularCenter.x() * 16 * 90;
        float centerY = ocularCenter.y() * 16 * 90;
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(centerX, centerY, -90.0D).color(255, 255, 255, 255).endVertex();
        for (int i = 0; i <= 180; i++) {
            float angle = (float) i * ((float) Math.PI * 2F) / 180.0F;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            builder.vertex(centerX + cos * rad, centerY + sin * rad, -90.0D).color(255, 255, 255, 255).endVertex();
        }
        builder.end();
        BufferUploader.end(builder);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        // 渲染目镜黑色遮罩
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePath);

        if (lensNodePath != null) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 2, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, lensNodePath);
        }

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        disableStencilTest();
    }

    private Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        Vector3f result = new Vector3f(poseStack.last().pose().m03, poseStack.last().pose().m13, poseStack.last().pose().m23);
        poseStack.popPose();
        return result;
    }

    private void renderTempPart(PoseStack poseStack, ItemTransforms.TransformType transformType, RenderType renderType,
                                   int light, int overlay, @Nonnull List<BedrockPart> path){
        poseStack.pushPose();
        for(int i = 0; i < path.size() - 1; ++i){
            path.get(i).translateAndRotateAndScale(poseStack);
        }
        BedrockPart part = path.get(path.size() - 1);
        part.visible = true;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        part.render(poseStack, transformType, vertexConsumer, light, overlay);
        bufferSource.endBatch(renderType);
        part.visible = false;
        poseStack.popPose();
    }

    private static void disableStencilTest(){
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}