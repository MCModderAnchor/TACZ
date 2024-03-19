package com.tac.guns.client.model;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tac.guns.api.client.render.ModifiableFrameBuffer;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.lwjgl.opengl.GL11;

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

    public void render(PoseStack matrixStack, ItemTransforms.TransformType transformType, RenderType renderType, int light, int overlay) {
        renderOcular(matrixStack, transformType, renderType, light, overlay);
    }

    public void renderOcular(PoseStack matrixStack, ItemTransforms.TransformType transformType, RenderType renderType, int light, int overlay){
        enableStencilTest();
        ModifiableFrameBuffer frameBuffer = (ModifiableFrameBuffer) Minecraft.getInstance().getMainRenderTarget();
        frameBuffer.setStencilBufferEnabledAndReload(true);
        if (ocularRingPath != null){
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
        if (lensNodePath != null){
            // 开启模板测试
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            // 绘制透镜
            renderTempPart(matrixStack, transformType, renderType, light, overlay, lensNodePath);
        }
        if (scopeBodyPath != null){
            // 开启模板测试
            RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            // 绘制镜筒
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        disableStencilTest();
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
    }

    private static void renderTempPart(PoseStack poseStack, ItemTransforms.TransformType transformType, RenderType renderType,
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

    private static void enableStencilTest(){
        RenderSystem.assertOnRenderThread();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    private static void disableStencilTest(){
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath() {
        return scopeViewPath;
    }
}