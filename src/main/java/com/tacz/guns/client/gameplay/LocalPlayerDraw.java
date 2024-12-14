package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.event.common.GunDrawEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerDrawGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LocalPlayerDraw {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerDraw(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void draw(ItemStack lastItem) {
        // 重置各种参数
        this.resetData();

        // 获取各种数据
        ItemStack currentItem = player.getMainHandItem();
        long drawTime = System.currentTimeMillis() - data.clientDrawTimestamp;
        IGun currentGun = IGun.getIGunOrNull(currentItem);
        IGun lastGun = IGun.getIGunOrNull(lastItem);

        // 计算 draw 时长和 putAway 时长
        if (drawTime >= 0) {
            drawTime = getDrawTime(lastItem, lastGun, drawTime);
        }
        long putAwayTime = Math.abs(drawTime);

        // 发包通知服务器
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.ensureHasSentCarriedItem();
        }
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerDrawGun());
        MinecraftForge.EVENT_BUS.post(new GunDrawEvent(player, lastItem, currentItem, LogicalSide.CLIENT));

        // 不处于收枪状态时才能收枪
        if (drawTime >= 0) {
            doPutAway(lastItem, lastGun, putAwayTime);
        }

        // 异步放映抬枪动画
        if (currentGun != null) {
            doDraw(currentItem, putAwayTime);
            // 刷新配件数据
            AttachmentPropertyManager.postChangeEvent(player, currentItem);
        }
    }

    private void doDraw(ItemStack currentItem, long putAwayTime) {
        TimelessAPI.getGunDisplay(currentItem).ifPresent(display -> {
            // 初始化状态机
            AnimationStateMachine<GunAnimationStateContext> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine == null) {
                return;
            }
            if (animationStateMachine.isInitialized()) {
                animationStateMachine.exit();
            }
            GunAnimationStateContext context = new GunAnimationStateContext();
            context.setCurrentGunItem(currentItem);
            animationStateMachine.setContext(context);
            animationStateMachine.initialize();
            // 取消预定中的 draw 行为
            if (data.drawFuture != null) {
                data.drawFuture.cancel(false);
            }
            // 根据 put away time 预定 draw 行为
            data.drawFuture = LocalPlayerDataHolder.SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
                Minecraft.getInstance().submitAsync(() -> {
                    animationStateMachine.trigger(GunAnimationConstant.INPUT_DRAW);
                    SoundPlayManager.stopPlayGunSound();
                    SoundPlayManager.playDrawSound(player, display);
                });
            }, putAwayTime, TimeUnit.MILLISECONDS);
        });
    }

    private void doPutAway(ItemStack lastItem, IGun lastGun, long putAwayTime) {
        if (lastGun == null) {
            return;
        }
        TimelessAPI.getGunDisplay(lastItem).ifPresent(display -> {
            Minecraft.getInstance().submitAsync(() -> {
                // 播放收枪音效
                SoundPlayManager.stopPlayGunSound();
                SoundPlayManager.playPutAwaySound(player, display);
            });
            // 播放收枪动画
            AnimationStateMachine<GunAnimationStateContext> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.processContextIfExist(context -> {
                    context.setPutAwayTime(putAwayTime / 1000F);
                    context.setCurrentGunItem(lastItem);
                });
                animationStateMachine.trigger(GunAnimationConstant.INPUT_PUT_AWAY);
                // 保持枪械的渲染直到收枪动作完成
                KeepingItemRenderer.getRenderer().keep(lastItem, putAwayTime);
                // 退出状态机
                if (animationStateMachine.isInitialized()) {
                    animationStateMachine.exit();
                }
            }
        });
    }

    private long getDrawTime(ItemStack lastItem, IGun lastGun, long drawTime) {
        if (lastGun != null) {
            // 如果不处于收枪状态，则需要加上收枪的时长
            Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(lastGun.getGunId(lastItem));
            float putAwayTime = gunIndex.map(index -> index.getGunData().getPutAwayTime()).orElse(0F);
            if (drawTime > putAwayTime * 1000) {
                drawTime = (long) (putAwayTime * 1000);
            }
            data.clientDrawTimestamp = System.currentTimeMillis() + drawTime;
        } else {
            drawTime = 0;
            data.clientDrawTimestamp = System.currentTimeMillis();
        }
        return drawTime;
    }

    private void resetData() {
        // 锁上状态锁
        data.lockState(operator -> operator.getSynDrawCoolDown() > 0);
        // 重置客户端的 shoot 时间戳
        data.isShootRecorded = true;
        data.clientShootTimestamp = -1;
        // 重置客户端瞄准状态
        data.clientIsAiming = false;
        data.clientAimingProgress = 0;
        LocalPlayerDataHolder.oldAimingProgress = 0;
        // 重置拉栓状态
        data.isBolting = false;
        // 更新切枪时间戳
        if (data.clientDrawTimestamp == -1) {
            data.clientDrawTimestamp = System.currentTimeMillis();
        }
    }
}
