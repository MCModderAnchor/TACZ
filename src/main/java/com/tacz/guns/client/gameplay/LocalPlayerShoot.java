package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class LocalPlayerShoot {
    private static final Predicate<IGunOperator> SHOOT_LOCKED_CONDITION = operator -> operator.getSynShootCoolDown() > 0;
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerShoot(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public ShootResult shoot() {
        // 按钮冷却时间未到，防止点击按钮后误触开火
        // 默认设置为 50 ms
        if (System.currentTimeMillis() - LocalPlayerDataHolder.clientClickButtonTimestamp < 50) {
            return ShootResult.COOL_DOWN;
        }
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if (!data.isShootRecorded) {
            return ShootResult.COOL_DOWN;
        }
        // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
        if (data.clientStateLock && data.lockedCondition != SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
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
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainhandItem).orElse(null);
        if (gunIndexOptional.isEmpty() || display == null) {
            return ShootResult.ID_NOT_EXIST;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        long coolDown = this.getCoolDown(iGun, mainhandItem, gunData);
        // 如果射击冷却大于等于 1 tick (即 50 ms)，则不允许开火
        if (coolDown >= 50) {
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
        // 检查是否正在拉栓
        if (gunOperator.getSynIsBolting()) {
            return ShootResult.IS_BOLTING;
        }
        // 判断是否处于近战冷却时间
        if (gunOperator.getSynMeleeCoolDown() != 0) {
            return ShootResult.IS_MELEE;
        }
        // 判断子弹数
        Bolt boltType = gunIndex.getGunData().getBolt();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainhandItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = iGun.getCurrentAmmoCount(mainhandItem) + (hasAmmoInBarrel ? 1 : 0);
        if (ammoCount < 1) {
            SoundPlayManager.playDryFireSound(player, display);
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
        data.lockState(SHOOT_LOCKED_CONDITION);
        data.isShootRecorded = false;
        // 调用开火逻辑
        this.doShoot(display, iGun, mainhandItem, gunData, coolDown);
        return ShootResult.SUCCESS;
    }

    private void doShoot(GunDisplayInstance display, IGun iGun, ItemStack mainhandItem, GunData gunData, long delay) {
        FireMode fireMode = iGun.getFireMode(mainhandItem);
        Bolt boltType = gunData.getBolt();
        // 获取余弹数
        boolean consumeAmmo = IGunOperator.fromLivingEntity(player).consumesAmmoOrNot();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainhandItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = consumeAmmo ? iGun.getCurrentAmmoCount(mainhandItem) + (hasAmmoInBarrel ? 1 : 0) : Integer.MAX_VALUE;
        // 连发射击间隔
        long period = fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1;
        // 最大连发数
        final int maxCount = Math.min(ammoCount, fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1);
        // 连发计数器
        AtomicInteger count = new AtomicInteger(0);
        LocalPlayerDataHolder.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            if (count.get() == 0) {
                // 转换 isRecord 状态，允许下一个tick的开火检测。
                data.isShootRecorded = true;
            }
            // 如果达到最大连发次数，或者玩家已经死亡，取消任务
            if (count.get() >= maxCount || player.isDeadOrDying()) {
                ScheduledFuture<?> future = (ScheduledFuture<?>) Thread.currentThread();
                future.cancel(false); // 取消当前任务
                return;
            }
            // 以下逻辑只需要执行一次
            if (count.get() == 0) {
                // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
                if (data.clientStateLock && data.lockedCondition != SHOOT_LOCKED_CONDITION && data.lockedCondition != null) {
                    return;
                }
                // 记录新的开火时间戳
                data.clientLastShootTimestamp = data.clientShootTimestamp;
                data.clientShootTimestamp = System.currentTimeMillis();
                // 发送开火的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot(data.clientShootTimestamp - data.clientBaseTimestamp));
            }

            // todo 需要检查
            // 播放声音和状态机触发需要从异步线程上传到主线程执行，否则会引起cme
            Minecraft.getInstance().submitAsync(() -> {
                // 触发击发事件
                boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(player, mainhandItem, LogicalSide.CLIENT));
                if (fire) {
                    // 动画和声音循环播放
                    AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
                    if (animationStateMachine != null) {
                        animationStateMachine.trigger(GunAnimationConstant.INPUT_SHOOT);
                    }
                    // 获取消音
                    final boolean useSilenceSound = this.useSilenceSound();
                    // 开火需要打断检视
                    SoundPlayManager.stopPlayGunSound(display, SoundManager.INSPECT_SOUND);
                    if (useSilenceSound) {
                        SoundPlayManager.playSilenceSound(player, display);
                    } else {
                        SoundPlayManager.playShootSound(player, display);
                    }
                }
            });

            count.getAndIncrement();
        }, delay, period, TimeUnit.MILLISECONDS);
    }

    private boolean useSilenceSound() {
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty != null) {
            Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
            return silence.right();
        }
        return false;
    }

    private long getCoolDown(IGun iGun, ItemStack mainHandItem, GunData gunData) {
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        long coolDown;
        if (fireMode == FireMode.BURST) {
            coolDown = (long) (gunData.getBurstData().getMinInterval() * 1000f) - (System.currentTimeMillis() - data.clientShootTimestamp);
        } else {
            coolDown = gunData.getShootInterval(this.player, fireMode) - (System.currentTimeMillis() - data.clientShootTimestamp);
        }
        return Math.max(coolDown, 0);
    }

    public long getClientShootCoolDown() {
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return -1;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndexOptional.map(commonGunIndex -> getCoolDown(iGun, mainHandItem, commonGunIndex.getGunData())).orElse(-1L);
    }
}
