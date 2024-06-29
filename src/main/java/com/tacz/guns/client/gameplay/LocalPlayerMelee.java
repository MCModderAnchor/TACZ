package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.internal.GunAnimationConstant;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
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
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;
    private int meleeCounter = 0;

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
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        // 先检查枪口有没有近战属性
        ItemStack muzzle = iGun.getAttachment(mainhandItem, AttachmentType.MUZZLE);
        MeleeData muzzleMeleeData = getMeleeData(muzzle);
        if (muzzleMeleeData != null) {
            this.doMuzzleMelee(gunId);
            return;
        }

        ItemStack stock = iGun.getAttachment(mainhandItem, AttachmentType.STOCK);
        MeleeData stockMeleeData = getMeleeData(stock);
        if (stockMeleeData != null) {
            this.doStockMelee(gunId);
            return;
        }

        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            GunDefaultMeleeData defaultMeleeData = index.getGunData().getMeleeData().getDefaultMeleeData();
            if (defaultMeleeData == null) {
                return;
            }
            String animationType = defaultMeleeData.getAnimationType();
            if (GunAnimationConstant.MELEE_STOCK_ANIMATION.equals(animationType)) {
                this.doStockMelee(gunId);
                return;
            }
            this.doPushMelee(gunId);
        });
    }

    private boolean prepareMelee() {
        // 锁上状态锁
        data.lockState(operator -> operator.getSynMeleeCoolDown() > 0);
        // 触发近战事件
        GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(player, player.getMainHandItem(), LogicalSide.CLIENT);
        return !MinecraftForge.EVENT_BUS.post(gunMeleeEvent);
    }

    private void doMuzzleMelee(ResourceLocation gunId) {
        if (prepareMelee()) {
            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
                // 播放音效
                SoundPlayManager.playMeleeBayonetSound(player, gunIndex);
                // 发送切换开火模式的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onBayonetAttack(meleeCounter);
                    meleeCounter = (meleeCounter + 1) % 3;
                }
            });
        }
    }

    private void doStockMelee(ResourceLocation gunId) {
        if (prepareMelee()) {
            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
                // 播放音效
                SoundPlayManager.playMeleeStockSound(player, gunIndex);
                // 发送切换开火模式的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onStockAttack();
                }
            });
        }
    }

    private void doPushMelee(ResourceLocation gunId) {
        if (prepareMelee()) {
            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
                // 播放音效
                SoundPlayManager.playMeleePushSound(player, gunIndex);
                // 发送切换开火模式的数据包，通知服务器
                NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerMelee());
                // 动画状态机转移状态
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine != null) {
                    animationStateMachine.onPushAttack();
                }
            });
        }
    }

    @Nullable
    private MeleeData getMeleeData(ItemStack attachmentStack) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment == null) {
            return null;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentStack);
        return TimelessAPI.getClientAttachmentIndex(attachmentId).map(index -> index.getData().getMeleeData()).orElse(null);
    }
}
