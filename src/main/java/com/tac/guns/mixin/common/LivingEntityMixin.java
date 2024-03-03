package com.tac.guns.mixin.common;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunReloadEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IGun;
import com.tac.guns.item.GunItem;
import com.tac.guns.api.network.MyEntityDataSerializers;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.GunData;
import com.tac.guns.resource.pojo.data.GunReloadData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
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
    private long tac$ShootTimestamp = -1L;

    @Unique
    private long tac$DrawTimestamp = -1L;

    @Unique
    @Nonnull
    private final ReloadState tac$ReloadState = new ReloadState();

    @Unique
    @Nonnull
    private ReloadState.StateType tac$LastTickReloadStateType = ReloadState.StateType.NOT_RELOADING;

    @Unique
    @Nullable
    private ItemStack tac$ReloadingGun = null;

    @Unique
    @Nullable
    private ItemStack tac$ShootingGun = null;

    @Override
    @Unique
    public long getSynShootCoolDown(){
        return this.getEntityData().get(DATA_SHOOT_COOL_DOWN_ID);
    }

    @Override
    @Unique
    public ReloadState getSynReloadState() {
        return this.getEntityData().get(DATA_RELOAD_STATE_ID);
    }

    @Unique
    public long getShootCoolDown(){
        if(tac$ShootingGun == null){
            return 0;
        }
        ResourceLocation gunId = GunItem.getData(tac$ShootingGun).getGunId();
        Optional<CommonGunIndex> gunIndex = CommonGunPackLoader.getGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval() - (System.currentTimeMillis() - tac$ShootTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown -= 5;
            if(coolDown < 0) return 0L;
            return coolDown;
        }).orElse(-1L);
    }

    @Override
    public void draw(ItemStack gunItemStack) {
        // todo
    }

    @Override
    public void reload(ItemStack gunItemStack) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ResourceLocation gunId = GunItem.getData(gunItemStack).getGunId();
        CommonGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查换弹是否还未完成
            if(!tac$ReloadState.isReloadFinished()){
                return;
            }
            // 检查是否正在开火冷却
            if(getShootCoolDown() != 0){
                return;
            }
            // todo 检查 draw 是否还未完成
            // 触发装弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(entity, gunItemStack, LogicalSide.SERVER))) {
                return;
            }
            // todo 根据枪内子弹数量 startReload
            tac$ReloadState.startReloadEmpty();
            tac$ReloadingGun = gunItemStack;
        });
    }

    @Override
    public ShootResult shoot(ItemStack gunItemStack, float pitch, float yaw) {
        // 判断射击是否正在冷却
        long coolDown = getShootCoolDown();
        if(coolDown == -1){
            return ShootResult.FAIL;
        }
        if (coolDown > 0) {
            return ShootResult.COOL_DOWN;
        }
        // 检查是否正在换弹
        if(tac$ReloadState.isReloading()){
            return ShootResult.FAIL;
        }
        // todo 检查 draw 是否还未完成
        LivingEntity entity = (LivingEntity) (Object) this;
        // 触发射击事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(entity, gunItemStack, LogicalSide.SERVER))) {
            return ShootResult.FAIL;
        }
        // todo 判断枪械是否有足够的弹药
        // 调用射击方法
        if(gunItemStack.getItem() instanceof IGun iGun){
            tac$ShootingGun = gunItemStack;
            iGun.shoot(entity, gunItemStack, pitch, yaw);
            tac$ShootTimestamp = System.currentTimeMillis();
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTickServerSide(CallbackInfo ci){
        LivingEntity entity = (LivingEntity) (Object) this;
        // 以下 tick 逻辑仅发生在服务端，因此如果为客户端调用，返回。
        if(entity.getLevel().isClientSide()){
            return;
        }
        if(tac$ReloadingGun != null) {
            ResourceLocation gunId = GunItem.getData(tac$ReloadingGun).getGunId();
            CommonGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                GunReloadData reloadData = gunIndex.getGunData().getReloadData();
                tac$ReloadState.tick(reloadData);
            });
        }
        if(tac$LastTickReloadStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING){
            if(tac$ReloadState.getStateType() != ReloadState.StateType.EMPTY_RELOAD_FEEDING){
                // todo 更改枪的弹药数
            }
        }
        if(tac$LastTickReloadStateType == ReloadState.StateType.NORMAL_RELOAD_FEEDING){
            if(tac$ReloadState.getStateType() != ReloadState.StateType.NORMAL_RELOAD_FEEDING){
                // todo 更改枪的弹药数
            }
        }
        tac$LastTickReloadStateType = tac$ReloadState.getStateType();
        // 从服务端同步数据
        this.getEntityData().set(DATA_SHOOT_COOL_DOWN_ID, getShootCoolDown());
        this.getEntityData().set(DATA_RELOAD_STATE_ID, new ReloadState(tac$ReloadState));
    }

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    public void defineSynData(CallbackInfo ci) {
        entityData.define(DATA_SHOOT_COOL_DOWN_ID, -1L);
        entityData.define(DATA_RELOAD_STATE_ID, new ReloadState());
    }

    @Override
    public FireMode fireSelect(ItemStack gunItemStack) {
        // 获取GunData
        GunItemData gunItemData = GunItem.getData(gunItemStack);
        ResourceLocation gunId = gunItemData.getGunId();
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return FireMode.SEMI;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        if (gunItemStack.getItem() instanceof IGun iGun) {
            FireMode fireMode = iGun.getFireMode(gunItemStack);
            List<FireMode> fireModeSet = gunData.getFireModeSet();
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            gunItemData.setFireMode(nextFireMode);
            GunItem.setData(gunItemStack, gunItemData);
            return nextFireMode;
        }
        return FireMode.SEMI;
    }
}
