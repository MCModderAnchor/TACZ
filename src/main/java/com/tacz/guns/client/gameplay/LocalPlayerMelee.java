package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.handshake.ClientMessagePlayerMelee;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
        ItemStack attachmentStack = iGun.getAttachment(mainhandItem, AttachmentType.MUZZLE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment == null) {
            return;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentStack);
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            this.doClientMelee(index, gunId);
            // 切换状态锁，不允许换弹、检视等行为进行。
            data.lockState(operator -> operator.getSynMeleeCoolDown() > 0);
        });
    }

    private void doClientMelee(ClientAttachmentIndex index, ResourceLocation gunId) {
        MeleeData meleeData = index.getData().getMeleeData();
        if (meleeData == null) {
            return;
        }
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            // 播放音效
            // SoundPlayManager.playFireSelectSound(player, gunIndex);
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
