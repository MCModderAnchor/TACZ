package com.tac.guns.mixin.common;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunFireSelectEvent;
import com.tac.guns.api.event.GunReloadEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IGun;
import com.tac.guns.api.network.MyEntityDataSerializers;
import com.tac.guns.item.GunItem;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.GunData;
import com.tac.guns.resource.pojo.data.GunReloadData;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
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

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IGunOperator {
    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Shadow
    protected abstract float tickHeadTurn(float p_21260_, float p_21261_);

    @Unique
    private static final EntityDataAccessor<Long> DATA_SHOOT_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, MyEntityDataSerializers.LONG);

    @Unique
    private static final EntityDataAccessor<ReloadState> DATA_RELOAD_STATE_ID = SynchedEntityData.defineId(LivingEntity.class, MyEntityDataSerializers.RELOAD_STATE);

    @Unique
    private static final EntityDataAccessor<Float> DATA_AIMING_PROGRESS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);

    // 开火逻辑的状态缓存
    /**
     * 射击时间戳，射击成功时更新，单位 ms。
     * 用于计算射击的冷却时间。
     */
    @Unique
    private long tac$ShootTimestamp = -1L;

    // 切枪逻辑的状态缓存
    /**
     * 切枪时间戳，在切枪开始时更新，单位 ms。
     * 用于计算切枪进度。切枪进度完成后，才能进行各种操作。
     */
    @Unique
    private long tac$DrawTimestamp = -1L;

    // 瞄准逻辑的状态缓存
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

    // 换弹逻辑的状态缓存
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

    @Override
    @Unique
    public long getSynShootCoolDown() {
        return this.getEntityData().get(DATA_SHOOT_COOL_DOWN_ID);
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
        ResourceLocation gunId = GunItem.getData(tac$CurrentGunItem).getGunId();
        Optional<CommonGunIndex> gunIndex = CommonGunPackLoader.getGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval() - (System.currentTimeMillis() - tac$ShootTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown -= 5;
            if (coolDown < 0) return 0L;
            return coolDown;
        }).orElse(-1L);
    }

    @Unique
    @Override
    public void draw(ItemStack gunItemStack) {
        tac$CurrentGunItem = gunItemStack;
        // 重置各个状态
        tac$ShootTimestamp = -1;
        tac$IsAiming = false;
        tac$AimingProgress = 0;
        tac$ReloadTimestamp = -1;
        tac$ReloadStateType = ReloadState.StateType.NOT_RELOADING;

        if(!IGun.isGun(gunItemStack)){
            tac$DrawTimestamp = -1;
            // todo 执行收枪逻辑
        }else{
            tac$DrawTimestamp = System.currentTimeMillis();
            // todo 执行切枪逻辑
        }
    }

    @Unique
    @Override
    public void reload() {
        if(tac$CurrentGunItem == null){
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        ResourceLocation gunId = GunItem.getData(tac$CurrentGunItem).getGunId();
        CommonGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查换弹是否还未完成
            if (tac$ReloadStateType.isReloading()) {
                return;
            }
            // 检查是否正在开火冷却
            if (getShootCoolDown() != 0) {
                return;
            }
            // todo 检查 draw 是否还未完成
            // 触发装弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
                return;
            }
            // todo 根据枪内子弹数量初始化换弹状态 ，此处默认空仓换弹
            tac$ReloadStateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            tac$ReloadTimestamp = System.currentTimeMillis();
        });
    }

    @Unique
    @Override
    public ShootResult shoot(float pitch, float yaw) {
        if(tac$CurrentGunItem == null){
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
        // todo 检查 draw 是否还未完成
        LivingEntity entity = (LivingEntity) (Object) this;
        // 触发射击事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FAIL;
        }
        // todo 判断枪械是否有足够的弹药
        // 调用射击方法
        if (tac$CurrentGunItem.getItem() instanceof IGun iGun) {
            iGun.shoot(entity, tac$CurrentGunItem, pitch, yaw);
            tac$ShootTimestamp = System.currentTimeMillis();
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }

    @Unique
    @Override
    public void aim(boolean isAim) {
        // todo 判断当前状态能不能瞄准
        tac$IsAiming = isAim;
    }

    @Unique
    @Override
    public FireMode fireSelect() {
        if(tac$CurrentGunItem == null){
            return FireMode.UNKNOWN;
        }
        // 获取GunData
        GunItemData gunItemData = GunItem.getData(tac$CurrentGunItem);
        ResourceLocation gunId = gunItemData.getGunId();
        LivingEntity entity = (LivingEntity) (Object) this;
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return FireMode.SEMI;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(entity, tac$CurrentGunItem, LogicalSide.SERVER))) {
            return FireMode.SEMI;
        }
        if (tac$CurrentGunItem.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(tac$CurrentGunItem);
            List<FireMode> fireModeSet = gunData.getFireModeSet();
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            gunItemData.setFireMode(nextFireMode);
            GunItem.setData(tac$CurrentGunItem, gunItemData);
            return nextFireMode;
        }
        return FireMode.SEMI;
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
        this.getEntityData().set(DATA_RELOAD_STATE_ID, reloadState);
        this.getEntityData().set(DATA_AIMING_PROGRESS_ID, tac$AimingProgress);
    }

    @Unique
    private void tickAimingProgress() {
        // currentGunItem 如果为 null，则取消瞄准状态并将 aimingProgress 归零。
        if (tac$CurrentGunItem == null) {
            tac$AimingProgress = 0;
            tac$AimingTimestamp = System.currentTimeMillis();
            return;
        }
        // 如果获取不到 aimTime，则取消瞄准状态并将 aimingProgress 归零，返回。
        ResourceLocation gunId = GunItem.getData(tac$CurrentGunItem).getGunId();
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            tac$AimingProgress = 0;
            return;
        }
        float aimTime = gunIndexOptional.get().getGunData().getAimTime();
        float alphaProgress = (System.currentTimeMillis() - tac$AimingTimestamp + 1) / (aimTime * 1000);
        if (tac$IsAiming) {
            // 处于执行瞄准状态，增加 aimingProgress
            tac$AimingProgress += alphaProgress;
            if (tac$AimingProgress > 1) tac$AimingProgress = 1;
        } else {
            // 处于取消瞄准状态，减小 aimingProgress
            tac$AimingProgress -= alphaProgress;
            if (tac$AimingProgress < 0) tac$AimingProgress = 0;
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
        // 获取当前枪械的 ReloadData。如果没有则返回。
        ResourceLocation gunId = GunItem.getData(tac$CurrentGunItem).getGunId();
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return reloadState;
        }
        GunReloadData reloadData = gunIndexOptional.get().getGunData().getReloadData();
        // 计算新的 stateType 和 countDown
        long countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
        ReloadState.StateType stateType = tac$ReloadStateType;
        long progressTime = System.currentTimeMillis() - tac$ReloadTimestamp;
        if (stateType.isReloadingEmpty()) {
            long magFedTime = (long) (reloadData.getEmptyMagFedTime() * 1000);
            long finishingTime = (long) (reloadData.getEmptyReloadTime() * 1000);
            if (progressTime < magFedTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                countDown = magFedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                tac$ReloadTimestamp = -1;
            }
        } else if (stateType.isReloadingNormal()) {
            long magFedTime = (long) (reloadData.getNormalMagFedTime() * 1000);
            long finishingTime = (long) (reloadData.getNormalReloadTime() * 1000);
            if (progressTime < magFedTime) {
                stateType = ReloadState.StateType.NORMAL_RELOAD_FEEDING;
                countDown = magFedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.NORMAL_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                tac$ReloadTimestamp = -1;
            }
        }
        // 更新枪内弹药
        if (tac$ReloadStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.EMPTY_RELOAD_FINISHING) {
                // todo 更改枪的弹药数
                entity.sendMessage(new TextComponent("fed!"), Util.NIL_UUID);
            }
        }
        if (tac$ReloadStateType == ReloadState.StateType.NORMAL_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.NORMAL_RELOAD_FINISHING) {
                // todo 更改枪的弹药数
                entity.sendMessage(new TextComponent("fed!"), Util.NIL_UUID);
            }
        }
        // 更新换弹状态缓存
        tac$ReloadStateType = stateType;
        // 返回 tick 结果
        reloadState.setStateType(stateType);
        reloadState.setCountDown(countDown);
        return reloadState;
    }

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    public void defineSynData(CallbackInfo ci) {
        entityData.define(DATA_SHOOT_COOL_DOWN_ID, -1L);
        entityData.define(DATA_RELOAD_STATE_ID, new ReloadState());
        entityData.define(DATA_AIMING_PROGRESS_ID, 0f);
    }
}
