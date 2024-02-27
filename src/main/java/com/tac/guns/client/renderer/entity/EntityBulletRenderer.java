package com.tac.guns.client.renderer.entity;

import com.tac.guns.entity.EntityBullet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EntityBulletRenderer extends EntityRenderer<EntityBullet> {
    public EntityBulletRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBullet pEntity) {
        return null;
    }
}
