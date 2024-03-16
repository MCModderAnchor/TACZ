package com.tac.guns.mixin.client;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.thrid.ThirdPersonManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "TAIL"))
    private void setRotationAnglesHead(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entityIn instanceof IGunOperator operator) {
            ItemStack mainHandItem = entityIn.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(mainHandItem)).ifPresent(index -> {
                String animation = index.getThirdPersonAnimation();
                float aimingProgress = operator.getSynAimingProgress();
                if (aimingProgress <= 0) {
                    ThirdPersonManager.getAnimation(animation).animateGunHold(entityIn, rightArm, leftArm, head, true);
                } else {
                    ThirdPersonManager.getAnimation(animation).animateGunAim(entityIn, rightArm, leftArm, head, true, aimingProgress);
                }
            });
        }
    }
}
