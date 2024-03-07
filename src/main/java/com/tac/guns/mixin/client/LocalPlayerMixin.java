package com.tac.guns.mixin.client;

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
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.*;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.GunData;
import com.tac.guns.resource.pojo.data.GunRecoil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements IClientPlayerGunOperator {
    @Unique
    private static final ScheduledExecutorService tac$ScheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Shadow
    public abstract void sendMessage(Component pComponent, UUID uuid);

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
     * 瞄准时间戳，单位 ms
     */
    @Unique
    private long tac$ClientAimingTimestamp = -1L;
    @Unique
    private boolean tac$ClientIsAiming = false;

    @Unique
    @Override
    public ShootResult shoot() {
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if (!tac$IsShootRecorded) {
            return ShootResult.COOL_DOWN;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return ShootResult.FAIL;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        Optional<ClientGunIndex> gunIndexOptional = ClientGunPackLoader.getGunIndex(gunId);
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
            // 如果射击冷却小于 1 tick，则认为玩家已经开火，但开火的客户端效果异步延迟执行。
            if (coolDown < 0) {
                coolDown = 0;
            }
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if (reloadState.getStateType().isReloading()) {
                return ShootResult.FAIL;
            }
            // 判断子弹数
            if (this.checkAmmo() && iGun.getCurrentAmmoCount(mainhandItem) < 1) {
                return ShootResult.NO_AMMO;
            }
            // TODO 判断是否在 draw
            // 触发开火事件
            if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, mainhandItem, LogicalSide.CLIENT))) {
                return ShootResult.FAIL;
            }
            tac$IsShootRecorded = false;
            // 开火效果需要延时执行，这样渲染效果更好。
            tac$ScheduledExecutorService.schedule(() -> {
                // 记录新的开火时间戳
                tac$ClientShootTimestamp = System.currentTimeMillis();
                // 转换 isRecord 状态，允许下一个tick的开火检测。
                tac$IsShootRecorded = true;
                // 发送开火的数据包，通知服务器。暂时只考虑主手能打枪。
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onGunShoot();
                }
                // 播放声音、摄像机后坐需要从异步线程上传到主线程执行。
                Minecraft.getInstance().submitAsync(() -> {
                    // TODO 应该发包，让周围的玩家都能听到
                    SoundPlayManager.playShootSound(player, gunIndex);
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
    public boolean checkAmmo() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        return !player.isCreative();
    }

    @Unique
    @Override
    public void draw() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 重置客户端的 shoot 时间戳
        tac$IsShootRecorded = true;
        tac$ClientShootTimestamp = -1;
        // 重置客户端瞄准状态
        tac$ClientIsAiming = false;
        tac$ClientAimingProgress = 0;
        // 发包通知服务器
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerDrawGun(player.getInventory().selected));
        // 放映 draw 动画
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
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
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查换弹是否未完成
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if (reloadState.getStateType().isReloading()) {
                return;
            }
            // 判断是否正在开火冷却
            if (IGunOperator.fromLivingEntity(player).getSynShootCoolDown() > 0) {
                return;
            }
            // 弹药简单检查
            if (this.checkAmmo()) {
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
            // TODO 检查 draw 是否还未完成
            // 触发换弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerReloadGun());
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.setNoAmmo(iGun.getCurrentAmmoCount(mainhandItem) <= 0);
                animationStateMachine.onGunReload();
            }
        });
    }

    @Unique
    @Override
    public void inspect() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // TODO 检测是否在开火、装弹、切枪、检视
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.onGunInspect();
            }
        });
    }

    @Override
    public void fireSelect() {
        // TODO 冷却时间检查，得让动画播放完毕才行
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
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
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
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            // TODO 发个 GunAimingEvent
            // TODO 判断能不能瞄准
            tac$ClientIsAiming = isAim;
            // 发送切换开火模式的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAim(isAim));
        });
    }

    @Unique
    @Override
    public float getClientAimingProgress() {
        return tac$ClientAimingProgress;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickClientSide(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.getLevel().isClientSide()) {
            tickAimingProgress();
        }
    }

    @Unique
    private void tickAimingProgress() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack mainhandItem = player.getMainHandItem();
        // 如果主手物品不是枪械，则取消瞄准状态并将 aimingProgress 归零，返回。
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            tac$ClientAimingProgress = 0;
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            tac$ClientAimingProgress = 0;
            return;
        }
        float aimTime = gunIndexOptional.get().getGunData().getAimTime();
        float alphaProgress = (System.currentTimeMillis() - tac$ClientAimingTimestamp + 1) / (aimTime * 1000);
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
}
