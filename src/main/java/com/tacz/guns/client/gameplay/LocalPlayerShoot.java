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
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.ChargeData;
import com.tacz.guns.resource.pojo.data.gun.ChargeType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.Nullable;

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

    public boolean chargeShoot(boolean isCharging) {
        // 因为开火冷却检测用了特别定制的方法，所以不检查状态锁，而是手动检查是否换弹、切枪
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ItemStack mainHandItem = player.getMainHandItem();
        // 暂定为只有主手能开枪
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            data.chargeProgress = 0f;
            return false;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainHandItem).orElse(null);
        if (gunIndexOptional.isEmpty() || display == null) {
            return false;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        FireMode fireMode = iGun.getFireMode(mainHandItem);

        ChargeData chargeData = gunData.getChargeData(fireMode);
        if (chargeData == null) {
            return isCharging;
        }

        boolean canChargeDuringCooldown = chargeData.isChargeDuringCooldown() || getCoolDown(iGun, mainHandItem, gunData) < 50;
        boolean canCharge = canChargeDuringCooldown && preCheck(iGun, gunOperator, gunIndex, mainHandItem, display, gunData, isCharging) == null;
        float chargeProgress = data.chargeProgress;
        ChargeType type = chargeData.getChargeType();

        if (type == ChargeType.AUTO) {
            if (isCharging && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
                return data.chargeProgress >= chargeData.getMaxCharge();
            } else {
                data.isCharging = false;
                data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
            }
        } else if (type == ChargeType.HOLD) {
            if (isCharging && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
            } else {
                if (canChargeDuringCooldown && chargeProgress >= chargeData.getFireThreshold()) {
                    return true;
                }
                data.isCharging = false;
                data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
            }
        } else if (type == ChargeType.DELAY) {
            if ((isCharging || chargeProgress > 0) && canCharge) {
                data.isCharging = true;
                data.chargeProgress = Math.min(chargeProgress + chargeData.getIncreasePerTick(), chargeData.getMaxCharge());
                return data.chargeProgress >= chargeData.getMaxCharge();
            } else {
                data.isCharging = false;
                data.chargeProgress = Math.max(chargeProgress - chargeData.getDecreasePerTick(), 0f);
            }
        }
        return false;
    }

    public ShootResult shoot() {
        // 因为开火冷却检测用了特别定制的方法，所以不检查状态锁，而是手动检查是否换弹、切枪
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ItemStack mainHandItem = player.getMainHandItem();
        // 暂定为只有主手能开枪
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainHandItem).orElse(null);
        if (gunIndexOptional.isEmpty() || display == null) {
            return ShootResult.ID_NOT_EXIST;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        GunData gunData = gunIndex.getGunData();
        long coolDown = this.getCoolDown(iGun, mainHandItem, gunData);

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

        // 如果射击冷却大于等于 1 tick (即 50 ms)，则不允许开火
        if (coolDown >= 50) {
            return ShootResult.COOL_DOWN;
        }

        // 基础检查
        ShootResult result = preCheck(iGun, gunOperator, gunIndex, mainHandItem, display, gunData, true);
        if (result != null) {
            return result;
        }

        // 检查是否正在奔跑
        if (gunOperator.getSynSprintTime() > 0) {
            return ShootResult.IS_SPRINTING;
        }
        // 触发开火事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, mainHandItem, LogicalSide.CLIENT))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }
        // 切换状态锁，不允许换弹、检视等行为进行。
        data.lockState(SHOOT_LOCKED_CONDITION);
        data.isShootRecorded = false;
        // 调用开火逻辑
        float finalChargeProgress = data.chargeProgress;
        this.doShoot(display, iGun, mainHandItem, gunData, coolDown, finalChargeProgress);

        FireMode fireMode = iGun.getFireMode(mainHandItem);
        ChargeData chargeData = gunData.getChargeData(fireMode);
        if (chargeData != null) {
            if (chargeData.getChargeType() == ChargeType.DELAY) {
                data.chargeProgress = 0f;
            } else {
                data.chargeProgress = Math.max(0f, data.chargeProgress - chargeData.getDecreaseOnFire());
            }
        }

        return ShootResult.SUCCESS;
    }

    private @Nullable ShootResult preCheck(IGun iGun, IGunOperator gunOperator, ClientGunIndex gunIndex, ItemStack mainHandItem,
                                           GunDisplayInstance display, GunData gunData, boolean playDrySound) {
        // 按钮冷却时间未到，防止点击按钮后误触开火
        // 默认设置为 50 ms
        if (System.currentTimeMillis() - LocalPlayerDataHolder.clientClickButtonTimestamp < 50) {
            return ShootResult.COOL_DOWN;
        }

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
        // 是否为背包直读
        boolean useInventoryAmmo = iGun.useInventoryAmmo(mainHandItem);
        // 膛内是否有子弹
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
        // 是否还有子弹 (创造模式是否消耗背包备弹)
        boolean hasInventoryAmmo = iGun.hasInventoryAmmo(player, mainHandItem, gunOperator.needCheckAmmo()) || hasAmmoInBarrel;
        int ammoCount = iGun.getCurrentAmmoCount(mainHandItem) + (hasAmmoInBarrel ? 1 : 0);
        // 判断没有子弹的条件 (背包直读且包内没子弹 / 非背包直读且总子弹数 < 1)
        boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                !useInventoryAmmo && ammoCount < 1;
        if (noAmmo) {
            if (playDrySound) {
                SoundPlayManager.playDryFireSound(player, display);
            }
            return ShootResult.NO_AMMO;
        }
        //Handle Heat Data
        if(gunData.hasHeatData()) {
            if(iGun.isOverheatLocked(mainHandItem)) {
                if (playDrySound) {
                    SoundPlayManager.playDryFireSound(player, display);
                }
                return ShootResult.OVERHEATED;
            }
        }
        // 检查膛内子弹
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            IClientPlayerGunOperator.fromLocalPlayer(player).bolt();
            return ShootResult.NEED_BOLT;
        }
        return null;
    }

    private void doShoot(GunDisplayInstance display, IGun iGun, ItemStack mainHandItem, GunData gunData, long delay, float chargeProgress) {
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        Bolt boltType = gunData.getBolt();
        // 获取余弹数
        boolean consumeAmmo = IGunOperator.fromLivingEntity(player).consumesAmmoOrNot();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(mainHandItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = consumeAmmo ? iGun.getCurrentAmmoCount(mainHandItem) + (hasAmmoInBarrel ? 1 : 0) : Integer.MAX_VALUE;
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
            //Handle Heat Data
            if(gunData.hasHeatData()) {
                if(iGun.isOverheatLocked(mainHandItem)) {
                    ScheduledFuture<?> future = (ScheduledFuture<?>) Thread.currentThread();
                    future.cancel(false); // 取消当前任务
                    return;
                }
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
                // AUTO 模式由服务端 tick 驱动射击，不发送 per-bullet 包
                if (!LivingEntityShoot.isAutoShootMode(fireMode, iGun, mainHandItem)) {
                    NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot(data.clientShootTimestamp - data.clientBaseTimestamp, chargeProgress));
                }
            }

            // todo 需要检查
            // 播放声音和状态机触发需要从异步线程上传到主线程执行，否则会引起cme
            Minecraft.getInstance().submitAsync(() -> {
                // 触发击发事件
                boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(player, mainHandItem, LogicalSide.CLIENT));
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
                        SoundPlayManager.playSilenceSound(player, display, gunData);
                    } else {
                        SoundPlayManager.playShootSound(player, display, gunData);
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
            coolDown = gunData.getShootInterval(this.player, fireMode, mainHandItem) - (System.currentTimeMillis() - data.clientShootTimestamp);
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
