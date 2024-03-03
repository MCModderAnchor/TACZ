package com.tac.guns.mixin.client;

import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.event.GunReloadEvent;
import com.tac.guns.api.event.GunFireSelectEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessagePlayerReloadGun;
import com.tac.guns.network.message.ClientMessagePlayerFireSelect;
import com.tac.guns.network.message.ClientMessagePlayerShoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements IClientPlayerGunOperator {
    @Shadow public Input input;
    @Unique
    private static final ScheduledExecutorService tac$ScheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Unique
    private long tac$ClientShootTimestamp = -1L;
    @Unique
    private boolean tac$IsReloadRecorded = true;

    @Unique
    @Override
    public ShootResult shoot() {
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if(!tac$IsReloadRecorded){
            return ShootResult.COOL_DOWN;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
        Optional<ClientGunIndex> gunIndexOptional = ClientGunPackLoader.getGunIndex(gunId);
        if(gunIndexOptional.isEmpty()){
            return ShootResult.FAIL;
        }
        ClientGunIndex gunIndex = gunIndexOptional.get();
        if (IGun.mainhandHoldGun(player)) {
            long coolDown = gunIndex.getGunData().getShootInterval() - (System.currentTimeMillis() - tac$ClientShootTimestamp);
            // 如果射击冷却大于 1 tick (即 50 ms)，则不允许开火
            if(coolDown > 50){
                return ShootResult.COOL_DOWN;
            }
            // 如果射击冷却小于 1 tick，则认为玩家已经开火，但开火的客户端效果异步延迟执行。
            if(coolDown < 0){
                coolDown = 0;
            }
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if(reloadState.isReloading()){
                return ShootResult.FAIL;
            }
            // todo 判断是否在 draw
            // 触发开火事件
            if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return ShootResult.FAIL;
            }
            tac$IsReloadRecorded = false;
            // 开火效果需要延时执行，这样渲染效果更好。
            tac$ScheduledExecutorService.schedule(() -> {
                // 记录新的开火时间戳
                tac$ClientShootTimestamp = System.currentTimeMillis();
                // 转换 isRecord 状态，允许下一个tick的开火检测。
                tac$IsReloadRecorded = true;
                // 发送开火的数据包，通知服务器。暂时只考虑主手能打枪。
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot(player.getInventory().selected));
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onGunShoot();
                }
                // 播放声音、摄像机后坐需要从异步线程上传到主线程执行。
                Minecraft.getInstance().submitAsync(() ->{
                    SoundPlayManager.playClientSound(player, gunIndex.getSounds("shoot"), 1.0f, 0.8f);
                    player.setXRot(player.getXRot() - 0.5f);
                });
            }, coolDown, TimeUnit.MILLISECONDS);
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }

    @Unique
    @Override
    public void draw() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // todo 重置各个状态
        ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
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
        ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            // 判断换弹是否未完成
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if(!reloadState.isReloadFinished()){
                return;
            }
            // 判断是否正在开火冷却
            if(IGunOperator.fromLivingEntity(player).getSynShootCoolDown() > 0){
                return;
            }
            // todo 检查 draw 是否还未完成
            // 触发换弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerReloadGun(player.getInventory().selected));
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                // todo 判断枪内是否有余弹
                animationStateMachine.setNoAmmo(true);
                animationStateMachine.onGunReload();
            }
        });
    }

    @Unique
    @Override
    public void inspect() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // todo 检测是否在开火、装弹、切枪、检视
        ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
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
        if (IGun.mainhandHoldGun(player)) {
            ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
            ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                    return;
                }
                // 发送切换开火模式的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerFireSelect());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onGunFireSelect();
                }
            });
        }
    }
}
