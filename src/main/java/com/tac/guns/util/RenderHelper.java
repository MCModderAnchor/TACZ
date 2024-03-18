package com.tac.guns.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;

public class RenderHelper {
    public static void blit(PoseStack poseStack, float x, float y, float uOffset, float vOffset, float pWidth, float height, float textureWidth, float textureHeight) {
        blit(poseStack, x, y, pWidth, height, uOffset, vOffset, pWidth, height, textureWidth, textureHeight);
    }

    private static void blit(PoseStack poseStack, float x, float y, float pWidth, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        innerBlit(poseStack, x, x + pWidth, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    private static void innerBlit(PoseStack poseStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight) {
        innerBlit(poseStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight);
    }

    private static void innerBlit(Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix, x1, y2, blitOffset).uv(minU, maxV).endVertex();
        bufferbuilder.vertex(matrix, x2, y2, blitOffset).uv(maxU, maxV).endVertex();
        bufferbuilder.vertex(matrix, x2, y1, blitOffset).uv(maxU, minV).endVertex();
        bufferbuilder.vertex(matrix, x1, y1, blitOffset).uv(minU, minV).endVertex();
        bufferbuilder.end();
        BufferUploader.end(bufferbuilder);
    }
}
