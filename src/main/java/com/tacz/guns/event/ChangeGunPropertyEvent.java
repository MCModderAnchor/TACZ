package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.item.IGun;
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
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> event.getCacheProperty().eval(gunItem, gunIndex.getGunData()));
    }
}
