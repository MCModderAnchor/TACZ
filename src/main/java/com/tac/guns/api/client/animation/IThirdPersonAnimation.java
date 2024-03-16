package com.tac.guns.api.client.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public interface IThirdPersonAnimation {
    void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun);

    void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun, float aimProgress);
}
