package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCancelReload;
import com.tacz.guns.network.message.ClientMessagePlayerReloadGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

public class LocalPlayerReload {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerReload(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void cancelReload() {
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof AbstractGunItem)) {
            return;
        }

        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(display -> {
            // 如果没在换弹，则返回
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
            ReloadState reloadState = gunOperator.getSynReloadState();
            if (!reloadState.getStateType().isReloading()) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCancelReload());
            // 执行本地取消换弹逻辑
            this.cancelReload(display);
        });
    }

    public void reload() {
        // 暂定只有主手可以装弹
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        ResourceLocation gunId = gunItem.getGunId(mainhandItem);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }
        TimelessAPI.getGunDisplay(mainhandItem).ifPresent(display -> {
            // 检查状态锁
            if (data.clientStateLock) {
                return;
            }
            // 弹药简单检查
            boolean canReload = gunItem.canReload(player, mainhandItem);
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !canReload) {
                return;
            }
            // 锁上状态锁
            data.lockState(operator -> operator.getSynReloadState().getStateType().isReloading());
            // 触发换弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerReloadGun());
            // 执行客户端 reload 相关内容
            this.doReload(gunItem, display, gunData, mainhandItem);
        });
    }

    private void doReload(IGun iGun, GunDisplayInstance display, GunData gunData, ItemStack mainhandItem) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            Bolt boltType = gunData.getBolt();
            boolean noAmmo;
            if (boltType == Bolt.OPEN_BOLT) {
                noAmmo = iGun.getCurrentAmmoCount(mainhandItem) <= 0;
            } else {
                noAmmo = !iGun.hasBulletInBarrel(mainhandItem);
            }
            // 触发 reload，停止播放声音
            SoundPlayManager.stopPlayGunSound();
            SoundPlayManager.playReloadSound(player, display, noAmmo);
            animationStateMachine.trigger(GunAnimationConstant.INPUT_RELOAD);
        }
    }

    private void cancelReload(GunDisplayInstance display) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            animationStateMachine.trigger(GunAnimationConstant.INPUT_CANCEL_RELOAD);
        }
    }
}
