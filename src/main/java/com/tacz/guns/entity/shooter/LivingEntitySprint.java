package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import net.minecraft.world.entity.LivingEntity;

public class LivingEntitySprint {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntitySprint(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public boolean getProcessedSprintStatus(boolean sprint) {
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
        boolean isAiming = gunOperator.getSynIsAiming();
        ReloadState.StateType reloadStateType = gunOperator.getSynReloadState().getStateType();
        if (isAiming || (reloadStateType.isReloading() && !reloadStateType.isReloadFinishing())) {
            return false;
        } else {
            return sprint;
        }
    }
}
