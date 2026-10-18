package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;

public class LocalPlayerDataHolder {
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(2);
    private final Set<ScheduledFuture<?>> shootFutures = ConcurrentHashMap.newKeySet();
    private static final long AMMO_COUNT_CACHE_MILLIS = 50L;
    private static final long STATE_LOCK_TIMEOUT_MILLIS = 250L;
    private static final Predicate<IGunOperator> TIMED_LOCKED_CONDITION = operator -> false;
    /** 主动卸弹后暂停自动换弹，主动换弹、尝试开火或切枪时解除。 */
    public boolean autoReloadSuppressed = false;
    private ItemStack cachedAmmoGun;
    private long cachedAmmoTimestamp = -1L;
    private long cachedAmmoEpoch = -1L;
    private int cachedAmmoCount;
    public long clientBaseTimestamp = -1L;
    /**
     * 上一个 tick 的瞄准进度，用于插值，范围 0 ~ 1
     */
    public static float oldAimingProgress = 0;
    /**
     * 点击按钮的时间戳，防止客户端点击按钮后误触开火
     */
    public static long clientClickButtonTimestamp = -1L;
    /**
     * 玩家对象
     */
    private final LocalPlayer player;
    /**
     * 与射击有关的几个变量
     */
    public volatile long clientShootTimestamp = -1L;
    public volatile long clientLastShootTimestamp = -1L;
    public volatile boolean isShootRecorded = true;
    public float chargeProgress = 0f;
    public boolean isCharging = false;
    /**
     * 这个状态锁表示：任意时刻，正在进行的枪械操作只能为一个。
     * 主要用于防止客户端操作表现效果重复执行。
     */
    public volatile boolean clientStateLock = false;
    public volatile boolean clientReloadStarting = false;
    /**
     * 用于标记 bolt 是否已经执行完成，防止因为客户端、服务端异步产生的数据不同步而造成的重复 bolt
     */
    public boolean isBolting = false;
    /**
     * 瞄准的进度，范围 0 ~ 1
     */
    public float clientAimingProgress = 0;
    /**
     * 瞄准时间戳，单位 ms
     */
    public long clientAimingTimestamp = -1L;
    public boolean clientIsAiming = false;
    /**
     * 切枪时间戳，在切枪开始时更新，单位 ms。
     * 在客户端仅用于计算收枪动画的时长和过渡时长。
     */
    public long clientDrawTimestamp = -1L;
    /**
     * 异步切枪
     */
    @Nullable
    public ScheduledFuture<?> drawFuture = null;
    /**
     * 用于等待上锁的服务端响应
     */
    @Nullable
    public Predicate<IGunOperator> lockedCondition = null;
    /**
     * 计算上锁响应时间，不允许超过最大响应时间，避免死锁
     */
    public long lockTimestamp = -1;

    public LocalPlayerDataHolder(LocalPlayer player) {
        this.player = player;
    }

    public int getCachedAmmoCount(ItemStack gun, IGun iGun) {
        long epoch = GunTime.epoch();
        long now = GunTime.nowMillis();
        long elapsed = now - cachedAmmoTimestamp;
        if (gun != cachedAmmoGun || epoch != cachedAmmoEpoch || cachedAmmoTimestamp < 0
                || elapsed < 0 || elapsed >= AMMO_COUNT_CACHE_MILLIS) {
            cachedAmmoCount = ClientAmmoCount.get(player, gun, iGun);
            cachedAmmoGun = gun;
            cachedAmmoTimestamp = now;
            cachedAmmoEpoch = epoch;
        }
        return cachedAmmoCount;
    }

    public void resetAmmoCountCache() {
        cachedAmmoGun = null;
        cachedAmmoTimestamp = -1L;
        cachedAmmoEpoch = -1L;
        cachedAmmoCount = 0;
    }

    /**
     * 锁上状态锁
     */
    public void lockState(@Nullable Predicate<IGunOperator> lockedCondition) {
        clientStateLock = true;
        lockTimestamp = GunTime.nowMillis();
        this.lockedCondition = lockedCondition;
    }

    /** 复用状态锁的响应超时窗口，为瞬时操作持锁 250 ms。 */
    public void lockState() {
        lockState(TIMED_LOCKED_CONDITION);
    }

    /** 射击、蓄力等允许绕过普通状态锁的入口仍需遵守此限时锁。 */
    public boolean isTimedStateLocked() {
        return clientStateLock && lockedCondition == TIMED_LOCKED_CONDITION;
    }

    /**
     * 此方法每 tick 执行一次，判断是否应当释放状态锁。
     */
    public void tickStateLock() {
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ReloadState reloadState = gunOperator.getSynReloadState();
        // 如果还没完成上锁，则不能释放状态锁
        // 上锁允许的最大响应时间，毫秒
        long lockTime = GunTime.nowMillis() - lockTimestamp;
        if (lockTime >= 0 && lockTime < STATE_LOCK_TIMEOUT_MILLIS
                && lockedCondition != null && !lockedCondition.test(gunOperator)) {
            return;
        }
        lockedCondition = null;
        if (reloadState.getStateType().isReloading()) {
            clientReloadStarting = true;
            return;
        }
        long shootCoolDown = gunOperator.getSynShootCoolDown();
        if (shootCoolDown > 0) {
            return;
        }
        if (gunOperator.getSynDrawCoolDown() > 0) {
            return;
        }
        if (gunOperator.getSynIsBolting()) {
            return;
        }
        if (gunOperator.getSynMeleeCoolDown() > 0) {
            return;
        }
        // 释放状态锁
        clientStateLock = false;
        clientReloadStarting = false;
    }

    public void registerShootFuture(ScheduledFuture<?> future) {
        shootFutures.add(future);
    }

    public boolean hasPendingShoot() {
        // 完成/取消与注册可能来自不同线程，不让残留的 future 永久阻止卸弹。
        shootFutures.removeIf(ScheduledFuture::isDone);
        return !shootFutures.isEmpty();
    }

    public void unregisterShootFuture(ScheduledFuture<?> future) {
        shootFutures.remove(future);
    }

    public void cancelShootFutures() {
        shootFutures.forEach(future -> future.cancel(false));
        shootFutures.clear();
    }

    /**
     * 重生后各种参数的重置
     */
    public void reset() {
        cancelShootFutures();
        resetAmmoCountCache();
        lockedCondition = null;
        lockTimestamp = -1L;
        autoReloadSuppressed = false;
        // 重置客户端的 shoot 时间戳
        isShootRecorded = true;
        clientShootTimestamp = -1;
        chargeProgress = 0f;
        isCharging = false;
        // 重置客户端瞄准状态
        clientIsAiming = false;
        clientAimingProgress = 0;
        oldAimingProgress = 0;
        // 重置拉栓状态
        isBolting = false;
        clientReloadStarting = false;
        // 打开状态锁
        clientStateLock = false;
    }
}
