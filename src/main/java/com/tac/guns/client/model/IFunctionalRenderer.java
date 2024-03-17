package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.ItemTransforms;

public interface IFunctionalRenderer {
    void render(PoseStack poseStack, ItemTransforms.TransformType transformType, int light, int overlay);
}
