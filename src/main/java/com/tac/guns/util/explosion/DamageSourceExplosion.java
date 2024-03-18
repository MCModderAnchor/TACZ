package com.tac.guns.util.explosion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class DamageSourceExplosion extends EntityDamageSource {
    private ResourceLocation item;

    public DamageSourceExplosion(@Nullable Entity damageSourceEntityIn, ResourceLocation item) {
        super("explosion.player", damageSourceEntityIn);
        this.item = item;
    }

    public ResourceLocation getItem() {
        return item;
    }
}
