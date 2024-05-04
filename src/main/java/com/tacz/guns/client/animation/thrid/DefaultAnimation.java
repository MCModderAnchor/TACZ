package com.tacz.guns.client.animation.thrid;

import com.tacz.guns.api.client.animation.IThirdPersonAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class DefaultAnimation implements IThirdPersonAnimation {
    @Override
    public void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head) {
        rightArm.yRot = -0.3F + head.yRot;
        leftArm.yRot = 0.8F + head.yRot;
        rightArm.xRot = -1.4F + head.xRot;
        leftArm.xRot = -1.4F + head.xRot;
    }

    @Override
    public void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head, float aimProgress) {
        float lerp1 = Mth.lerp(aimProgress, 0.3f, 0.35f);
        float lerp2 = Mth.lerp(aimProgress, 1.4f, 1.6f);
        rightArm.yRot = -lerp1 + head.yRot;
        leftArm.yRot = 0.8F + head.yRot;
        rightArm.xRot = -lerp2 + head.xRot;
        leftArm.xRot = -lerp2 + head.xRot;
    }
}
