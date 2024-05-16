package com.tacz.guns.client.model.papi;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class AmmoCountPapi implements Function<ItemStack, String> {
    public static final String NAME = "ammo_count";

    @Override
    public String apply(ItemStack stack) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun != null) {
            ResourceLocation gunId = iGun.getGunId(stack);
            ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(gunId).orElse(null);
            if (gunIndex == null) {
                return "";
            }
            int ammoCount = iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
            return "" + ammoCount;
        }
        return "";
    }
}
