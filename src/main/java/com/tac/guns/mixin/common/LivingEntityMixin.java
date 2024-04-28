package com.tac.guns.mixin.common;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.entity.KnockBackModifier;
import com.tac.guns.api.event.common.GunFireSelectEvent;
import com.tac.guns.api.event.common.GunReloadEvent;
import com.tac.guns.api.event.common.GunShootEvent;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IAmmoBox;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.config.common.GunConfig;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.entity.serializer.ModEntityDataSerializers;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.attachment.Silence;
import com.tac.guns.resource.pojo.data.gun.*;
import com.tac.guns.sound.SoundManager;
import com.tac.guns.util.AttachmentDataUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.function.Supplier;

@SuppressWarnings("UnreachableCode")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IGunOperator, KnockBackModifier {
    @Unique
    private static final EntityDataAccessor<Long> DATA_SHOOT_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.LONG);
    @Unique
    private static final EntityDataAccessor<ReloadState> DATA_RELOAD_STATE_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.RELOAD_STATE);
    @Unique
    private static final EntityDataAccessor<Float> DATA_AIMING_PROGRESS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Long> DATA_DRAW_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.LONG);
    @Unique
    private static final EntityDataAccessor<Boolean> DATA_IS_AIMING_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Float> DATA_SPRINT_TIME_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    @Unique
    private static final EntityDataAccessor<Long> DATA_BOLT_COOL_DOWN_ID = SynchedEntityData.defineId(LivingEntity.class, ModEntityDataSerializers.LONG);
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
     * 拉栓时间戳，在拉栓开始时更新，单位 ms。
     */
    @Unique
    private long tac$BoltTimestamp = -1;
    @Unique
    private long tac$BoltCoolDown = -1;
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
     * 当前操作的枪械物品的 Supplier。在切枪时 (draw 方法) 更新。
     */
    @Unique
    @Nullable
    private Supplier<ItemStack> tac$CurrentGunItem = null;
    /**
     * 缓存当前枪械的收枪时间，以确保下一次切枪的时候使用此时间计算收枪。
     * 此数值不会因 tac$CurrentGunItem 提供的 ItemStack 改变而改变，因此应当在恰当的时机调用 updatePutAwayTime() 进行更新。
     */
    @Unique
    private float tac$CurrentPutAwayTimeS = 0;
    @Unique
    private float tac$SprintTimeS = 0;
    @Unique
    private long tac$SprintTimestamp = -1;
    /**
     * 用来记录子弹击退能力，负数表示使用原版击退
     */
    @Unique
    private double tac$KnockbackStrength = -1;

    /**
     * 记录射击数，用以判定曳光弹
     */
    @Unique
    private int tac$shootCount = 0;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Shadow(remap = false)
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
    public long getSynBoltCoolDown() {
        return this.getEntityData().get(DATA_BOLT_COOL_DOWN_ID);
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

    @Override
    @Unique
    public float getSynSprintTime() {
        return this.getEntityData().get(DATA_SPRINT_TIME_ID);
    }

    @Override
    @Unique
    public boolean getSynIsAiming() {
        return this.getEntityData().get(DATA_IS_AIMING_ID);
    }

    @Unique
    private long getShootCoolDown() {
        if (tac$CurrentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
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
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
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
    private void initialData() {
        // 重置各个状态
        tac$ShootTimestamp = -1;
        tac$IsAiming = false;
        tac$AimingProgress = 0;
        tac$ReloadTimestamp = -1;
        tac$ReloadStateType = ReloadState.StateType.NOT_RELOADING;
        tac$SprintTimestamp = -1;
        tac$SprintTimeS = 0;
        tac$BoltTimestamp = -1;
        tac$BoltCoolDown = -1;
        tac$shootCount = 0;
    }

    @Unique
    @Override
    public void draw(Supplier<ItemStack> gunItemSupplier) {
        // 重置各个状态
        initialData();
        // 更新切枪时间戳
        if (tac$DrawTimestamp == -1) {
            tac$DrawTimestamp = System.currentTimeMillis();
        }
        long drawTime = System.currentTimeMillis() - tac$DrawTimestamp;
        if (drawTime >= 0) {
            // 如果不处于收枪状态，则需要计算收枪时长
            if (drawTime < tac$CurrentPutAwayTimeS * 1000) {
                tac$DrawTimestamp = System.currentTimeMillis() + drawTime;
            } else {
                tac$DrawTimestamp = System.currentTimeMillis() + (long) (tac$CurrentPutAwayTimeS * 1000);
            }
        }
        tac$CurrentGunItem = gunItemSupplier;
        ((IGunOperator) this).updatePutAwayTime();
    }

    @Unique
    @Override
    public void bolt() {
        if (tac$CurrentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 判断是否正在射击冷却
            if (getShootCoolDown() != 0) {
                return;
            }
            // 检查是否正在换弹
            if (tac$ReloadStateType.isReloading()) {
                return;
            }
            // 检查是否在切枪
            if (getDrawCoolDown() != 0) {
                return;
            }
            // 检查是否在拉栓
            if (tac$BoltCoolDown >= 0) {
                return;
            }
            // 检查 bolt 类型是否是 manual action
            Bolt boltType = gunIndex.getGunData().getBolt();
            if (boltType != Bolt.MANUAL_ACTION) {
                return;
            }
            // 检查是否有弹药在枪膛内
            if (iGun.hasBulletInBarrel(currentGunItem)) {
                return;
            }
            // 检查弹匣内是否有子弹
            if (iGun.getCurrentAmmoCount(currentGunItem) == 0) {
                return;
            }
            tac$BoltTimestamp = System.currentTimeMillis();
            // 将bolt cool down随便改为一个非 -1 的数值，以标记bolt进程开始
            tac$BoltCoolDown = 0;
        });
    }

    @Unique
    @Override
    public void updatePutAwayTime() {
        ItemStack gunItem = tac$CurrentGunItem == null ? ItemStack.EMPTY : tac$CurrentGunItem.get();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun != null) {
            Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem));
            tac$CurrentPutAwayTimeS = gunIndex.map(index -> index.getGunData().getPutAwayTime()).orElse(0F);
        } else {
            tac$CurrentPutAwayTimeS = 0;
        }
    }

    @Unique
    @Override
    public void reload() {
        if (tac$CurrentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
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
            // 检查是否在拉栓
            if (tac$BoltCoolDown >= 0) {
                return;
            }
            int currentAmmoCount = iGun.getCurrentAmmoCount(currentGunItem);
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(currentGunItem, gunIndex.getGunData());
            // 检查弹药
            if (this.needCheckAmmo()) {
                // 超出或达到上限，不换弹
                if (currentAmmoCount >= maxAmmoCount) {
                    return;
                }
                boolean hasAmmo = this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(cap -> {
                    // 背包检查
                    for (int i = 0; i < cap.getSlots(); i++) {
                        ItemStack checkAmmoStack = cap.getStackInSlot(i);
                        if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(currentGunItem, checkAmmoStack)) {
                            return true;
                        }
                        if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(currentGunItem, checkAmmoStack)) {
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
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(entity, currentGunItem, LogicalSide.SERVER))) {
                return;
            }
            Bolt boltType = gunIndex.getGunData().getBolt();
            boolean needBolt = boltType == Bolt.CLOSED_BOLT || boltType == Bolt.MANUAL_ACTION;
            boolean hasBulletInBarrel = iGun.hasBulletInBarrel(currentGunItem);
            if (needBolt && !hasBulletInBarrel) {
                // 初始化空仓换弹的 tick 的状态
                tac$ReloadStateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            } else {
                // 初始化战术换弹的 tick 的状态
                tac$ReloadStateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
            }
            tac$ReloadTimestamp = System.currentTimeMillis();
        });
    }

    @Unique
    @Override
    public ShootResult shoot(float pitch, float yaw) {
        if (tac$CurrentGunItem == null) {
            return ShootResult.NOT_DRAW;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.ID_NOT_EXIST;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        // 判断射击是否正在冷却
        long coolDown = getShootCoolDown();
        if (coolDown == -1) {
            // 一般来说不太可能为 -1，原因未知
            return ShootResult.UNKNOWN_FAIL;
        }
        if (coolDown > 0) {
            return ShootResult.COOL_DOWN;
        }
        // 检查是否正在换弹
        if (tac$ReloadStateType.isReloading()) {
            return ShootResult.IS_RELOADING;
        }
        // 检查是否在切枪
        if (getDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 检查是否在拉栓
        if (tac$BoltCoolDown >= 0) {
            return ShootResult.IS_BOLTING;
        }
        // 检查是否在奔跑
        if (tac$SprintTimeS > 0) {
            return ShootResult.IS_SPRINTING;
        }
        LivingEntity shooter = (LivingEntity) (Object) this;
        Bolt boltType = gunIndex.getGunData().getBolt();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (hasAmmoInBarrel ? 1 : 0);
        // 创造模式不判断子弹数
        if (needCheckAmmo() && ammoCount < 1) {
            return ShootResult.NO_AMMO;
        }
        // 检查膛内子弹
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            return ShootResult.NEED_BOLT;
        }
        if (boltType == Bolt.CLOSED_BOLT && !hasAmmoInBarrel) {
            iGun.reduceCurrentAmmoCount(currentGunItem);
            iGun.setBulletInBarrel(currentGunItem, true);
        }
        // 触发射击事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }
        // 调用射击方法
        Level world = shooter.getLevel();
        BulletData bulletData = gunIndex.getBulletData();
        InaccuracyType inaccuracyState = InaccuracyType.getInaccuracyType(shooter);
        final float[] inaccuracy = new float[]{gunIndex.getGunData().getInaccuracy(inaccuracyState)};
        final int[] soundDistance = new int[]{GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get()};
        final boolean[] useSilenceSound = new boolean[]{false};
        AttachmentDataUtils.getAllAttachmentData(currentGunItem, gunIndex.getGunData(), attachmentData -> {
            // 影响除瞄准外所有的不准确度
            if (!inaccuracyState.isAim()) {
                inaccuracy[0] += attachmentData.getInaccuracyAddend();
            }
            Silence silence = attachmentData.getSilence();
            if (silence != null) {
                soundDistance[0] += silence.getDistanceAddend();
                if (silence.isUseSilenceSound()) {
                    useSilenceSound[0] = true;
                }
            }
        });
        inaccuracy[0] = Math.max(0, inaccuracy[0]);
        float speed = Mth.clamp(bulletData.getSpeed() / 20, 0, Float.MAX_VALUE);
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);
        boolean isTracerAmmo = bulletData.hasTracerAmmo() && (tac$shootCount % (bulletData.getTracerCountInterval() + 1) == 0);
        ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();
        // 开始生成子弹
        for (int i = 0; i < bulletAmount; i++) {
            EntityBullet bullet = new EntityBullet(world, shooter, ammoId, bulletData, isTracerAmmo, gunId);
            bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, speed, inaccuracy[0]);
            world.addFreshEntity(bullet);
        }
        if (soundDistance[0] > 0) {
            String soundId = useSilenceSound[0] ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
            SoundManager.sendSoundToNearby(shooter, soundDistance[0], gunId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
        }
        // 削减弹药数
        if (this.needCheckAmmo()) {
            if (boltType == Bolt.MANUAL_ACTION) {
                iGun.setBulletInBarrel(currentGunItem, false);
            } else if (boltType == Bolt.CLOSED_BOLT) {
                if (iGun.getCurrentAmmoCount(currentGunItem) > 0) {
                    iGun.reduceCurrentAmmoCount(currentGunItem);
                } else {
                    iGun.setBulletInBarrel(currentGunItem, false);
                }
            } else {
                iGun.reduceCurrentAmmoCount(currentGunItem);
            }
        }
        tac$ShootTimestamp = System.currentTimeMillis();
        tac$shootCount += 1;
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
    public void fireSelect() {
        if (tac$CurrentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(entity, currentGunItem, LogicalSide.SERVER))) {
            return;
        }
        // 应用切换逻辑
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> {
            FireMode fireMode = iGun.getFireMode(currentGunItem);
            List<FireMode> fireModeSet = gunIndex.getGunData().getFireModeSet();
            // 即使玩家拿的是没有的 FireMode，这里也能切换到正常情况
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            iGun.setFireMode(currentGunItem, nextFireMode);
            return nextFireMode;
        });
    }

    @Unique
    @Override
    public void zoom() {
        if (tac$CurrentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ItemStack scopeItem = iGun.getAttachment(currentGunItem, AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
        if (iAttachment != null) {
            TimelessAPI.getCommonAttachmentIndex(iAttachment.getAttachmentId(scopeItem)).ifPresent(index -> {
                int zoomNumber = iAttachment.getZoomNumber(scopeItem);
                ++zoomNumber;
                zoomNumber = zoomNumber % (Integer.MAX_VALUE - 1); // 避免上溢变成负的
                iAttachment.setZoomNumber(scopeItem, zoomNumber);
                iGun.installAttachment(currentGunItem, scopeItem);
            });
        }
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
        tickSprint();
        tickBolt();
        // 从服务端同步数据
        this.getEntityData().set(DATA_SHOOT_COOL_DOWN_ID, getShootCoolDown());
        this.getEntityData().set(DATA_DRAW_COOL_DOWN_ID, getDrawCoolDown());
        this.getEntityData().set(DATA_BOLT_COOL_DOWN_ID, tac$BoltCoolDown);
        this.getEntityData().set(DATA_RELOAD_STATE_ID, reloadState);
        this.getEntityData().set(DATA_AIMING_PROGRESS_ID, tac$AimingProgress);
        this.getEntityData().set(DATA_IS_AIMING_ID, tac$IsAiming);
        this.getEntityData().set(DATA_SPRINT_TIME_ID, tac$SprintTimeS);
    }

    private void tickSprint() {
        LivingEntity entity = (LivingEntity) (Object) this;
        ReloadState reloadState = this.getEntityData().get(DATA_RELOAD_STATE_ID);
        if (tac$IsAiming || (reloadState.getStateType().isReloading() && !reloadState.getStateType().isReloadFinishing())) {
            entity.setSprinting(false);
        }
        if (tac$SprintTimestamp == -1) {
            tac$SprintTimestamp = System.currentTimeMillis();
        }
        if (tac$CurrentGunItem == null) {
            return;
        }
        ItemStack gunItem = tac$CurrentGunItem.get();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem)).ifPresentOrElse(gunIndex -> {
            float gunSprintTime = gunIndex.getGunData().getSprintTime();
            if (entity.isSprinting()) {
                tac$SprintTimeS += System.currentTimeMillis() - tac$SprintTimestamp;
                if (tac$SprintTimeS > gunSprintTime) {
                    tac$SprintTimeS = gunSprintTime;
                }
            } else {
                tac$SprintTimeS -= System.currentTimeMillis() - tac$SprintTimestamp;
                if (tac$SprintTimeS < 0) {
                    tac$SprintTimeS = 0;
                }
            }
        }, () -> {
            tac$SprintTimeS = 0;
        });
        tac$SprintTimestamp = System.currentTimeMillis();
    }

    @Unique
    private void tickAimingProgress() {
        // currentGunItem 如果为 null，则取消瞄准状态并将 aimingProgress 归零。
        if (tac$CurrentGunItem == null || !(tac$CurrentGunItem.get().getItem() instanceof IGun iGun)) {
            tac$AimingProgress = 0;
            tac$AimingTimestamp = System.currentTimeMillis();
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        // 如果获取不到 gunIndex，则取消瞄准状态并将 aimingProgress 归零，返回。
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            tac$AimingProgress = 0;
            return;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        final float[] aimTime = new float[]{gunData.getAimTime()};
        AttachmentDataUtils.getAllAttachmentData(currentGunItem, gunData, attachmentData -> aimTime[0] += attachmentData.getAdsAddendTime());
        aimTime[0] = Math.max(0, aimTime[0]);
        float alphaProgress = (System.currentTimeMillis() - tac$AimingTimestamp + 1) / (aimTime[0] * 1000);
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
    private void tickBolt() {
        // bolt cool down 为 -1 时，代表拉栓逻辑进程没有开始，不需要tick
        if (tac$BoltCoolDown == -1) {
            return;
        }
        if (tac$CurrentGunItem == null) {
            tac$BoltCoolDown = -1;
            return;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            tac$BoltCoolDown = -1;
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        tac$BoltCoolDown = gunIndex.map(index -> {
            long coolDown = (long) (index.getGunData().getBoltActionTime() * 1000) - (System.currentTimeMillis() - tac$BoltTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
        if (tac$BoltCoolDown == 0) {
            if (iGun.getCurrentAmmoCount(currentGunItem) > 0) {
                iGun.reduceCurrentAmmoCount(currentGunItem);
                iGun.setBulletInBarrel(currentGunItem, true);
            }
            tac$BoltCoolDown = -1;
        }
    }

    @Unique
    private ReloadState tickReloadState() {
        // 初始化 tick 返回值
        ReloadState reloadState = new ReloadState();
        reloadState.setStateType(ReloadState.StateType.NOT_RELOADING);
        reloadState.setCountDown(ReloadState.NOT_RELOADING_COUNTDOWN);
        // 判断是否正在进行装填流程。如果没有则返回。
        if (tac$ReloadTimestamp == -1 || tac$CurrentGunItem == null) {
            return reloadState;
        }
        if (!(tac$CurrentGunItem.get().getItem() instanceof IGun iGun)) {
            return reloadState;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        // 获取当前枪械的 ReloadData。如果没有则返回。
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
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
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(currentGunItem, gunData);
        if (tac$ReloadStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.EMPTY_RELOAD_FINISHING) {
                iGun.setCurrentAmmoCount(currentGunItem, getAndExtractNeedAmmoCount(iGun, maxAmmoCount));
                Bolt boltType = gunIndexOptional.get().getGunData().getBolt();
                if (boltType == Bolt.MANUAL_ACTION || boltType == Bolt.CLOSED_BOLT) {
                    iGun.reduceCurrentAmmoCount(currentGunItem);
                    iGun.setBulletInBarrel(currentGunItem, true);
                }
            }
        }
        if (tac$ReloadStateType == ReloadState.StateType.TACTICAL_RELOAD_FEEDING) {
            if (stateType == ReloadState.StateType.TACTICAL_RELOAD_FINISHING) {
                iGun.setCurrentAmmoCount(currentGunItem, getAndExtractNeedAmmoCount(iGun, maxAmmoCount));
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
        if (tac$CurrentGunItem == null) {
            return -1;
        }
        ItemStack currentGunItem = tac$CurrentGunItem.get();
        int currentAmmoCount = iGun.getCurrentAmmoCount(currentGunItem);
        if (this.needCheckAmmo()) {
            return this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(cap -> {
                // 子弹数量检查
                int needAmmoCount = maxAmmoCount - currentAmmoCount;
                // 背包检查
                for (int i = 0; i < cap.getSlots(); i++) {
                    ItemStack checkAmmoStack = cap.getStackInSlot(i);
                    if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(currentGunItem, checkAmmoStack)) {
                        ItemStack extractItem = cap.extractItem(i, needAmmoCount, false);
                        needAmmoCount = needAmmoCount - extractItem.getCount();
                        if (needAmmoCount <= 0) {
                            break;
                        }
                    }
                    if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(currentGunItem, checkAmmoStack)) {
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
        entityData.define(DATA_BOLT_COOL_DOWN_ID, -1L);
        entityData.define(DATA_IS_AIMING_ID, false);
        entityData.define(DATA_SPRINT_TIME_ID, 0f);
    }

    @Inject(method = "die", at = @At("RETURN"))
    public void onDie(DamageSource pDamageSource, CallbackInfo ci) {
        // 重置各个状态
        initialData();
    }

    @Override
    @Unique
    public void resetKnockBackStrength() {
        this.tac$KnockbackStrength = -1;
    }

    @Override
    @Unique
    public double getKnockBackStrength() {
        return this.tac$KnockbackStrength;
    }

    @Override
    @Unique
    public void setKnockBackStrength(double strength) {
        this.tac$KnockbackStrength = strength;
    }
}
