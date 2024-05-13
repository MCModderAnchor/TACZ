package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;

public class TextShowRender implements IFunctionalRenderer {
    private final BedrockGunModel bedrockGunModel;
    private final TextShow textShow;

    public TextShowRender(BedrockGunModel bedrockGunModel, TextShow textShow) {
        this.bedrockGunModel = bedrockGunModel;
        this.textShow = textShow;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemTransforms.TransformType transformType, int light, int overlay) {
        String text = I18n.get(textShow.getTextKey());
        if (StringUtils.isBlank(text)) {
            return;
        }
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
        Matrix3f normal = poseStack.last().normal().copy();
        Matrix4f pose = poseStack.last().pose().copy();

        // 和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
        bedrockGunModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            Font font = Minecraft.getInstance().font;
            boolean shadow = textShow.isShadow();
            int color = textShow.getColorInt();
            float scale = textShow.getScale();
            int packLight = LightTexture.pack(textShow.getTextLight(), textShow.getTextLight());
            int width = font.width(text);
            int xOffset;
            switch (textShow.getAlign()) {
                case CENTER -> xOffset = width / 2;
                case RIGHT -> xOffset = width;
                default -> xOffset = 0;
            }

            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().multiply(pose);
            poseStack2.scale(2 / 300f * scale, -2 / 300f * scale, -2 / 300f);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            font.drawInBatch(text, -xOffset, -font.lineHeight / 2f, color, shadow, poseStack2.last().pose(), bufferSource, false, 0, packLight);
            bufferSource.endBatch();
        });
    }
}
