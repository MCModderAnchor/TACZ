package com.tacz.guns.entity.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireSelectEvent;
import com.tacz.guns.api.gun.FireMode;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;

public class GunFireSelect {
    private final LivingEntity shooter;
    private final ModDataHolder data;

    public GunFireSelect(LivingEntity shooter, ModDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void fireSelect() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (MinecraftForge.EVENT_BUS.post(new GunFireSelectEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
            return;
        }
        // 应用切换逻辑
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> {
            FireMode fireMode = iGun.getFireMode(currentGunItem);
            List<FireMode> fireModeSet = gunIndex.getGunData().getFireModeSet();
            // 即使玩家拿的是没有的 FireMode，这里也能切换到正常情况
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            iGun.setFireMode(currentGunItem, nextFireMode);
            return nextFireMode;
        });
    }
}
