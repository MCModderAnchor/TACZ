package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerBoltGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerBolt {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerBolt(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void bolt() {
        // 检查状态锁
        if (data.clientStateLock) {
            return;
        }
        if (data.isBolting) {
            return;
        }
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(mainhandItem)).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }

        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(display -> {
            // 检查 bolt 类型是否是 manual action
            Bolt boltType = gunData.getBolt();
            if (boltType != Bolt.MANUAL_ACTION) {
                return;
            }
            // 检查是否有弹药在枪膛内
            if (iGun.hasBulletInBarrel(mainhandItem)) {
                return;
            }
            // 检查弹匣内是否有子弹
            if (iGun.getCurrentAmmoCount(mainhandItem) == 0) {
                return;
            }
            // 锁上状态锁
            data.lockState(IGunOperator::getSynIsBolting);
            data.isBolting = true;
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerBoltGun());
            // 播放动画和音效
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                SoundPlayManager.playBoltSound(player, display);
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BOLT);
            }
        });
    }

    public void tickAutoBolt() {
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            data.isBolting = false;
            return;
        }
        bolt();
        if (data.isBolting) {
            // 对于客户端来说，膛内弹药被填入的状态同步到客户端的瞬间，bolt 过程才算完全结束
            if (iGun.hasBulletInBarrel(mainhandItem)) {
                data.isBolting = false;
            }
        }
    }
}
