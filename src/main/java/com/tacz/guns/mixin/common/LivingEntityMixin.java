package com.tacz.guns.mixin.common;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.*;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("All")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IGunOperator, KnockBackModifier {
    private final @Unique LivingEntity tacz$shooter = (LivingEntity) (Object) this;
    private final @Unique ShooterDataHolder tacz$data = new ShooterDataHolder();
    private final @Unique LivingEntityDrawGun tacz$draw = new LivingEntityDrawGun(tacz$shooter, tacz$data);
    private final @Unique LivingEntityAim tacz$aim = new LivingEntityAim(tacz$shooter, this.tacz$data);
    private final @Unique LivingEntityCrawl tacz$crawl = new LivingEntityCrawl(tacz$shooter, this.tacz$data);
    private final @Unique LivingEntityAmmoCheck tacz$ammoCheck = new LivingEntityAmmoCheck(tacz$shooter);
    private final @Unique LivingEntityFireSelect tacz$fireSelect = new LivingEntityFireSelect(tacz$shooter, this.tacz$data);
    private final @Unique LivingEntityMelee tacz$melee = new LivingEntityMelee(tacz$shooter, this.tacz$data, this.tacz$draw);
    private final @Unique LivingEntityShoot tacz$shoot = new LivingEntityShoot(tacz$shooter, this.tacz$data, this.tacz$draw);
    private final @Unique LivingEntityBolt tacz$bolt = new LivingEntityBolt(this.tacz$data, this.tacz$shooter, this.tacz$draw, this.tacz$shoot);
    private final @Unique LivingEntityReload tacz$reload = new LivingEntityReload(tacz$shooter, this.tacz$data, this.tacz$draw, this.tacz$shoot);
    private final @Unique LivingEntitySpeedModifier tacz$speed = new LivingEntitySpeedModifier(tacz$shooter, tacz$data);
    private final @Unique LivingEntitySprint tacz$sprint = new LivingEntitySprint(tacz$shooter, this.tacz$data);
    private final @Unique LivingEntityHeat tacz$heat = new LivingEntityHeat(tacz$shooter, this.tacz$data);


    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Unique
    public long getSynShootCoolDown() {
        return ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    public long getSynMeleeCoolDown() {
        return ModSyncedEntityData.MELEE_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public long getSynDrawCoolDown() {
        return ModSyncedEntityData.DRAW_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public boolean getSynIsBolting() {
        return ModSyncedEntityData.IS_BOLTING_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public ReloadState getSynReloadState() {
        return ModSyncedEntityData.RELOAD_STATE_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public float getSynAimingProgress() {
        return ModSyncedEntityData.AIMING_PROGRESS_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public float getSynSprintTime() {
        return ModSyncedEntityData.SPRINT_TIME_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public boolean getSynIsAiming() {
        return ModSyncedEntityData.IS_AIMING_KEY.getValue(tacz$shooter);
    }

    @Override
    @Unique
    public void initialData() {
        // 初始化 ShooterDataHolder
        this.tacz$data.initialData();
        // 刷新当前武器
        this.tacz$data.setCurrentGunItem(() -> tacz$shooter.getMainHandItem());
        // 刷新配件属性缓存
        AttachmentPropertyManager.postChangeEvent(tacz$shooter, tacz$shooter.getMainHandItem());
    }

    @Unique
    @Override
    public void draw(Supplier<ItemStack> gunItemSupplier) {
        this.tacz$draw.draw(gunItemSupplier);
    }

    @Unique
    @Override
    public void bolt() {
        tacz$runGunAction("start_bolt", () -> this.tacz$bolt.bolt());
    }

    @Unique
    @Override
    public void reload() {
        tacz$runGunAction("start_reload", () -> this.tacz$reload.reload());
    }
    @Unique
    @Override
    public void cancelReload(){
        tacz$runGunAction("interrupt_reload", () -> this.tacz$reload.cancelReload());
    }

    @Override
    public void melee() {
        tacz$runGunAction("melee", () -> this.tacz$melee.melee());
    }

    @Unique
    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw) {
        return this.shoot(pitch, yaw, GunTime.nowMillis() - tacz$data.baseTimestamp);
    }

    @Unique
    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp) {
        return tacz$runGunAction("shoot", () -> tacz$shoot.shoot(pitch, yaw, timestamp), ShootResult.UNKNOWN_FAIL);
    }

    @Unique
    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp, float chargeProgress) {
        return tacz$runGunAction("shoot", () -> tacz$shoot.shoot(pitch, yaw, timestamp, chargeProgress), ShootResult.UNKNOWN_FAIL);
    }

    @Unique
    @Override
    public boolean needCheckAmmo() {
        return this.tacz$ammoCheck.needCheckAmmo();
    }

    @Unique
    @Override
    public boolean consumesAmmoOrNot() {
        return this.tacz$ammoCheck.consumesAmmoOrNot();
    }

    @Unique
    @Override
    public boolean getProcessedSprintStatus(boolean sprint) {
        return this.tacz$sprint.getProcessedSprintStatus(sprint);
    }

    @Unique
    @Override
    public void aim(boolean isAim) {
        this.tacz$aim.aim(isAim);
    }

    @Override
    public void crawl(boolean isCrawl) {
        this.tacz$crawl.crawl(isCrawl);
    }

    @Override
    public void updateCacheProperty(AttachmentCacheProperty cacheProperty) {
        this.tacz$data.cacheProperty = cacheProperty;
    }

    @Override
    @Nullable
    public AttachmentCacheProperty getCacheProperty() {
        return this.tacz$data.cacheProperty;
    }

    @Override
    public ShooterDataHolder getDataHolder() {
        return this.tacz$data;
    }

    @Override
    public boolean nextBulletIsTracer(int tracerCountInterval) {
        this.tacz$data.shootCount++;
        if (tracerCountInterval == -1) {
            return false;
        }
        return tacz$data.shootCount % (tracerCountInterval + 1) == 0;
    }

    @Unique
    @Override
    public void fireSelect() {
        tacz$runGunAction("fire_select", () -> this.tacz$fireSelect.fireSelect());
    }

    @Unique
    @Override
    public void zoom() {
        this.tacz$aim.zoom();
    }

    @Unique
    private void tacz$runGunAction(String operation, Runnable action) {
        GunScriptGuard.run(tacz$data, tacz$shooter, tacz$data.currentGunId, operation, action);
    }

    @Unique
    private <T> T tacz$runGunAction(String operation, Supplier<T> action, T fallback) {
        return GunScriptGuard.run(tacz$data, tacz$shooter, tacz$data.currentGunId, operation, action, fallback);
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTickServerSide(CallbackInfo ci) {
        // 仅在服务端调用
        if (!level().isClientSide()) {
            // 完成各种 tick 任务
            ReloadState reloadState = tacz$runGunAction("tick_reload", () -> this.tacz$reload.tickReloadState(), new ReloadState());
            this.tacz$aim.tickAimingProgress();
            this.tacz$aim.tickSprint();
            this.tacz$crawl.tickCrawling();
            tacz$runGunAction("tick_bolt", () -> this.tacz$bolt.tickBolt());
            tacz$runGunAction("tick_melee", () -> this.tacz$melee.scheduleTickMelee());
            this.tacz$speed.updateSpeedModifier();
            tacz$runGunAction("tick_heat", () -> this.tacz$heat.tickHeat());
            // 后续脚本也可能中止换弹，不能同步本 tick 较早取得的旧结果。
            if (!this.tacz$data.reloadStateType.isReloading()) {
                reloadState = new ReloadState();
            }
            tacz$shooter.setSprinting(getProcessedSprintStatus(tacz$shooter.isSprinting()));
            // 从服务端同步数据
            ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.setValue(tacz$shooter,
                    tacz$runGunAction("shoot_cooldown", () -> this.tacz$shoot.getShootCoolDown(), 0L));
            ModSyncedEntityData.MELEE_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$melee.getMeleeCoolDown());
            ModSyncedEntityData.DRAW_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$draw.getDrawCoolDown());
            ModSyncedEntityData.IS_BOLTING_KEY.setValue(tacz$shooter, this.tacz$data.isBolting);
            ModSyncedEntityData.RELOAD_STATE_KEY.setValue(tacz$shooter, reloadState);
            ModSyncedEntityData.AIMING_PROGRESS_KEY.setValue(tacz$shooter, this.tacz$data.aimingProgress);
            ModSyncedEntityData.IS_AIMING_KEY.setValue(tacz$shooter, this.tacz$data.isAiming);
            ModSyncedEntityData.SPRINT_TIME_KEY.setValue(tacz$shooter, this.tacz$data.sprintTimeS);
        }
    }

    @Override
    @Unique
    public void resetKnockBackStrength() {
        this.tacz$data.knockbackStrength = -1;
    }

    @Override
    @Unique
    public double getKnockBackStrength() {
        return this.tacz$data.knockbackStrength;
    }

    @Override
    @Unique
    public void setKnockBackStrength(double strength) {
        this.tacz$data.knockbackStrength = strength;
    }
}
