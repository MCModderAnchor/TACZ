package com.tac.guns.client.animation.thrid;

import com.tac.guns.api.client.animation.IThirdPersonAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class DefaultAnimation implements IThirdPersonAnimation {
    @Override
    public void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun) {
        ModelPart holdModel = rightHoldGun ? rightArm : leftArm;
        ModelPart otherModel = rightHoldGun ? leftArm : rightArm;
        holdModel.yRot = (rightHoldGun ? -0.3F : 0.3F) + head.yRot;
        otherModel.yRot = (rightHoldGun ? 0.8F : -0.8F) + head.yRot;
        holdModel.xRot = -1.4F + head.xRot;
        otherModel.xRot = -1.4F + head.xRot;
    }

    @Override
    public void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun, float aimProgress) {
        ModelPart holdModel = rightHoldGun ? rightArm : leftArm;
        ModelPart otherModel = rightHoldGun ? leftArm : rightArm;
        float lerp1 = Mth.lerp(aimProgress, 0.3f, 0.35f);
        float lerp2 = Mth.lerp(aimProgress, 1.4f, 1.6f);
        holdModel.yRot = (rightHoldGun ? -lerp1 : lerp1) + head.yRot;
        otherModel.yRot = (rightHoldGun ? 0.8F : -0.8F) + head.yRot;
        holdModel.xRot = -lerp2 + head.xRot;
        otherModel.xRot = -lerp2 + head.xRot;
    }
}
