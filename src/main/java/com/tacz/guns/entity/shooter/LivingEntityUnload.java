package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class LivingEntityUnload {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;
    private final LivingEntityMelee melee;

    public LivingEntityUnload(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw,
                              LivingEntityShoot shoot, LivingEntityMelee melee) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
        this.melee = melee;
    }

    public void unloadAmmo() {
        if (shooter.level().isClientSide() || !shooter.isAlive() || shooter.isSpectator()
                || data.currentGunItem == null || data.reloadStateType.isReloading() || data.isBolting
                || data.meleePrepTickCount >= 0 || draw.getDrawCoolDown() != 0
                || shoot.getShootCoolDown() != 0 || melee.getMeleeCoolDown() != 0
                || data.isAiming || data.aimingProgress > 0 || data.sprintTimeS > 0 || shooter.isSprinting()) {
            return;
        }
        ItemStack stack = shooter.getMainHandItem();
        if (stack != data.currentGunItem.get() || !(stack.getItem() instanceof AbstractGunItem gun)
                || gun.useInventoryAmmo(stack)) {
            return;
        }
        gun.unloadAmmo(data, stack, shooter);
    }
}
