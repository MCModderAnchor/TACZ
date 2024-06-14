package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LivingEntityMelee {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntityMelee(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void melee() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        if (iGun instanceof AbstractGunItem logicGun) {
            logicGun.melee(this.shooter, currentGunItem);
        }
    }
}
