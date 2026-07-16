package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class LocalPlayerInspect {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerInspect(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void inspect() {
        // 暂定只有主手可以检视
        ItemStack mainHandItem = player.getMainHandItem();

        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            if (IClientItemExtensions.of(mainHandItem).getCustomRenderer() instanceof AnimateGeoItemRenderer<?,?> renderer) {
                renderer.triggerAnimation(mainHandItem, GunAnimationConstant.INPUT_INSPECT);
            }
            return;
        }
        // 检查状态锁
        if (data.clientStateLock) {
            return;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(mainHandItem)).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }
        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(gunIndex -> {
            Bolt boltType = gunData.getBolt();
            boolean noAmmo;
            if (boltType == Bolt.OPEN_BOLT) {
                noAmmo = iGun.getCurrentAmmoCount(mainHandItem) <= 0;
            } else {
                noAmmo = !iGun.hasBulletInBarrel(mainHandItem);
            }
            // 触发 inspect，停止播放声音
            SoundPlayManager.stopPlayGunSound();
            SoundPlayManager.playInspectSound(player, gunIndex, noAmmo);
            var animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_INSPECT);
            }
        });
    }

    public static void cancelInspect(LocalPlayer player) {
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun)) {
            return;
        }
        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(LocalPlayerInspect::cancelInspect);
    }

    private static void cancelInspect(GunDisplayInstance display) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            animationStateMachine.trigger(GunAnimationConstant.INPUT_INSPECT_RETREAT);
        }
        SoundPlayManager.stopPlayGunSound(display, SoundManager.INSPECT_SOUND);
        SoundPlayManager.stopPlayGunSound(display, SoundManager.INSPECT_EMPTY_SOUND);
    }
}
