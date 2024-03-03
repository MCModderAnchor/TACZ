package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.entity.IShooter;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.sound.SoundPlayManager;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessagePlayerShoot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ShootKey {
    public static final KeyMapping SHOOT_KEY = new KeyMapping("key.tac.shoot.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            "key.category.tac");

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END && !isInGame()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (SHOOT_KEY.isDown() && player instanceof IShooter shooter && IGun.mainhandHoldGun(player)) {
            ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
            ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                long shootCoolDown = gunIndex.getGunData().getShootInterval() - (System.currentTimeMillis() - shooter.getShootTime());
                // 如果开火冷却时间剩余大于 1 个 tick (即 50 ms)，则不能开火。
                if(shootCoolDown > 50){
                    return;
                }
                // 如果开火冷却时间剩余小于 1 个 tick ，则认为玩家已经开火。
                // 触发开火事件
                if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                    return;
                }
                // 发送开火的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerShoot());
                // 开火效果需要延时执行，这样渲染效果更好。
                scheduledExecutorService.schedule(() -> {
                    // 记录新的开火时间戳
                    shooter.recordShootTime();
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
                }, shootCoolDown, TimeUnit.MILLISECONDS);
            });
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (mc.getOverlay() != null) {
            return false;
        }
        if (mc.screen != null) {
            return false;
        }
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        return mc.isWindowActive();
    }
}
