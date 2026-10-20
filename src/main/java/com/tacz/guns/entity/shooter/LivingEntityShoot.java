package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.ChargeData;
import com.tacz.guns.resource.pojo.data.gun.ChargeType;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class LivingEntityShoot {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;

    public LivingEntityShoot(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
    }

    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp) {
        return shoot(pitch, yaw, timestamp, 0f, false);
    }

    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, float chargeProgress) {
        return shoot(pitch, yaw, timestamp, chargeProgress, true);
    }

    private ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, float chargeProgress, boolean hasChargeContext) {
        if (data.currentGunItem == null) {
            return ShootResult.NOT_DRAW;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.ID_NOT_EXIST;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        if (SyncConfig.SERVER_SHOOT_COOLDOWN_V.get()) {
            // 判断射击是否正在冷却
            long coolDown = getShootCoolDown(timestamp);
            if (coolDown == -1) {
                // 一般来说不太可能为 -1，原因未知
                return ShootResult.UNKNOWN_FAIL;
            }
            if (coolDown > 0) {
                return ShootResult.COOL_DOWN;
            }
        }
        if (SyncConfig.SERVER_SHOOT_NETWORK_V.get()) {
            // 根据 tick time 和 允许的网络延迟波动 计算 时间戳的接受窗口
            MinecraftServer server = Objects.requireNonNull(shooter.getServer());
            double tickTime = Math.max(server.getTickTimesNanos()[server.getTickCount() % 100] * 1.0E-6D, 50);
            long alpha = GunTime.nowMillis() - data.baseTimestamp - timestamp;
            if (alpha < -300 || alpha > 300 + tickTime * 2) { // 允许 +- 300ms 的网络波动、窗口下限再扩大 2 个 tick time 时间(最坏情况射击会延迟2个 tick)
                if (shooter instanceof ServerPlayer player) {
                    PacketDistributor.sendToPlayer(player, ServerMessageSyncBaseTimestamp.INSTANCE);
                }
                return ShootResult.NETWORK_FAIL;
            }
        }
        // 检查是否正在换弹
        if (data.reloadStateType.isReloading()) {
            return ShootResult.IS_RELOADING;
        }
        // 检查是否在切枪
        if (draw.getDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 检查是否在拉栓
        if (data.isBolting) {
            return ShootResult.IS_BOLTING;
        }
        // 检查是否在奔跑
        if (data.sprintTimeS > 0) {
            return ShootResult.IS_SPRINTING;
        }
        ChargeData chargeData = gunIndex.getGunData().getChargeData(iGun.getFireMode(currentGunItem));
        if (hasChargeContext && !isChargeProgressReasonable(chargeData, chargeProgress)) {
            return ShootResult.UNKNOWN_FAIL;
        }
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
        // 判断子弹数
        Bolt boltType = gunIndex.getGunData().getBolt();
        // 是否为背包直读
        boolean useInventoryAmmo = iGun.useInventoryAmmo(currentGunItem);
        // 膛内是否有子弹
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
        // 是否还有子弹 (创造模式是否消耗背包备弹)
        boolean hasInventoryAmmo = iGun.hasInventoryAmmo(shooter, currentGunItem, gunOperator.needCheckAmmo()) || hasAmmoInBarrel;
        int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (hasAmmoInBarrel ? 1 : 0);
        // 判断没有子弹的条件 (背包直读且包内没子弹 / 非背包直读且总子弹数 < 1)
        boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                !useInventoryAmmo && ammoCount < 1;
        if (noAmmo) {
            return ShootResult.NO_AMMO;
        }
        //Handle Heat Data
        if(gunIndex.getGunData().hasHeatData()) {
            if(iGun.isOverheatLocked(currentGunItem)) {
                return ShootResult.OVERHEATED;
            }
        }
        // 检查膛内子弹
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            return ShootResult.NEED_BOLT;
        }
        // 闭膛的膛内检查逻辑
        if (boltType == Bolt.CLOSED_BOLT && !hasAmmoInBarrel) {
            // 两种不同的上膛情况
            if (useInventoryAmmo) {
                consumeAmmoFromPlayer(1, currentGunItem, gunOperator.needCheckAmmo());
            } else {
                iGun.reduceCurrentAmmoCount(currentGunItem);
            }
            iGun.setBulletInBarrel(currentGunItem, true);
        }
        // 触发射击事件
        if (NeoForge.EVENT_BUS.post(new GunShootEvent(shooter, currentGunItem, LogicalSide.SERVER)).isCanceled()) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }

        NetworkHandler.sendToTrackingEntity(new ServerMessageGunShoot(shooter.getId(), currentGunItem), shooter);
        data.lastShootTimestamp = data.shootTimestamp;
        data.heatTimestamp = GunTime.nowMillis();
        data.shootTimestamp = timestamp;
        data.chargeProgress = validateChargeProgress(chargeData, chargeProgress, hasChargeContext);
        // 执行枪械射击逻辑
        if (iGun instanceof AbstractGunItem logicGun) {
            logicGun.shoot(data, currentGunItem, pitch, yaw, shooter);
        }
        return ShootResult.SUCCESS;
    }

    // 简单校验，服务端不追踪扳机按住状态，所以只拒绝超过“客户端一直按住蓄力”时理论可达到的最大进度。
    private boolean isChargeProgressReasonable(ChargeData chargeData, float chargeProgress) {
        final float tolerance = 0.001f;
        if (!Float.isFinite(chargeProgress)) {
            return false;
        }
        if (chargeData == null) {
            return Math.abs(chargeProgress) <= tolerance;
        }
        if (chargeProgress < -tolerance) {
            return false;
        }
        float minimumProgress = Math.min(chargeData.getFireThreshold(), chargeData.getMaxCharge());
        if (chargeProgress + tolerance < minimumProgress) {
            return false;
        }
        if (chargeProgress > getMaxReasonableChargeProgress(chargeData) + tolerance) {
            return false;
        }
        return true;
    }

    private float getMaxReasonableChargeProgress(ChargeData chargeData) {
        // 预留少量 tick 余量，用于容忍网络抖动和客户端/服务端调度偏差。
        final float extraTicks = 4f;
        float startProgress = getChargeProgressAfterLastFire(chargeData);
        float elapsedTicks = Math.max(getChargeElapsedMillis() / 50f, 0f) + extraTicks;
        float maxProgress = startProgress + elapsedTicks * Math.max(chargeData.getIncreasePerTick(), 0f);
        return Math.min(maxProgress, chargeData.getMaxCharge());
    }

    private float getChargeProgressAfterLastFire(ChargeData chargeData) {
        if (data.shootTimestamp < 0) {
            return 0f;
        }
        // delay 蓄力模式在客户端开火后总是重置。
        if (chargeData.getChargeType() == ChargeType.DELAY) {
            return 0f;
        }
        return Math.max(0f, data.chargeProgress - chargeData.getDecreaseOnFire());
    }

    private long getChargeElapsedMillis() {
        if (data.shootTimestamp >= 0) {
            long startTimestamp = data.baseTimestamp + data.shootTimestamp;
            return GunTime.nowMillis() - startTimestamp;
        }
        if (data.drawTimestamp >= 0) {
            return GunTime.nowMillis() - data.drawTimestamp;
        }
        return 0L;
    }

    private float validateChargeProgress(ChargeData chargeData, float chargeProgress, boolean hasChargeContext) {
        if (!hasChargeContext || !Float.isFinite(chargeProgress)) {
            return 0f;
        }
        if (chargeData == null) {
            return 0f;
        }
        return Math.max(0f, Math.min(chargeProgress, chargeData.getMaxCharge()));
    }

    /**
     * 以当前时间戳查询射击冷却。返回值一般不会超过枪械的射击间隔
     * @return 射击冷却
     */
    public long getShootCoolDown() {
        return getShootCoolDown(GunTime.nowMillis() - data.baseTimestamp);
    }

    /**
     * 查询指定的 timestamp 下的射击冷却。根据情况返回值可能超过枪械的射击间隔。
     * @param timestamp 指定 timestamp，是偏移时间戳（基于base timestamp 的相对时间戳）
     * @return 射击冷却
     */
    public long getShootCoolDown(long timestamp) {
        if (data.currentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        FireMode fireMode = iGun.getFireMode(currentGunItem);
        long interval = timestamp - data.shootTimestamp;
        if (fireMode == FireMode.BURST) {
            return gunIndex.map(index -> {
                long coolDown = (long) (index.getGunData().getBurstData().getMinInterval() * 1000f) - interval;
                // 给 5 ms 的窗口时间，以平衡延迟
                coolDown = coolDown - 5;
                return Math.max(coolDown, 0L);
            }).orElse(-1L);
        }
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval(this.shooter, fireMode, currentGunItem) - interval;
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            return Math.max(coolDown, 0L);
        }).orElse(-1L);
    }

    /**
     * 消耗备弹 TODO: 需要检查，是否有其他更简单的方法消耗背包内的弹药 (这段是直接从逻辑机 API 里复制过来的)
     */
    public void consumeAmmoFromPlayer(int neededAmount, ItemStack itemStack, boolean needCheckAmmo) {
        if (!(itemStack.getItem() instanceof AbstractGunItem abstractGunItem)) {
            return;
        }
        // 如果处于创造模式不消耗的情况
        if (!needCheckAmmo) {
            return;
        }
        if (abstractGunItem.useDummyAmmo(itemStack)) {
            abstractGunItem.findAndExtractDummyAmmo(itemStack, neededAmount);
        } else {
            Optional.ofNullable(shooter.getCapability(Capabilities.ItemHandler.ENTITY, null))
                    .map(cap -> abstractGunItem.findAndExtractInventoryAmmo(cap, itemStack, neededAmount));
        }
    }
}
