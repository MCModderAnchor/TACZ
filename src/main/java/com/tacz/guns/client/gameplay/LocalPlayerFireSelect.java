package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.event.common.GunFireSelectEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.ClientMessagePlayerFireSelect;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public class LocalPlayerFireSelect {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerFireSelect(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void fireSelect() {
        // 检查状态锁及蓄力状态
        if (data.clientStateLock || data.isCharging) {
            return;
        }
        // 暂定为主手
        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (NeoForge.EVENT_BUS.post(new GunFireSelectEvent(player, player.getMainHandItem(), LogicalSide.CLIENT)).isCanceled()) {
            return;
        }

        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(gunIndex -> {
            // 播放音效
            SoundPlayManager.playFireSelectSound(player, gunIndex);
            // 发送切换开火模式的数据包，通知服务器
            PacketDistributor.sendToServer(ClientMessagePlayerFireSelect.INSTANCE);
            // 客户端切换开火模式
            if (iGun instanceof AbstractGunItem logicGun) {
                logicGun.fireSelect(null, mainHandItem);
            }
            AttachmentPropertyManager.postChangeEvent(player, mainHandItem);
            // 动画状态机转移状态
            AnimationStateMachine<?> animationStateMachine = gunIndex.getAnimationStateMachine();
            if (animationStateMachine != null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_FIRE_SELECT);
            }
        });
    }
}
