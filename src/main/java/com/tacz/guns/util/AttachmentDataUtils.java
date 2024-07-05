package com.tacz.guns.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class AttachmentDataUtils {
    public static void getAllAttachmentData(ItemStack gunItem, GunData gunData, Consumer<AttachmentData> dataConsumer) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, type);
            if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
                continue;
            }
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
            if (attachmentData != null) {
                dataConsumer.accept(attachmentData);
            } else {
                TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> dataConsumer.accept(index.getData()));
            }
        }
    }

    public static int getAmmoCountWithAttachment(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return gunData.getAmmoAmount();
        }
        int[] extendedMagAmmoAmount = gunData.getExtendedMagAmmoAmount();
        if (extendedMagAmmoAmount == null) {
            return gunData.getAmmoAmount();
        }
        ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, AttachmentType.EXTENDED_MAG);
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return gunData.getAmmoAmount();
        }
        AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
        if (attachmentData != null) {
            int level = attachmentData.getExtendedMagLevel();
            if (level <= 0 || level > 3) {
                return gunData.getAmmoAmount();
            }
            return extendedMagAmmoAmount[level];
        } else {
            return TimelessAPI.getCommonAttachmentIndex(attachmentId).map(index -> {
                int level = index.getData().getExtendedMagLevel();
                if (level <= 0 || level > 3) {
                    return gunData.getAmmoAmount();
                }
                return extendedMagAmmoAmount[level - 1];
            }).orElse(gunData.getAmmoAmount());
        }
    }
}
