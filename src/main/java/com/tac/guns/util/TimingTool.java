package com.tac.guns.util;

import com.tac.guns.api.entity.IShooter;
import com.tac.guns.resource.pojo.data.GunData;

public final class TimingTool {
    public static boolean isShootCooldown(IShooter shooter, GunData gunData) {
        return (System.currentTimeMillis() - shooter.getShootTime()) < gunData.getShootInterval();
    }

    public static boolean isReloadCooldown(IShooter shooter, GunData gunData) {
        // FIXME 应该依据类型进行不同冷却时间判断
        float emptyMagFedTime = gunData.getReloadData().getEmptyMagFedTime();
        return (System.currentTimeMillis() - shooter.getReloadTime()) < (emptyMagFedTime * 1000);
    }

    public static boolean isDrawCooldown(IShooter shooter, GunData gunData) {
        return (System.currentTimeMillis() - shooter.getDrawTime()) < (gunData.getDrawTime() * 1000);
    }
}
