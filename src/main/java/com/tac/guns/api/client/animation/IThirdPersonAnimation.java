package com.tac.guns.api.client.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public interface IThirdPersonAnimation {
    /**
     * 第三人称动画：持有枪械时
     *
     * @param entity       持有枪械的实体
     * @param rightArm     右手模型
     * @param leftArm      左手模型
     * @param head         头部模型
     * @param rightHoldGun 是否是右手持枪
     */
    void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun);

    /**
     * 第三人称动画：枪械瞄准时
     *
     * @param entity       持有枪械的实体
     * @param rightArm     右手模型
     * @param leftArm      左手模型
     * @param head         头部模型
     * @param rightHoldGun 是否是右手持枪
     * @param aimProgress  瞄准进度 0-1
     */
    void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart head, boolean rightHoldGun, float aimProgress);
}
