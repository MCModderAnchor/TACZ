package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LivingEntityBolt {
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;
    private final LivingEntity shooter;

    public LivingEntityBolt(ShooterDataHolder data, LivingEntity shooter, LivingEntityDrawGun draw, LivingEntityShoot shoot) {
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
        this.shooter = shooter;
    }

    public void bolt() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
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
            if (data.isBolting) {
                return;
            }
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
            // 检查 bolt 类型是否是 manual action
            Bolt boltType = gunIndex.getGunData().getBolt();
            // 是否为背包直读
            boolean useInventoryAmmo = iGun.useInventoryAmmo(currentGunItem);
            // 膛内是否有子弹
            boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
            // 背包内是否还有子弹 (创造模式是否消耗背包备弹)
            boolean hasInventoryAmmo = iGun.hasInventoryAmmo(shooter, currentGunItem, gunOperator.needCheckAmmo());
            // 判断没有子弹的条件 (背包直读且包内没子弹 / 非背包直读且弹匣子弹数 < 1)
            boolean noAmmo = useInventoryAmmo && !hasInventoryAmmo ||
                    !useInventoryAmmo && iGun.getCurrentAmmoCount(currentGunItem) < 1;
            if (boltType != Bolt.MANUAL_ACTION) {
                return;
            }
            // 检查是否有弹药在枪膛内
            if (hasAmmoInBarrel) {
                return;
            }
            // 检查弹匣内是否有子弹
            if (noAmmo) {
                return;
            }
            data.boltTimestamp = GunTime.nowMillis();
            data.isBolting = iGun.startBolt(data, currentGunItem, shooter);
            if (data.isBolting && gunIndex.getGunData().isForceAimExitOnBolt()) {
                gunOperator.aim(false);
            }
        });
    }

    public void tickBolt() {
        // bolt cool down 为 -1 时，代表拉栓逻辑进程没有开始，不需要tick
        if (!data.isBolting) {
            return;
        }
        if (data.currentGunItem == null) {
            data.isBolting = false;
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
            data.isBolting = false;
            return;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        data.isBolting = gunIndex.map(index -> iGun.tickBolt(data, currentGunItem, shooter)).orElse(false);
    }
}
