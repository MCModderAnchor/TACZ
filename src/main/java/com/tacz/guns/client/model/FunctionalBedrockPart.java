package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.UnshadedVertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * visible的优先级低于FunctionalBedrockPart，当visible为false的时候，仍然会执行functionalRenderers
 */
public class FunctionalBedrockPart extends BedrockPart {
    public @Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer;

    public FunctionalBedrockPart(@Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer, @Nonnull String name) {
        super(name);
        this.functionalRenderer = functionalRenderer;
    }

    public FunctionalBedrockPart(@Nullable Function<BedrockPart, IFunctionalRenderer> functionalRenderer, @Nonnull BedrockPart part) {
        super(part.name);
        this.cubes.addAll(part.cubes);
        this.children.addAll(part.children);
        this.x = part.x;
        this.y = part.y;
        this.z = part.z;
        this.xRot = part.xRot;
        this.yRot = part.yRot;
        this.zRot = part.zRot;
        this.offsetX = part.offsetX;
        this.offsetY = part.offsetY;
        this.offsetZ = part.offsetZ;
        this.visible = part.visible;
        this.mirror = part.mirror;
        this.setInitRotationAngle(part.getInitRotX(), part.getInitRotY(), part.getInitRotZ());
        this.xScale = part.xScale;
        this.yScale = part.yScale;
        this.zScale = part.zScale;
        this.functionalRenderer = functionalRenderer;
    }

    @Override
    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        render(poseStack, transformType, consumer, light, overlay, red, green, blue, alpha, null);
    }

    @Override
    public void render(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha, @Nullable MultiBufferSource bufferSource) {
        int cubePackedLight = light;
        VertexConsumer cubeConsumer = consumer;
        if (illuminated) {
            // 最大亮度
            cubePackedLight = LightTexture.pack(15, 15);
            cubeConsumer = UnshadedVertexConsumer.wrap(consumer);
        }

        poseStack.pushPose();
        this.translateAndRotateAndScale(poseStack);

        if (functionalRenderer != null) {
            @Nullable IFunctionalRenderer renderer = functionalRenderer.apply(this);
            if (renderer != null) {
                if (bufferSource != null) {
                    renderer.render(poseStack, cubeConsumer, transformType, cubePackedLight, overlay, bufferSource);
                } else {
                    renderer.render(poseStack, cubeConsumer, transformType, cubePackedLight, overlay);
                }
            } else {
                if (this.visible) {
                    super.compile(poseStack.last(), cubeConsumer, cubePackedLight, overlay, red, green, blue, alpha);
                    for (BedrockPart part : this.children) {
                        part.render(poseStack, transformType, cubeConsumer, cubePackedLight, overlay, red, green, blue, alpha, bufferSource);
                    }
                }
            }
        } else {
            if (this.visible) {
                super.compile(poseStack.last(), cubeConsumer, cubePackedLight, overlay, red, green, blue, alpha);
                for (BedrockPart part : this.children) {
                    part.render(poseStack, transformType, cubeConsumer, cubePackedLight, overlay, red, green, blue, alpha, bufferSource);
                }
            }
        }
        poseStack.popPose();
    }
}
