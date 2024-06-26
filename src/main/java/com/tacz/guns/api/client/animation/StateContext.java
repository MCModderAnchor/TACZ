package com.tacz.guns.api.client.animation;

import net.minecraft.world.entity.Entity;

public class StateContext {
    private float partialTicks;
    private Entity entity;

    public float getPartialTicks() {
        return partialTicks;
    }

    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }
}
