package com.tacz.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin implements KeepingItemRenderer {
    @Shadow
    private float mainHandHeight;
    @Shadow
    private float oMainHandHeight;
    @Shadow
    private ItemStack mainHandItem;
    @Unique
    private ItemStack tacz$KeepItem;
    @Unique
    private long tacz$KeepTimeMs;
    @Unique
    private long tacz$KeepTimestamp;
    @Unique
    private long tacz$KeepEpoch = -1;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource.BufferSource pBuffer, LocalPlayer pPlayerEntity, int pCombinedLight, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new BeforeRenderHandEvent(pMatrixStack));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void cancelEquippedProgress(CallbackInfo ci) {
//        if (Minecraft.getInstance().player == null) {
//            return;
//        }
//        if (tacz$KeepItem != null) {
//            long time = GunTime.nowMillis() - tacz$KeepTimestamp;
//            if (time < tacz$KeepTimeMs) {
//                mainHandHeight = 1.0f;
//                oMainHandHeight = 1.0f;
//                mainHandItem = tacz$KeepItem;
//                return;
//            }
//        }
//        ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
//        IGun iGun = IGun.getIGunOrNull(itemStack);
//        if (iGun != null) {
//            mainHandHeight = 1.0f;
//            oMainHandHeight = 1.0f;
//            mainHandItem = itemStack;
//        }
    }

    @Unique
    @Override
    public void keep(ItemStack itemStack, long timeMs) {
        if (tacz$KeepItem != null && tacz$KeepEpoch == GunTime.epoch()
                && GunTime.isWithin(tacz$KeepTimestamp, tacz$KeepTimeMs)) {
            return;
        }
        this.tacz$KeepTimeMs = timeMs;
        this.tacz$KeepTimestamp = GunTime.nowMillis();
        this.tacz$KeepEpoch = GunTime.epoch();
        this.tacz$KeepItem = itemStack;
        this.mainHandItem = itemStack;
    }

    @Override
    public ItemStack getCurrentItem() {
        if (Minecraft.getInstance().player == null) {
            return mainHandItem;
        }
        if (tacz$KeepItem != null) {
            if (tacz$KeepEpoch == GunTime.epoch() && GunTime.isWithin(tacz$KeepTimestamp, tacz$KeepTimeMs)) {
                return tacz$KeepItem;
            } else {
                tacz$KeepItem = null;
            }
        }
        return mainHandItem;
    }
}
