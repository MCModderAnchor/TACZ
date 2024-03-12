package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransforms;

public interface ISimpleRenderer {
    void render(PoseStack poseStack, ItemTransforms.TransformType transformType, int light, int overlay);
}
