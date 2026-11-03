package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import com.tacz.guns.client.resource.pojo.display.gun.RangefinderManager;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears timestamps tied to a client world after SBM resets its animation clock. */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class ClientGunTimeResetEvent {
    private ClientGunTimeResetEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ScopeLensRenderer.close();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
            dataHolder.cancelShootFutures();
            dataHolder.resetAmmoCountCache();
        }
        GunTime.resetEpoch();
        LocalPlayerDataHolder.clientClickButtonTimestamp = -1L;
        RefitTransform.resetTimestamps();
        RenderCrosshairEvent.resetTimestamps();
        KillAmountOverlay.resetTimestamp();
        ClientHitMark.resetTimestamp();
        GunHudOverlay.resetTimestamp();
        FirstPersonRenderGunEvent.resetTimestamps();
        CameraSetupEvent.resetTimestamp();
        MuzzleFlashRender.resetTimestamps();
        MuzzleParticleManager.resetMuzzleLight();
        ThirdPersonMuzzleParticleManager.resetTimestamps();
        RangefinderManager.resetTimestamps();
        GunHurtBobTweak.resetTimestamp();
    }
}
