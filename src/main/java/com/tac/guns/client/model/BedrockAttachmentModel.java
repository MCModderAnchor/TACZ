package com.tac.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Matrix4f;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String OCULAR_NODE = "ocular";
    @Nullable
    protected List<BedrockPart> scopeViewPath;
    @Nullable
    protected List<BedrockPart> ocularNodePath;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPath = getPath(modelMap.get(SCOPE_VIEW_NODE));
        ocularNodePath = getPath(modelMap.get(OCULAR_NODE));
    }

    public void render(PoseStack matrixStack, ItemTransforms.TransformType transformType, ResourceLocation textureLocation, int light, int overlay) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        BedrockPart ocularPart = null;
        boolean renderOcular = false;
        if (ocularNodePath != null) {
            RenderSystem.colorMask(false, false, false, false);
            matrixStack.pushPose();
            for(int i = 0; i < ocularNodePath.size() - 1; ++i){
                ocularNodePath.get(i).translateAndRotateAndScale(matrixStack);
            }
            ocularPart = ocularNodePath.get(ocularNodePath.size() - 1);
            renderOcular = ocularPart.visible;
            RenderType renderType = RenderType.itemEntityTranslucentCull(textureLocation);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            ocularPart.render(matrixStack, transformType, vertexConsumer, light, overlay);
            bufferSource.endBatch(renderType);
            matrixStack.popPose();
            RenderSystem.colorMask(true, true, true, true);
        }
        if (ocularPart != null) {
            ocularPart.visible = false;
        }
        super.render(matrixStack, transformType, RenderType.itemEntityTranslucentCull(textureLocation), light, overlay);
        if (ocularPart != null) {
            ocularPart.visible = renderOcular;
        }
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath() {
        return scopeViewPath;
    }
}