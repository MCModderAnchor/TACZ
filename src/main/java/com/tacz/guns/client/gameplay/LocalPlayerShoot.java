package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LocalPlayerShoot {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerShoot(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public ShootResult shoot() {
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if (!data.isShootRecorded) {
            return ShootResult.COOL_DOWN;
        }
        // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
        if (data.clientStateLock && data.lockedCondition != LocalPlayerDataHolder.SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
            data.isShootRecorded = true;
            // 因为这块主要目的是防止切枪后开火动作覆盖切枪动作，返回 IS_DRAWING
            return ShootResult.IS_DRAWING;
        }
        // 暂定为只有主手能开枪
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.ID_NOT_EXIST;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        long coolDown = this.getCoolDown(iGun, mainhandItem, gunData);
        // 如果射击冷却大于 1 tick (即 50 ms)，则不允许开火
        if (coolDown > 50) {
            return ShootResult.COOL_DOWN;
        }
        // 因为开火冷却检测用了特别定制的方法，所以不检查状态锁，而是手动检查是否换弹、切枪
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        // 检查是否正在换弹
        if (gunOperator.getSynReloadState().getStateType().isReloading()) {
            return ShootResult.IS_RELOADING;
        }
        // 检查是否正在切枪
        if (gunOperator.getSynDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 判断子弹数
        Bolt boltType = gunIndex.getGunData().getBolt();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainhandItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = iGun.getCurrentAmmoCount(mainhandItem) + (hasAmmoInBarrel ? 1 : 0);
        if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && ammoCount < 1) {
            SoundPlayManager.playDryFireSound(player, gunIndex);
            return ShootResult.NO_AMMO;
        }
        // 判断膛内子弹
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            IClientPlayerGunOperator.fromLocalPlayer(player).bolt();
            return ShootResult.NEED_BOLT;
        }
        // 检查是否正在奔跑
        if (gunOperator.getSynSprintTime() > 0) {
            return ShootResult.IS_SPRINTING;
        }
        // 触发开火事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, mainhandItem, LogicalSide.CLIENT))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }
        // 切换状态锁，不允许换弹、检视等行为进行。
        data.lockState(LocalPlayerDataHolder.SHOOT_LOCKED_CONDITION);
        data.isShootRecorded = false;
        // 开火效果需要延时执行，这样渲染效果更好。
        LocalPlayerDataHolder.SCHEDULED_EXECUTOR_SERVICE.schedule(() -> this.doShoot(gunIndex, mainhandItem, gunData), coolDown, TimeUnit.MILLISECONDS);
        return ShootResult.SUCCESS;
    }

    private void doShoot(ClientGunIndex gunIndex, ItemStack mainhandItem, GunData gunData) {
        // 转换 isRecord 状态，允许下一个tick的开火检测。
        data.isShootRecorded = true;
        // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
        if (data.clientStateLock && data.lockedCondition != LocalPlayerDataHolder.SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
            data.isShootRecorded = true;
            return;
        }
        // 记录新的开火时间戳
        data.clientShootTimestamp = System.currentTimeMillis();
        // 发送开火的数据包，通知服务器。暂时只考虑主手能打枪。
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
        // 动画状态机转移状态
        GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
        if (animationStateMachine != null) {
            animationStateMachine.onGunShoot();
        }
        // 获取消音
        boolean useSilenceSound = this.useSilenceSound(mainhandItem, gunData);
        // 播放声音需要从异步线程上传到主线程执行。
        Minecraft.getInstance().submitAsync(() -> {
            // 开火需要打断检视
            SoundPlayManager.stopPlayGunSound(gunIndex, SoundManager.INSPECT_SOUND);
            if (useSilenceSound) {
                SoundPlayManager.playSilenceSound(player, gunIndex);
            } else {
                SoundPlayManager.playShootSound(player, gunIndex);
            }
        });
    }

    private boolean useSilenceSound(ItemStack mainhandItem, GunData gunData) {
        final boolean[] useSilenceSound = new boolean[]{false};
        AttachmentDataUtils.getAllAttachmentData(mainhandItem, gunData, attachmentData -> {
            if (attachmentData.getSilence() != null && attachmentData.getSilence().isUseSilenceSound()) {
                useSilenceSound[0] = true;
            }
        });
        return useSilenceSound[0];
    }

    private long getCoolDown(IGun iGun, ItemStack mainHandItem, GunData gunData) {
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        long coolDown;
        if (fireMode == FireMode.BURST) {
            coolDown = gunData.getBurstShootInterval() - (System.currentTimeMillis() - data.clientShootTimestamp);
        } else {
            coolDown = gunData.getShootInterval() - (System.currentTimeMillis() - data.clientShootTimestamp);
        }
        return Math.max(coolDown, 0);
    }

    public long getClientShootCoolDown() {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return -1;
        }
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (fireMode == FireMode.BURST) {
            return gunIndexOptional.map(gunIndex -> gunIndex.getGunData().getBurstShootInterval() - (System.currentTimeMillis() - data.clientShootTimestamp)).orElse(-1L);
        }
        return gunIndexOptional.map(gunIndex -> gunIndex.getGunData().getShootInterval() - (System.currentTimeMillis() - data.clientShootTimestamp)).orElse(-1L);
    }
}
