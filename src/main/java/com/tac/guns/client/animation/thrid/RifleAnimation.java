package com.tac.guns.client.animation.thrid;

import com.tac.guns.api.client.animation.IThirdPersonAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;

public class RifleAnimation implements IThirdPersonAnimation {
    @Override
    public void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun) {
        ModelPart holdModel = rightHoldGun ? rightArm : leftArm;
        ModelPart otherModel = rightHoldGun ? leftArm : rightArm;
        holdModel.yRot = (rightHoldGun ? -0.3F : 0.3F) + head.yRot;
        otherModel.yRot = (rightHoldGun ? 0.6F : -0.6F) + head.yRot;
        holdModel.xRot = (-(float) Math.PI / 2F) + head.xRot + 0.1F;
        otherModel.xRot = -1.5F + head.xRot;
    }

    @Override
    public void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun, float aimProgress) {
        ModelPart holdModel = rightHoldGun ? rightArm : leftArm;
        ModelPart otherModel = rightHoldGun ? leftArm : rightArm;
        holdModel.yRot = (rightHoldGun ? -0.3F : 0.3F) + head.yRot;
        otherModel.yRot = (rightHoldGun ? 0.6F : -0.6F) + head.yRot;
        holdModel.xRot = (-(float) Math.PI / 2F) + head.xRot + 0.1F;
        otherModel.xRot = -1.5F + head.xRot;
    }
}
