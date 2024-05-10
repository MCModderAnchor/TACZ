package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LivingEntityBolt {
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;

    public LivingEntityBolt(ShooterDataHolder data, LivingEntityDrawGun draw, LivingEntityShoot shoot) {
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
    }

    public void bolt() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 判断是否正在射击冷却
            if (shoot.getShootCoolDown() != 0) {
                return;
            }
            // 检查是否正在换弹
            if (data.reloadStateType.isReloading()) {
                return;
            }
            // 检查是否在切枪
            if (draw.getDrawCoolDown() != 0) {
                return;
            }
            // 检查是否在拉栓
            if (data.boltCoolDown >= 0) {
                return;
            }
            // 检查 bolt 类型是否是 manual action
            Bolt boltType = gunIndex.getGunData().getBolt();
            if (boltType != Bolt.MANUAL_ACTION) {
                return;
            }
            // 检查是否有弹药在枪膛内
            if (iGun.hasBulletInBarrel(currentGunItem)) {
                return;
            }
            // 检查弹匣内是否有子弹
            if (iGun.getCurrentAmmoCount(currentGunItem) == 0) {
                return;
            }
            data.boltTimestamp = System.currentTimeMillis();
            // 将bolt cool down随便改为一个非 -1 的数值，以标记bolt进程开始
            data.boltCoolDown = 0;
        });
    }

    public void tickBolt() {
        // bolt cool down 为 -1 时，代表拉栓逻辑进程没有开始，不需要tick
        if (data.boltCoolDown == -1) {
            return;
        }
        if (data.currentGunItem == null) {
            data.boltCoolDown = -1;
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            data.boltCoolDown = -1;
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        data.boltCoolDown = gunIndex.map(index -> {
            long coolDown = (long) (index.getGunData().getBoltActionTime() * 1000) - (System.currentTimeMillis() - data.boltTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
        if (data.boltCoolDown == 0) {
            if (iGun instanceof AbstractGunItem logicGun) {
                logicGun.bolt(currentGunItem);
            }
            data.boltCoolDown = -1;
        }
    }
}
