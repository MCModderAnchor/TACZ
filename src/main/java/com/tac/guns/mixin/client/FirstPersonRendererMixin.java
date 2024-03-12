package com.tac.guns.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.api.client.event.BeforeRenderHandEvent;
import com.tac.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class FirstPersonRendererMixin {
    @Shadow
    private float mainHandHeight;
    @Shadow
    private float oMainHandHeight;
    @Shadow
    private ItemStack mainHandItem;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource.BufferSource pBuffer, LocalPlayer pPlayerEntity, int pCombinedLight, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new BeforeRenderHandEvent(pMatrixStack));
    }

    @Inject(method = "tick",at = @At("RETURN"))
    public void cancelEquippedProgress(CallbackInfo ci){
        if(Minecraft.getInstance().player == null) {
            return;
        }
        ItemStack mainHandItemStack = Minecraft.getInstance().player.getMainHandItem();
        if(IGun.getIGunOrNull(mainHandItemStack) == null) {
            return;
        }
        mainHandItem = mainHandItemStack;
        mainHandHeight = 1.0f;
        oMainHandHeight = 0f;
    }
}
