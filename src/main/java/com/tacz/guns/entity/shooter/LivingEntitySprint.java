package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.config.sync.SyncConfig;
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
        boolean reloadBlocksSprint = !SyncConfig.ALLOW_RELOAD_WHILE_SPRINTING.get()
                && reloadStateType.isReloading() && !reloadStateType.isReloadFinishing();
        if (isAiming || reloadBlocksSprint) {
            return false;
        } else {
            return sprint;
        }
    }
}
