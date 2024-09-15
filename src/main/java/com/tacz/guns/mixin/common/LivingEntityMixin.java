package com.tacz.guns.mixin.common;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.*;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
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
    private final @Unique LivingEntityBolt tacz$bolt = new LivingEntityBolt(this.tacz$data, this.tacz$draw, this.tacz$shoot);
    private final @Unique LivingEntityReload tacz$reload = new LivingEntityReload(tacz$shooter, this.tacz$data, this.tacz$draw, this.tacz$shoot);
    private final @Unique LivingEntitySpeedModifier tacz$speed = new LivingEntitySpeedModifier(tacz$shooter, tacz$data);


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
    public long getSynBoltCoolDown() {
        return ModSyncedEntityData.BOLT_COOL_DOWN_KEY.getValue(tacz$shooter);
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
        this.tacz$data.initialData();
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
        this.tacz$bolt.bolt();
    }

    @Unique
    @Override
    public void reload() {
        this.tacz$reload.reload();
    }

    @Override
    public void melee() {
        this.tacz$melee.melee();
    }

    @Unique
    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw) {
        return this.tacz$shoot.shoot(pitch, yaw);
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

    @Nullable
    public AttachmentCacheProperty getCacheProperty() {
        return this.tacz$data.cacheProperty;
    }

    @Unique
    @Override
    public void fireSelect() {
        this.tacz$fireSelect.fireSelect();
    }

    @Unique
    @Override
    public void zoom() {
        this.tacz$aim.zoom();
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTickServerSide(CallbackInfo ci) {
        // 仅在服务端调用
        if (!level().isClientSide()) {
            // 完成各种 tick 任务
            ReloadState reloadState = this.tacz$reload.tickReloadState();
            this.tacz$aim.tickAimingProgress();
            this.tacz$aim.tickSprint();
            this.tacz$crawl.tickCrawling();
            this.tacz$bolt.tickBolt();
            this.tacz$melee.scheduleTickMelee();
            this.tacz$speed.updateSpeedModifier();
            // 从服务端同步数据
            ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$shoot.getShootCoolDown());
            ModSyncedEntityData.MELEE_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$melee.getMeleeCoolDown());
            ModSyncedEntityData.DRAW_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$draw.getDrawCoolDown());
            ModSyncedEntityData.BOLT_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$data.boltCoolDown);
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
