package com.tac.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.ItemTransforms;

public interface IModelRenderer {
    void render(PoseStack poseStack, ItemTransforms.TransformType transformType, VertexConsumer consumer, int light, int overlay);
}
