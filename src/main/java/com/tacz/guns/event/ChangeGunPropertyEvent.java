package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ChangeGunPropertyEvent {
    @SubscribeEvent
    public static void onAttachmentPropertyEvent(AttachmentPropertyEvent event) {
        ItemStack gunItem = event.getGunItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> getAllAttachmentData(event, gunIndex, gunItem));
    }

    private static void getAllAttachmentData(AttachmentPropertyEvent event, CommonGunIndex gunIndex, ItemStack gunItem) {
        GunData gunData = gunIndex.getGunData();
        AttachmentCacheProperty cacheProperty = event.getCacheProperty();
        AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, data -> cacheProperty.eval(gunItem, gunData, data));
    }
}
