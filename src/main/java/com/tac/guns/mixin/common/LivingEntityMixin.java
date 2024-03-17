package com.tac.guns.mixin.common;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunFireSelectEvent;
import com.tac.guns.api.event.GunReloadEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IAmmoBox;
import com.tac.guns.api.item.IGun;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.entity.serializer.ModEntityDataSerializers;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.gun.GunData;
import com.tac.guns.resource.pojo.data.gun.GunReloadData;
import com.tac.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.CapabilityItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.tac.guns.resource.DefaultAssets.SHOOT_SOUND;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IGunOperator {
    @Unique
    private static final EntityDataAccessor<Long> DATA_SHOOT_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.LONG);
    @Unique
    private static final EntityDataAccessor<ReloadState> DATA_RELOAD_STATE_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.RELOAD_STATE);
    @Unique
    private static final EntityDataAccessor<Float> DATA_AIMING_PROGRESS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Long> DATA_DRAW_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.LONG);
    /**
     * 射击时间戳，射击成功时更新，单位 ms。
     * 用于计算射击的冷却时间。
     */
    @Unique
    private long tac$ShootTimestamp = -1L;
    /**
     * 切枪时间戳，在切枪开始时更新，单位 ms。
     * 用于计算切枪进度。切枪进度完成后，才能进行各种操作。
     */
    @Unique
    private long tac$DrawTimestamp = -1L;
    /**
     * 瞄准的进度，范围 0 ~ 1
     */
    @Unique
    private float tac$AimingProgress = 0;
    /**
     * 瞄准时间戳，在每个 tick 更新，单位 ms。
     * 用于在每个 tick 计算: 距离上一次更新 aimingProgress 的时长，并依此计算 aimingProgress 的增量。
     */
    @Unique
    private long tac$AimingTimestamp = -1L;
    /**
     * 为 true 时表示正在 执行瞄准 状态，aimingProgress 会在每个 tick 叠加，
     * 为 false 时表示正在 取消瞄准 状态，aimingProgress 会在每个 tick 递减。
     */
    @Unique
    private boolean tac$IsAiming = false;
    /**
     * 装弹时间戳，在开始装弹的瞬间更新，单位 ms。
     * 用于在每个 tick 计算: 从开始装弹 到 当前时间点 的时长，并依此计算出换弹的状态和冷却。
     */
    @Unique
    private long tac$ReloadTimestamp = -1;
    /**
     * 装填状态的缓存。会在每个 tick 进行更新。
     */
    @Unique
    @Nonnull
    private ReloadState.StateType tac$ReloadStateType = ReloadState.StateType.NOT_RELOADING;
    /**
     * 缓存实体当前操作的枪械物品。在切枪时 (draw 方法) 更新。
     */
    @Unique
    @Nullable
    private ItemStack tac$CurrentGunItem = null;
    /**
     * 用来记录子弹击退能力，负数表示使用原版击退
     */
    @Unique
    private double tac$KnockbackStrength = -1;
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Shadow
    public abstract <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing);

    @Override
    @Unique
    public long getSynShootCoolDown() {
        return this.getEntityData().get(DATA_SHOOT_COOL_DOWN_ID);
    }

    @Override
    @Unique
    public long getSynDrawCoolDown() {
        return this.getEntityData().get(DATA_DRAW_COOL_DOWN_ID);
    }

    @Override
    @Unique
    public ReloadState getSynReloadState() {
        return this.getEntityData().get(DATA_RELOAD_STATE_ID);
    }

    @Override
    @Unique
    public float getSynAimingProgress() {
        return this.getEntityData().get(DATA_AIMING_PROGRESS_ID);
    }

    @Unique
    private long getShootCoolDown() {
        if (tac$CurrentGunItem == null) {
            return 0;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval() - (System.currentTimeMillis() - tac$ShootTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
    }

    @Unique
    private long getDrawCoolDown() {
        if (tac$CurrentGunItem == null) {
            return 0;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = (long) (index.getGunData().getDrawTime() * 1000) - (System.currentTimeMillis() - tac$DrawTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
    }

    @Unique
    @Override
    public void draw(ItemStack itemStack) {
        // 重置各个状态
        tac$CurrentGunItem = itemStack;
        tac$ShootTimestamp = -1;
        tac$IsAiming = false;
        tac$AimingProgress = 0;
        tac$ReloadTimestamp = -1;
        tac$ReloadStateType = ReloadState.StateType.NOT_RELOADING;

        if (IGun.getIGunOrNull(itemStack) == null) {
            tac$DrawTimestamp = -1;
            // TODO 执行收枪逻辑
        } else {
            tac$DrawTimestamp = System.currentTimeMillis();
        }
    }

    @Unique
    @Override
    public void reload() {
        if (tac$CurrentGunItem == null) {
            return;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查换弹是否还未完成
            if (tac$ReloadStateType.isReloading()) {
                return;
            }
            // 检查是否正在开火冷却
            if (getShootCoolDown() != 0) {
                return;
            }
            // 检查是否在切枪
            if (getDrawCoolDown() != 0) {
                return;
            }
            int currentAmmoCount = iGun.getCurrentAmmoCount(tac$CurrentGunItem);
            // 检查弹药
            if (this.needCheckAmmo()) {
                // 超出或达到上限，不换弹
                if (currentAmmoCount >= gunIndex.getGunData().getAmmoAmount()) {
                    return;
                }
                boolean hasAmmo = this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(cap -> {
                    // 背包检查
                    for (int i = 0; i < cap.getSlots(); i++) {
                        ItemStack checkAmmoStack = cap.getStackInSlot(i);
                        if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(tac$CurrentGunItem, checkAmmoStack)) {
                            return true;
                        }
                        if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(tac$CurrentGunItem, checkAmmoStack)) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
                if (!hasAmmo) {
                    return;
                }
            }
            // 触发装弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
                return;
            }
            // 空仓换弹，初始化用于 tick 的状态
            if (currentAmmoCount <= 0) {
                tac$ReloadStateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                tac$ReloadTimestamp = System.currentTimeMillis();
                return;
            }
            // 战术换弹，初始化用于 tick 的状态
            if (currentAmmoCount <= gunIndex.getGunData().getAmmoAmount()) {
                tac$ReloadStateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
                tac$ReloadTimestamp = System.currentTimeMillis();
            }
        });
    }

    @Unique
    @Override
    public ShootResult shoot(float pitch, float yaw) {
        if (tac$CurrentGunItem == null) {
            return ShootResult.FAIL;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return ShootResult.FAIL;
        }
        // 判断射击是否正在冷却
        long coolDown = getShootCoolDown();
        if (coolDown == -1) {
            return ShootResult.FAIL;
        }
        if (coolDown > 0) {
            return ShootResult.COOL_DOWN;
        }
        // 检查是否正在换弹
        if (tac$ReloadStateType.isReloading()) {
            return ShootResult.FAIL;
        }
        // 检查是否在切枪
        if (getDrawCoolDown() != 0) {
            return ShootResult.FAIL;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        // 创造模式不判断子弹数
        if (needCheckAmmo() && iGun.getCurrentAmmoCount(tac$CurrentGunItem) < 1) {
            return ShootResult.NO_AMMO;
        }
        // 触发射击事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FAIL;
        }
        // TODO 判断枪械是否有足够的弹药
        // 调用射击方法
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        LivingEntity shooter = (LivingEntity) (Object) this;
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // TODO 获取 GunData 并根据其中的弹道参数创建 EntityBullet
            Level world = shooter.getLevel();
            EntityBullet bullet = new EntityBullet(world, shooter, gunIndex.getGunData().getAmmoId());
            InaccuracyType inaccuracyState = InaccuracyType.getInaccuracyType(shooter);
            float inaccuracy = gunIndex.getGunData().getInaccuracy(inaccuracyState);
            bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, 10f, inaccuracy);
            world.addFreshEntity(bullet);
            // 播放声音
            // TODO 配置文件决定衰减距离
            NetworkHandler.sendSoundToNearby(shooter, 64, gunId, SHOOT_SOUND, 1.0f, 1.0f);
            // 削减弹药数
            if (this.needCheckAmmo()) {
                iGun.reduceCurrentAmmoCount(tac$CurrentGunItem);
            }
        });
        tac$ShootTimestamp = System.currentTimeMillis();
        return ShootResult.SUCCESS;
    }

    @Unique
    @Override
    public boolean needCheckAmmo() {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player) {
            return !player.isCreative();
        }
        return true;
    }

    @Unique
    @Override
    public void aim(boolean isAim) {
        tac$IsAiming = isAim;
    }

    @Unique
    @Override
    public FireMode fireSelect() {
        if (tac$CurrentGunItem == null) {
            return FireMode.UNKNOWN;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return FireMode.UNKNOWN;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
            return FireMode.UNKNOWN;
        }
        // 应用切换逻辑
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> {
            FireMode fireMode = iGun.getFireMode(tac$CurrentGunItem);
            List<FireMode> fireModeSet = gunIndex.getGunData().getFireModeSet();
            // 即使玩家拿的是没有的 FireMode，这里也能切换到正常情况
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            iGun.setFireMode(tac$CurrentGunItem, nextFireMode);
            return nextFireMode;
        }).orElse(FireMode.UNKNOWN);
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTickServerSide(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // 如果为客户端调用，返回。
        if (entity.getLevel().isClientSide()) {
            return;
        }
        // 完成各种 tick 任务
        ReloadState reloadState = tickReloadState();
        tickAimingProgress();
        // 从服务端同步数据
        this.getEntityData().set(DATA_SHOOT_COOL_DOWN_ID, getShootCoolDown());
        this.getEntityData().set(DATA_DRAW_COOL_DOWN_ID, getDrawCoolDown());
        this.getEntityData().set(DATA_RELOAD_STATE_ID, reloadState);
        this.getEntityData().set(DATA_AIMING_PROGRESS_ID, tac$AimingProgress);
    }

    @Unique
    private void tickAimingProgress() {
        // currentGunItem 如果为 null，则取消瞄准状态并将 aimingProgress 归零。
        if (tac$CurrentGunItem == null || !(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            tac$AimingProgress = 0;
            tac$AimingTimestamp = System.currentTimeMillis();
            return;
        }
        // 如果获取不到 gunIndex，则取消瞄准状态并将 aimingProgress 归零，返回。
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            tac$AimingProgress = 0;
            return;
        }
        float aimTime = gunIndexOptional.get().getGunData().getAimTime();
        float alphaProgress = (System.currentTimeMillis() - tac$AimingTimestamp + 1) / (aimTime * 1000);
        if (tac$IsAiming) {
            // 处于执行瞄准状态，增加 aimingProgress
            tac$AimingProgress += alphaProgress;
            if (tac$AimingProgress > 1) {
                tac$AimingProgress = 1;
            }
        } else {
            // 处于取消瞄准状态，减小 aimingProgress
            tac$AimingProgress -= alphaProgress;
            if (tac$AimingProgress < 0) {
                tac$AimingProgress = 0;
            }
        }
        tac$AimingTimestamp = System.currentTimeMillis();
    }

    @Unique
    private ReloadState tickReloadState() {
        LivingEntity entity = (LivingEntity) (Object) this;
        // 初始化 tick 返回值
        ReloadState reloadState = new ReloadState();
        reloadState.setStateType(ReloadState.StateType.NOT_RELOADING);
        reloadState.setCountDown(ReloadState.NOT_RELOADING_COUNTDOWN);
        // 判断是否正在进行装填流程。如果没有则返回。
        if (tac$ReloadTimestamp == -1 || tac$CurrentGunItem == null) {
            return reloadState;
        }
        if (!(tac$CurrentGunItem.getItem() instanceof IGun iGun)) {
            return reloadState;
        }
        // 获取当前枪械的 ReloadData。如果没有则返回。
        ResourceLocation gunId = iGun.getGunId(tac$CurrentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return reloadState;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        GunReloadData reloadData = gunData.getReloadData();
        // 计算新的 stateType 和 countDown
        long countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
        ReloadState.StateType stateType = tac$ReloadStateType;
        long progressTime = System.currentTimeMillis() - tac$ReloadTimestamp;
        if (stateType.isReloadingEmpty()) {
            long feedTime = (long) (reloadData.getFeed().getEmptyTime() * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getEmptyTime() * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                tac$ReloadTimestamp = -1;
            }
        } else if (stateType.isReloadingTactical()) {
            long feedTime = (long) (reloadData.getFeed().getTacticalTime() * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getTacticalTime() * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                tac$ReloadTimestamp = -1;
            }
        }
        // 更新枪内弹药
        if (tac$ReloadStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.EMPTY_RELOAD_FINISHING) {
                iGun.setCurrentAmmoCount(tac$CurrentGunItem, getAndExtractNeedAmmoCount(iGun, gunData.getAmmoAmount()));
                entity.sendMessage(new TranslatableComponent("message.tac.reload.success"), Util.NIL_UUID);
            }
        }
        if (tac$ReloadStateType == ReloadState.StateType.TACTICAL_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.TACTICAL_RELOAD_FINISHING) {
                iGun.setCurrentAmmoCount(tac$CurrentGunItem, getAndExtractNeedAmmoCount(iGun, gunData.getAmmoAmount()));
                entity.sendMessage(new TranslatableComponent("message.tac.reload.success"), Util.NIL_UUID);
            }
        }
        // 更新换弹状态缓存
        tac$ReloadStateType = stateType;
        // 返回 tick 结果
        reloadState.setStateType(stateType);
        reloadState.setCountDown(countDown);
        return reloadState;
    }

    @Unique
    private int getAndExtractNeedAmmoCount(IGun iGun, int maxAmmoCount) {
        int currentAmmoCount = iGun.getCurrentAmmoCount(tac$CurrentGunItem);
        if (this.needCheckAmmo()) {
            return this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(cap -> {
                // 子弹数量检查
                int needAmmoCount = maxAmmoCount - currentAmmoCount;
                // 背包检查
                for (int i = 0; i < cap.getSlots(); i++) {
                    ItemStack checkAmmoStack = cap.getStackInSlot(i);
                    if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(tac$CurrentGunItem, checkAmmoStack)) {
                        ItemStack extractItem = cap.extractItem(i, needAmmoCount, false);
                        needAmmoCount = needAmmoCount - extractItem.getCount();
                        if (needAmmoCount <= 0) {
                            break;
                        }
                    }
                    if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(tac$CurrentGunItem, checkAmmoStack)) {
                        int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                        int extractCount = Math.min(boxAmmoCount, needAmmoCount);
                        int remainCount = boxAmmoCount - extractCount;
                        iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                        if (remainCount <= 0) {
                            iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                        }
                        needAmmoCount = needAmmoCount - extractCount;
                        if (needAmmoCount <= 0) {
                            break;
                        }
                    }
                }
                return maxAmmoCount - needAmmoCount;
            }).orElse(currentAmmoCount);
        }
        return maxAmmoCount;
    }

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    public void defineSynData(CallbackInfo ci) {
        entityData.define(DATA_SHOOT_COOL_DOWN_ID, -1L);
        entityData.define(DATA_RELOAD_STATE_ID, new ReloadState());
        entityData.define(DATA_AIMING_PROGRESS_ID, 0f);
        entityData.define(DATA_DRAW_COOL_DOWN_ID, -1L);
    }

    @Override
    @Unique
    public void resetKnockbackStrength() {
        this.tac$KnockbackStrength = -1;
    }

    @Override
    @Unique
    public double getKnockbackStrength() {
        return this.tac$KnockbackStrength;
    }

    @Override
    @Unique
    public void setKnockbackStrength(double strength) {
        this.tac$KnockbackStrength = strength;
    }
}
