package com.tacz.guns.entity.shooter;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageGunStop;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.ShootBus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class LivingEntityShoot {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private static final ScheduledExecutorService SHOOT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Gun-AutoShoot-Scheduler");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> shootTask;
    public LivingEntityShoot(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
    }

    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, int count, boolean fromServer) {
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
        if (SyncConfig.SERVER_SHOOT_NETWORK_V.get() && !fromServer) {
            // 根据 tick time 和 允许的网络延迟波动 计算 时间戳的接受窗口 如果来自服务器则无需计算窗口可直接使用
            MinecraftServer server = Objects.requireNonNull(shooter.getServer());
            double tickTime = Math.max(server.tickTimes[server.getTickCount() % 100] * 1.0E-6D, 50);
            long alpha = System.currentTimeMillis() - data.baseTimestamp - timestamp;
            if (alpha < -300 || alpha > 300 + tickTime * 2) { // 允许 +- 300ms 的网络波动、窗口下限再扩大 2 个 tick time 时间(最坏情况射击会延迟2个 tick)
                if (shooter instanceof ServerPlayer player) {
                    NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ServerMessageSyncBaseTimestamp());
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
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }

        NetworkHandler.sendToTrackingEntity(new ServerMessageGunShoot(shooter.getId(), currentGunItem), shooter);
        data.lastShootTimestamp = data.shootTimestamp;
        data.heatTimestamp = System.currentTimeMillis();
        data.shootTimestamp = timestamp;
        // 执行枪械射击逻辑
        if (iGun instanceof AbstractGunItem logicGun) {
            logicGun.shoot(data, currentGunItem, pitch, yaw, shooter, count);
        }
        if(((IGun)shooter.getMainHandItem().getItem()).getFireMode(shooter.getMainHandItem()) == FireMode.AUTO) {
            ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
            api.setItemStack(currentGunItem);
            api.setShooter(shooter);
            api.setDataHolder(data);
            api.setPitchSupplier(pitch);
            api.setYawSupplier(yaw);
            if(!api.reduceAmmoOnce(true))
                NetworkHandler.sendToClientPlayer(new ServerMessageGunStop(shooter.getId()), (Player) shooter);
        }
        return ShootResult.SUCCESS;
    }
    public boolean startFullAuto(long timestamp) {
        UUID playerUuid = this.shooter.getUUID();
        if(this.shooter.getMainHandItem().getItem() instanceof IGun iGun) {
            int rpm = iGun.getRPM(this.shooter.getMainHandItem());
            double roundsPerSecond = rpm / 60.0;
            long intervalNanos = (long) (1_000_000_000.0 / roundsPerSecond);
            ScheduledFuture<?> task = SHOOT_SCHEDULER.scheduleAtFixedRate(
                    () -> ShootBus.addShot(playerUuid),
                    0, intervalNanos, TimeUnit.NANOSECONDS
            );
            shootTask = task;
            return true;
        }
        return false;
    }
    public boolean stopFullAuto() {
        return shootTask.cancel(true);
    }
    /**
     * 以当前时间戳查询射击冷却。返回值一般不会超过枪械的射击间隔
     * @return 射击冷却
     */
    public long getShootCoolDown() {
        return getShootCoolDown(System.currentTimeMillis() - data.baseTimestamp);
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
            shooter.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                    .map(cap -> abstractGunItem.findAndExtractInventoryAmmo(cap, itemStack, neededAmount));
        }
    }
}
