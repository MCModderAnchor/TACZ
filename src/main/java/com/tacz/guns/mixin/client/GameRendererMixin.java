package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.RenderItemInHandBobEvent;
import com.tacz.guns.api.client.event.RenderLevelBobEvent;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private boolean tacz$useFovSetting;

    @Shadow
    public abstract Minecraft getMinecraft();

    @Shadow
    public abstract void render(float pPartialTicks, long pNanoTime, boolean pRenderLevel);

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
        // 取消受伤导致的视角摇晃
        if (this.getMinecraft().getCameraEntity() instanceof LocalPlayer player && !player.isDeadOrDying()) {
            if (GunHurtBobTweak.onHurtBobTweak(player, pMatrixStack, pPartialTicks)) {
                ci.cancel();
                return;
            }
        }
        // 触发其他事件
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderItemInHandBobEvent.BobHurt());
        } else {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderLevelBobEvent.BobHurt());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    // 第三个旋转是受伤倾斜；保持死亡旋转及受击方向的前后旋转不变。
    @ModifyArg(method = "bobHurt", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2), index = 0)
    private float tacz$scaleHeldGunHurtTilt(float angle) {
        Minecraft minecraft = this.getMinecraft();
        if (minecraft.options.getCameraType().isFirstPerson()
                && minecraft.getCameraEntity() instanceof LocalPlayer player
                && !player.isDeadOrDying()
                && player.getMainHandItem().getItem() instanceof AbstractGunItem) {
            // 正面和背面受击以俯仰为主，额外减半；侧面保持配置倍率，斜向平滑过渡。
            float frontBackWeight = Math.abs(Mth.sin(player.getHurtDir() * Mth.DEG_TO_RAD));
            float directionMultiplier = 1.0F - 0.5F * frontBackWeight;
            return angle * RenderConfig.GUN_HURT_BOB_MULTIPLIER.get().floatValue() * directionMultiplier;
        }
        return angle;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBobView(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
        boolean cancel;
        if (!tacz$useFovSetting) {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderItemInHandBobEvent.BobView());
        } else {
            cancel = MinecraftForge.EVENT_BUS.post(new RenderLevelBobEvent.BobView());
        }
        if (cancel) {
            ci.cancel();
        }
    }

    /**
     * 是一个 hack 实现。因为 getFov 这个方法只有在构建 投影矩阵 的时候调用。
     * 因此可以根据 getFov 中的 pUseFovSetting 来判断当前准备渲染 Level 还是渲染 HandWithItem 。
     * 至于为什么不直接对 renderItemInHand 这个方法 mixin ，是因为安装了 Optifine 之后，这个方法的内容被大幅度修改了。
     */
    @Inject(method = "getFov", at = @At("HEAD"))
    public void switchRenderType(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting, CallbackInfoReturnable<Double> cir) {
        this.tacz$useFovSetting = pUseFOVSetting;
    }
}
