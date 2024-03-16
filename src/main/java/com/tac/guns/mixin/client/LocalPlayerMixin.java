package com.tac.guns.mixin.client;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunFireSelectEvent;
import com.tac.guns.api.event.GunReloadEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.*;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.gun.GunData;
import com.tac.guns.resource.pojo.data.gun.GunRecoil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements IClientPlayerGunOperator {
    @Unique
    private static final ScheduledExecutorService tac$ScheduledExecutorService = Executors.newScheduledThreadPool(1);
    @Unique
    private static final Predicate<IGunOperator> tac$ShootLockedCondition = operator -> operator.getSynShootCoolDown() > 0;

    @Unique
    private volatile long tac$ClientShootTimestamp = -1L;
    @Unique
    private volatile boolean tac$IsShootRecorded = true;
    /**
     * 瞄准的进度，范围 0 ~ 1
     */
    @Unique
    private float tac$ClientAimingProgress = 0;
    /**
     * 上一个 tick 的瞄准进度，用于插值，范围 0 ~ 1
     */
    @Unique
    private static float tac$OldAimingProgress = 0;
    /**
     * 瞄准时间戳，单位 ms
     */
    @Unique
    private long tac$ClientAimingTimestamp = -1L;
    @Unique
    private boolean tac$ClientIsAiming = false;

    /**
     * 这个状态锁表示：任意时刻，正在进行的枪械操作只能为一个。
     * 主要用于防止客户端操作表现效果重复执行。
     */
    @Unique
    private volatile boolean tac$ClientStateLock = false;
    /**
     * 用于跳过状态锁上锁 到 服务端数据更新的这段延迟...
     */
    @Unique
    @Nullable
    private Predicate<IGunOperator> tac$LockedCondition = null;

    @Unique
    @Override
    public ShootResult shoot() {
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if (!tac$IsShootRecorded) {
            return ShootResult.COOL_DOWN;
        }
        // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
        if (tac$ClientStateLock && tac$LockedCondition != tac$ShootLockedCondition && tac$LockedCondition != null){
            tac$IsShootRecorded = true;
            return ShootResult.FAIL;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为只有主手能开枪
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return ShootResult.FAIL;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        Optional<ClientGunIndex> gunIndexOptional = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.FAIL;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        if (mainhandItem.getItem() instanceof IGun) {
            GunData gunData = gunIndex.getGunData();
            long coolDown = gunData.getShootInterval() - (System.currentTimeMillis() - tac$ClientShootTimestamp);
            // 如果射击冷却大于 1 tick (即 50 ms)，则不允许开火
            if (coolDown > 50) {
                return ShootResult.COOL_DOWN;
            }
            if (coolDown < 0) {
                coolDown = 0;
            }
            // 因为开火冷却检测用了特别定制的方法，所以不检查状态锁，而是手动检查是否换弹、切枪
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
            if (gunOperator.getSynReloadState().getStateType().isReloading()) {
                return ShootResult.FAIL;
            }
            if (gunOperator.getSynDrawCoolDown() != 0) {
                return ShootResult.FAIL;
            }
            // 判断子弹数
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && iGun.getCurrentAmmoCount(mainhandItem) < 1) {
                SoundPlayManager.playDryFireSound(player, gunIndex);
                return ShootResult.NO_AMMO;
            }
            // 触发开火事件
            if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, mainhandItem, LogicalSide.CLIENT))) {
                return ShootResult.FAIL;
            }
            // 切换状态锁，不允许换弹、检视等行为进行。
            lockState(tac$ShootLockedCondition);
            tac$IsShootRecorded = false;
            // 开火效果需要延时执行，这样渲染效果更好。
            tac$ScheduledExecutorService.schedule(() -> {
                // 转换 isRecord 状态，允许下一个tick的开火检测。
                tac$IsShootRecorded = true;
                // 如果状态锁正在准备锁定，且不是开火的状态锁，则不允许开火(主要用于防止切枪后开火动作覆盖切枪动作)
                if (tac$ClientStateLock && tac$LockedCondition != tac$ShootLockedCondition && tac$LockedCondition != null){
                    tac$IsShootRecorded = true;
                    return;
                }
                // 记录新的开火时间戳
                tac$ClientShootTimestamp = System.currentTimeMillis();
                // 发送开火的数据包，通知服务器。暂时只考虑主手能打枪。
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onGunShoot();
                }
                // 播放声音、摄像机后坐需要从异步线程上传到主线程执行。
                Minecraft.getInstance().submitAsync(() -> {
                    // 触发 shot，停止播放声音
                    SoundPlayManager.stopPlayGunSound();
                    GunRecoil recoil = gunData.getRecoil();
                    player.setXRot(player.getXRot() - recoil.getRandomPitch());
                    player.setYRot(player.getYRot() + recoil.getRandomYaw());
                });
            }, coolDown, TimeUnit.MILLISECONDS);
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }

    @Unique
    @Override
    public void draw() {
        // 触发 draw，先停止播放声音
        SoundPlayManager.stopPlayGunSound();
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 锁上状态锁
        lockState(operator -> operator.getSynDrawCoolDown() > 0);
        // 重置客户端的 shoot 时间戳
        tac$IsShootRecorded = true;
        tac$ClientShootTimestamp = -1;
        // 重置客户端瞄准状态
        tac$ClientIsAiming = false;
        tac$ClientAimingProgress = 0;
        tac$OldAimingProgress = 0;
        // 发包通知服务器
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerDrawGun(player.getInventory().selected));
        // 放映 draw 动画
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            SoundPlayManager.playDrawSound(player, gunIndex);
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.onGunDraw();
            }
        });
    }

    @Unique
    @Override
    public void reload() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定只有主手可以装弹
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查状态锁
            if (tac$ClientStateLock) {
                return;
            }
            // 弹药简单检查
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
                // 满弹检查也放这，这样创造模式玩家随意随便换弹
                // 满弹不需要换
                if (iGun.getCurrentAmmoCount(mainhandItem) >= gunIndex.getGunData().getAmmoAmount()) {
                    return;
                }
                // 背包弹药检查
                boolean hasAmmo = false;
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack checkAmmo = inventory.getItem(i);
                    if (checkAmmo.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(mainhandItem, checkAmmo)) {
                        hasAmmo = true;
                        break;
                    }
                }
                if (!hasAmmo) {
                    return;
                }
            }
            // 锁上状态锁
            lockState(operator -> operator.getSynReloadState().getStateType().isReloading());
            // 触发换弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerReloadGun());
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                boolean noAmmo = iGun.getCurrentAmmoCount(mainhandItem) <= 0;
                // 触发 reload，停止播放声音
                SoundPlayManager.stopPlayGunSound();
                SoundPlayManager.playReloadSound(player, gunIndex, noAmmo);
                animationStateMachine.setNoAmmo(noAmmo);
                animationStateMachine.onGunReload();
            }
        });
    }

    @Unique
    @Override
    public void inspect() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定只有主手可以检视
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 检查状态锁
        if (tac$ClientStateLock) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            boolean noAmmo = iGun.getCurrentAmmoCount(mainhandItem) <= 0;
            // 触发 inspect，停止播放声音
            SoundPlayManager.stopPlayGunSound();
            SoundPlayManager.playInspectSound(player, gunIndex, noAmmo);
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.setNoAmmo(noAmmo);
                animationStateMachine.onGunInspect();
            }
        });
    }

    @Override
    public void fireSelect() {
        // 检查状态锁
        if (tac$ClientStateLock) {
            return;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            // 发送切换开火模式的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerFireSelect());
            // 动画状态机转移状态
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.onGunFireSelect();
            }
        });
    }

    @Override
    public void aim(boolean isAim) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            // TODO 发个 GunAimingEvent
            // TODO 判断能不能瞄准
            tac$ClientIsAiming = isAim;
            // 发送切换开火模式的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAim(isAim));
        });
    }

    @Unique
    @Override
    public float getClientAimingProgress(float partialTicks) {
        return Mth.lerp(partialTicks, tac$OldAimingProgress, tac$ClientAimingProgress);
    }

    @Unique
    private void lockState(@Nullable Predicate<IGunOperator> lockedCondition) {
        tac$ClientStateLock = true;
        tac$LockedCondition = lockedCondition;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickClientSide(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.getLevel().isClientSide()) {
            tickAimingProgress();
            tickStateLock();
        }
    }

    @Unique
    private void tickAimingProgress() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack mainhandItem = player.getMainHandItem();
        // 如果主手物品不是枪械，则取消瞄准状态并将 aimingProgress 归零，返回。
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            tac$ClientAimingProgress = 0;
            tac$OldAimingProgress = 0;
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            tac$ClientAimingProgress = 0;
            tac$OldAimingProgress = 0;
            return;
        }
        float aimTime = gunIndexOptional.get().getGunData().getAimTime();
        float alphaProgress = (System.currentTimeMillis() - tac$ClientAimingTimestamp + 1) / (aimTime * 1000);
        tac$OldAimingProgress = tac$ClientAimingProgress;
        if (tac$ClientIsAiming) {
            // 处于执行瞄准状态，增加 aimingProgress
            tac$ClientAimingProgress += alphaProgress;
            if (tac$ClientAimingProgress > 1) {
                tac$ClientAimingProgress = 1;
            }
        } else {
            // 处于取消瞄准状态，减小 aimingProgress
            tac$ClientAimingProgress -= alphaProgress;
            if (tac$ClientAimingProgress < 0) {
                tac$ClientAimingProgress = 0;
            }
        }
        tac$ClientAimingTimestamp = System.currentTimeMillis();
    }

    /**
     * 此方法每 tick 执行一次，判断是否应当释放状态锁。
     */
    @Unique
    private void tickStateLock() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        ReloadState reloadState = gunOperator.getSynReloadState();
        // 如果还没完成上锁，则不能释放状态锁
        if (tac$LockedCondition != null && !tac$LockedCondition.test(gunOperator)) {
            return;
        }
        tac$LockedCondition = null;
        if (reloadState.getStateType().isReloading()) {
            return;
        }
        long shootCoolDown = gunOperator.getSynShootCoolDown();
        if (shootCoolDown > 0) {
            return;
        }
        if (gunOperator.getSynDrawCoolDown() > 0) {
            return;
        }
        // 释放状态锁
        tac$ClientStateLock = false;
    }

    @Override
    public boolean isAim() {
        return this.tac$ClientIsAiming;
    }
}
