package com.tacz.guns.client.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.attachment.AttachmentType;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.internal.GunAnimationStateMachine;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerReloadGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

public class LocalPlayerReload {
    private final LocalPlayerDataHolder data;
    private final LocalPlayer player;

    public LocalPlayerReload(LocalPlayerDataHolder data, LocalPlayer player) {
        this.data = data;
        this.player = player;
    }

    public void reload() {
        // 暂定只有主手可以装弹
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查状态锁
            if (data.clientStateLock) {
                return;
            }
            // 弹药简单检查
            if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !inventoryHasAmmo(iGun, gunIndex, mainhandItem)) {
                return;
            }
            // 锁上状态锁
            data.lockState(operator -> operator.getSynReloadState().getStateType().isReloading());
            // 触发换弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(player, player.getMainHandItem(), LogicalSide.CLIENT))) {
                return;
            }
            // 发包通知服务器
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerReloadGun());
            // 执行客户端 reload 相关内容
            this.doReload(iGun, gunIndex, mainhandItem);
        });
    }

    private void doReload(IGun iGun, ClientGunIndex gunIndex, ItemStack mainhandItem) {
        GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
        if (animationStateMachine != null) {
            Bolt boltType = gunIndex.getGunData().getBolt();
            boolean noAmmo;
            if (boltType == Bolt.OPEN_BOLT) {
                noAmmo = iGun.getCurrentAmmoCount(mainhandItem) <= 0;
            } else {
                noAmmo = !iGun.hasBulletInBarrel(mainhandItem);
            }
            // TODO 这块没完全弄好，目前还有问题
            // this.playMagExtendedAnimation(mainhandItem, iGun, animationStateMachine);
            // 触发 reload，停止播放声音
            SoundPlayManager.stopPlayGunSound();
            SoundPlayManager.playReloadSound(player, gunIndex, noAmmo);
            animationStateMachine.setNoAmmo(noAmmo).onGunReload();
        }
    }

    // TODO 这块没完全弄好，目前还有问题
    private void playMagExtendedAnimation(ItemStack mainhandItem, IGun iGun, GunAnimationStateMachine animationStateMachine) {
        ItemStack extendedMagItem = iGun.getAttachment(mainhandItem, AttachmentType.EXTENDED_MAG);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(extendedMagItem);
        if (iAttachment != null) {
            TimelessAPI.getCommonAttachmentIndex(iAttachment.getAttachmentId(extendedMagItem)).ifPresent(index -> {
                animationStateMachine.setMagExtended(index.getData().getExtendedMagLevel() > 0);
            });
        }
    }

    private boolean inventoryHasAmmo(IGun iGun, ClientGunIndex gunIndex, ItemStack mainhandItem) {
        // 满弹检查也放这，这样创造模式玩家随意随便换弹
        // 满弹不需要换
        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(mainhandItem, gunIndex.getGunData());
        if (iGun.getCurrentAmmoCount(mainhandItem) >= maxAmmoCount) {
            return false;
        }
        // 背包弹药检查
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack checkAmmo = inventory.getItem(i);
            if (checkAmmo.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(mainhandItem, checkAmmo)) {
                return true;
            }
            if (checkAmmo.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(mainhandItem, checkAmmo)) {
                return true;
            }
        }
        return false;
    }
}
