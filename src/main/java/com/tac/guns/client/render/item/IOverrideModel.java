package com.tac.guns.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A simple interface to render custom models.
 */
public interface IOverrideModel {
    /**
     * Renders the overridden model.
     *
     * @param partialTicks  the current partial ticks
     * @param transformType the camera transform type
     * @param stack         the itemstack of the item that has the overridden model
     * @param parent        if an attachment, the parent is the weapon this attachment is attached to otherwise it's an empty stack.
     * @param entity        the entity holding the item
     * @param matrixStack   the current matrix stack
     * @param buffer        a render type buffer get
     * @param light         the combined light for the item
     * @param overlay       the overlay texture for the item
     */
    void render(float partialTicks, ItemTransforms.TransformType transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay);
}
