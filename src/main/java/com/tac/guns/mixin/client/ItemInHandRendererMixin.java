package com.tac.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.api.client.event.BeforeRenderHandEvent;
import com.tac.guns.api.item.IGun;
import com.tac.guns.duck.KeepingItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
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
    private ItemStack tac$KeepItem;
    @Unique
    private long tac$KeepTimeMs;
    @Unique
    private long tac$KeepTimestamp;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource.BufferSource pBuffer, LocalPlayer pPlayerEntity, int pCombinedLight, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new BeforeRenderHandEvent(pMatrixStack));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void cancelEquippedProgress(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (tac$KeepItem != null) {
            long time = System.currentTimeMillis() - tac$KeepTimestamp;
            if (time < tac$KeepTimeMs) {
                mainHandHeight = 1.0f;
                oMainHandHeight = 1.0f;
                mainHandItem = tac$KeepItem;
                return;
            }
        }
        ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(itemStack);
        if (iGun != null) {
            mainHandHeight = 1.0f;
            oMainHandHeight = 1.0f;
            mainHandItem = itemStack;
        }
    }

    @Unique
    @Override
    public void keep(ItemStack itemStack, long timeMs) {
        long time = System.currentTimeMillis() - tac$KeepTimestamp;
        if (time < tac$KeepTimeMs) {
            return;
        }
        this.tac$KeepItem = itemStack;
        this.mainHandItem = itemStack;
        this.tac$KeepTimeMs = timeMs;
        this.tac$KeepTimestamp = System.currentTimeMillis();
    }

    @Override
    public ItemStack getCurrentItem() {
        return mainHandItem;
    }
}
