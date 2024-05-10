package com.tacz.guns.util;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.phys.EntityHitResult;

public class TacHitResult extends EntityHitResult {
    private final boolean headshot;

    public TacHitResult(EntityKineticBullet.EntityResult result) {
        super(result.getEntity(), result.getHitPos());
        this.headshot = result.isHeadshot();
    }

    public boolean isHeadshot() {
        return this.headshot;
    }
}