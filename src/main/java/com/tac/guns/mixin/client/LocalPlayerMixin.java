package com.tac.guns.mixin.client;

import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.event.GunFireSelectEvent;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessagePlayerFireSelect;
import com.tac.guns.network.message.ClientMessagePlayerShoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin implements IClientPlayerGunOperator {
    @Unique
    private static final ScheduledExecutorService tac$ScheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Unique
    private long tac$ShootTimestamp = -1L;

    @Unique
    private long tac$DrawTimestamp = -1L;

    @Unique
    private long tac$ReloadTimestamp = -1L;

    @Unique
    private boolean tac$IsRecorded = true;

    @Override
    public ShootResult shoot() {
        // 如果上一次异步开火的效果还未执行，则直接返回，等待异步开火效果执行
        if (!tac$IsRecorded) {
            return ShootResult.COOL_DOWN;
        }
        // todo 判断是否在装弹
        // todo 判断是否在 draw
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (IGun.mainhandHoldGun(player)) {
            ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
            ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                long shootCoolDown = gunIndex.getGunData().getShootInterval() - (System.currentTimeMillis() - tac$ShootTimestamp);
                // 如果开火冷却时间剩余大于 1 个 tick (即 50 ms)，则不能开火。
                if (shootCoolDown > 50) {
                    return;
                }
                // 如果开火冷却时间剩余小于 1 个 tick ，则认为玩家已经开火。
                // 触发开火事件
                if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                    return;
                }
                // 发送开火的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
                tac$IsRecorded = false;
                // 开火效果需要延时执行，这样渲染效果更好。
                tac$ScheduledExecutorService.schedule(() -> {
                    // 记录新的开火时间戳
                    tac$ShootTimestamp = System.currentTimeMillis();
                    // 转换 isRecord 状态，允许下一个tick的开火检测。
                    tac$IsRecorded = true;
                    // 动画状态机转移状态
                    GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                    if (animationStateMachine != null) {
                        animationStateMachine.onGunShoot();
                    }
                    // 播放声音、摄像机后坐需要从异步线程上传到主线程执行。
                    Minecraft.getInstance().submitAsync(() -> {
                        SoundPlayManager.playClientSound(player, gunIndex.getSounds("shoot"), 1.0f, 0.8f);
                        player.setXRot(player.getXRot() - 0.5f);
                    });
                }, shootCoolDown, TimeUnit.MILLISECONDS);
            });
        }
        return ShootResult.FAIL;
    }

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

    @Override
    public void reload() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.onGunReload();
            }
        });
        // todo 发包通知服务器
    }

    @Override
    public void inspect() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        // todo 检测是否在开火
        // todo 检测是否在装弹
        // todo 检测是否在切枪
        // todo 检测是否在检视
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
