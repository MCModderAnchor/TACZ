package com.tac.guns.client.animation.thrid;

import com.tac.guns.api.client.animation.IThirdPersonAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public class PistolAnimation implements IThirdPersonAnimation {
    @Override
    public void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun) {
    }

    @Override
    public void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun, float aimProgress) {
    }
}
