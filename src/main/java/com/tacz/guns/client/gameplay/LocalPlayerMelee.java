package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerMelee;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import com.tacz.guns.resource.pojo.data.gun.GunDefaultMeleeData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

public class LocalPlayerMelee {
    public static final String MELEE_STOCK_ANIMATION = "melee_stock";
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerMelee(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void melee() {
        // 检查状态锁
        if (data.clientStateLock) {
            return;
        }
        // 暂定为主手
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        GunDisplayInstance display = TimelessAPI.getGunDisplay(mainhandItem).orElse(null);
        if (display == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        // 先检查枪口有没有近战属性
        ResourceLocation muzzleId = iGun.getAttachmentId(mainhandItem, AttachmentType.MUZZLE);
        MeleeData muzzleMeleeData = getMeleeData(muzzleId);
        if (muzzleMeleeData != null) {
            this.doMuzzleMelee(display);
            return;
        }

        ResourceLocation stockId = iGun.getAttachmentId(mainhandItem, AttachmentType.STOCK);
        MeleeData stockMeleeData = getMeleeData(stockId);
        if (stockMeleeData != null) {
            this.doStockMelee(display);
            return;
        }

        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            GunDefaultMeleeData defaultMeleeData = index.getGunData().getMeleeData().getDefaultMeleeData();
            if (defaultMeleeData == null) {
                return;
            }
            String animationType = defaultMeleeData.getAnimationType();
            if (MELEE_STOCK_ANIMATION.equals(animationType)) {
                this.doStockMelee(display);
                return;
            }
            this.doPushMelee(display);
        });
    }

    private boolean prepareMelee() {
        // 锁上状态锁
        data.lockState(operator -> operator.getSynMeleeCoolDown() > 0);
        // 触发近战事件
        GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(player, player.getMainHandItem(), LogicalSide.CLIENT);
        return !MinecraftForge.EVENT_BUS.post(gunMeleeEvent);
    }

    private void doMuzzleMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            SoundPlayManager.playMeleeBayonetSound(player, display);
            // 发送执行近战的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
            // 动画状态机转移状态
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_MUZZLE);
            }
        }
    }

    private void doStockMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            SoundPlayManager.playMeleeStockSound(player, display);
            // 发送执行近战的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
            // 动画状态机转移状态
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_STOCK);
            }
        }
    }

    private void doPushMelee(GunDisplayInstance display) {
        if (prepareMelee()) {
            // 播放音效
            SoundPlayManager.playMeleePushSound(player, display);
            // 发送执行近战的数据包，通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
            // 动画状态机转移状态
            AnimationStateMachine<?> animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_BAYONET_PUSH);
            }
        }
    }

    @Nullable
    private MeleeData getMeleeData(ResourceLocation attachmentId) {
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return null;
        }
        return TimelessAPI.getClientAttachmentIndex(attachmentId).map(index -> index.getData().getMeleeData()).orElse(null);
    }
}
