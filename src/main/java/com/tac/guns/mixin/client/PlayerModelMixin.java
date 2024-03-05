package com.tac.guns.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;
    @Shadow
    @Final
    public ModelPart jacket;
    @Shadow
    @Final
    private boolean slim;

    public PlayerModelMixin(ModelPart part) {
        super(part);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "HEAD"))
    private void setRotationAnglesHead(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entityIn instanceof Player)) {
            return;
        }

        this.tac$resetAll(this.head);
        this.tac$resetAll(this.hat);
        this.tac$resetAll(this.body);
        this.tac$resetAll(this.jacket);

        this.tac$resetAll(this.rightArm);
        this.rightArm.x = -5.0F;
        this.rightArm.y = this.slim ? 2.5F : 2.0F;
        this.rightArm.z = 0.0F;

        this.tac$resetAll(this.rightSleeve);
        this.rightSleeve.x = -5.0F;
        this.rightSleeve.y = this.slim ? 2.5F : 2.0F;
        this.rightSleeve.z = 10.0F;

        this.tac$resetAll(this.leftArm);
        this.leftArm.x = 5.0F;
        this.leftArm.y = this.slim ? 2.5F : 2.0F;
        this.leftArm.z = 0.0F;

        this.tac$resetAll(this.leftSleeve);
        this.leftSleeve.x = 5.0F;
        this.leftSleeve.y = this.slim ? 2.5F : 2.0F;
        this.leftSleeve.z = 0.0F;

        this.tac$resetAll(this.leftLeg);
        this.leftLeg.x = 1.9F;
        this.leftLeg.y = 12.0F;
        this.leftLeg.z = 0.0F;

        this.tac$resetAll(this.leftPants);
        this.leftPants.copyFrom(this.leftLeg);

        this.tac$resetAll(this.rightLeg);
        this.rightLeg.x = -1.9F;
        this.rightLeg.y = 12.0F;
        this.rightLeg.z = 0.0F;

        this.tac$resetAll(this.rightPants);
        this.rightPants.copyFrom(this.rightLeg);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "TAIL"))
    private void setRotationAnglesTail(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entityIn instanceof Player)) {
            return;
        }

        // Dirty Hack，用于清除默认的手臂旋转
        @SuppressWarnings("all")
        PlayerModel model = (PlayerModel) (Object) this;
        if (ageInTicks == 0F) {
            model.rightArm.xRot = 0;
            model.rightArm.yRot = 0;
            model.rightArm.zRot = 0;
            model.leftArm.xRot = 0;
            model.leftArm.yRot = 0;
            model.leftArm.zRot = 0;
            return;
        }

        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
        this.jacket.copyFrom(this.body);
        this.hat.copyFrom(this.head);
    }

    /**
     * 将给定模型的旋转角度和旋转点重置为零
     */
    @Unique
    private void tac$resetAll(ModelPart part) {
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        part.x = 0.0F;
        part.y = 0.0F;
        part.z = 0.0F;
    }
}