package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;

public interface IFunctionalRenderer {
    @Deprecated
    void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay);

    /**
     * 带 bufferSource 的渲染：由模型渲染循环在持有当前 bufferSource 时调用。
     * 需要按手部 buffer（而非全局 renderBuffers）绘制的渲染器覆写此方法；
     * 默认实现保留旧函数式委托；需要另取顶点消费者的实现应沿用此 bufferSource。
     */
    default void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay, MultiBufferSource bufferSource) {
        render(poseStack, vertexBuffer, transformType, light, overlay);
    }
}
