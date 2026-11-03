package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At(value = "TAIL"))
    private void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, LivingEntity livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
        // 第三人称持枪层渲染结束，清除当前实体关联
        ThirdPersonMuzzleParticleManager.currentEntity = null;
        HumanoidOffhandRender.renderGun(livingEntity, matrixStack, buffer, packedLight);
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderArmWithItemHead(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext pDisplayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        // 记录正在渲染第三人称持枪层的实体，供 BEWLR 渲染时捕获枪口世界坐标
        ThirdPersonMuzzleParticleManager.currentEntity = livingEntity;
        LocalPlayer player = Minecraft.getInstance().player;
        if (livingEntity.equals(player)) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
        if (IGun.mainHandHoldGun(livingEntity) && arm == HumanoidArm.LEFT) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "TAIL"))
    private void renderArmWithItemTail(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext pDisplayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        MuzzleFlashRender.isSelf = false;
        ShellRender.isSelf = false;
    }
}
