package com.tacz.guns.client.animation.third;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class InnerThirdPersonManager {
    public static void setRotationAnglesHead(LivingEntity entityIn, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head, float limbSwingAmount) {
        if (AnimationRenderContext.isFirstPersonHand(entityIn) || !PlayerAnimatorCompat.canPlayAnimation(entityIn)) {
            return;
        }
        if (entityIn instanceof IGunOperator operator) {
            ItemStack mainHandItem = entityIn.getMainHandItem();
            TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                if (!PlayerAnimatorCompat.hasPlayerAnimator3rd(entityIn, display)) {
                    playVanillaAnimation(entityIn, rightArm, leftArm, body, head, operator, display);
                }
            });
        }
    }

    private static void playVanillaAnimation(LivingEntity entityIn, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head, IGunOperator operator, GunDisplayInstance display) {
        String animation = display.getThirdPersonAnimation();
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            ThirdPersonManager.getAnimation(animation).animateGunHold(entityIn, rightArm, leftArm, body, head);
        } else {
            ThirdPersonManager.getAnimation(animation).animateGunAim(entityIn, rightArm, leftArm, body, head, aimingProgress);
        }
    }
}
