package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerInspect {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerInspect(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void inspect() {
        // 暂定只有主手可以检视
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 检查状态锁
        if (data.clientStateLock) {
            return;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(mainhandItem)).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }
        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(gunIndex -> {
            Bolt boltType = gunData.getBolt();
            boolean noAmmo;
            if (boltType == Bolt.OPEN_BOLT) {
                noAmmo = iGun.getCurrentAmmoCount(mainhandItem) <= 0;
            } else {
                noAmmo = !iGun.hasBulletInBarrel(mainhandItem);
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
}
