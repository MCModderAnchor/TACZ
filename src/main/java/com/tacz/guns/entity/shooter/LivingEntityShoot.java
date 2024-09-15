package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunShoot;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;
import java.util.function.Supplier;

public class LivingEntityShoot {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;

    public LivingEntityShoot(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
    }

    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw) {
        if (data.currentGunItem == null) {
            return ShootResult.NOT_DRAW;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return ShootResult.NOT_GUN;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return ShootResult.ID_NOT_EXIST;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        // 判断射击是否正在冷却
        long coolDown = getShootCoolDown();
        if (coolDown == -1) {
            // 一般来说不太可能为 -1，原因未知
            return ShootResult.UNKNOWN_FAIL;
        }
        if (coolDown > 0) {
            return ShootResult.COOL_DOWN;
        }
        // 检查是否正在换弹
        if (data.reloadStateType.isReloading()) {
            return ShootResult.IS_RELOADING;
        }
        // 检查是否在切枪
        if (draw.getDrawCoolDown() != 0) {
            return ShootResult.IS_DRAWING;
        }
        // 检查是否在拉栓
        if (data.boltCoolDown >= 0) {
            return ShootResult.IS_BOLTING;
        }
        // 检查是否在奔跑
        if (data.sprintTimeS > 0) {
            return ShootResult.IS_SPRINTING;
        }
        Bolt boltType = gunIndex.getGunData().getBolt();
        boolean hasAmmoInBarrel = iGun.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT;
        int ammoCount = iGun.getCurrentAmmoCount(currentGunItem) + (hasAmmoInBarrel ? 1 : 0);
        // 创造模式也要判断子弹数
        if (ammoCount < 1) {
            return ShootResult.NO_AMMO;
        }
        // 检查膛内子弹
        if (boltType == Bolt.MANUAL_ACTION && !hasAmmoInBarrel) {
            return ShootResult.NEED_BOLT;
        }
        if (boltType == Bolt.CLOSED_BOLT && !hasAmmoInBarrel) {
            iGun.reduceCurrentAmmoCount(currentGunItem);
            iGun.setBulletInBarrel(currentGunItem, true);
        }
        // 触发射击事件
        if (MinecraftForge.EVENT_BUS.post(new GunShootEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
            return ShootResult.FORGE_EVENT_CANCEL;
        }
        NetworkHandler.sendToTrackingEntity(new ServerMessageGunShoot(shooter.getId(), currentGunItem), shooter);
        // 执行枪械射击逻辑
        if (iGun instanceof AbstractGunItem logicGun) {
            BulletData bulletData = gunIndex.getBulletData();
            boolean isTracerAmmo = bulletData.hasTracerAmmo() && (data.shootCount % (bulletData.getTracerCountInterval() + 1) == 0);
            logicGun.shoot(currentGunItem, pitch, yaw, isTracerAmmo, shooter);
        }
        data.shootTimestamp = System.currentTimeMillis();
        data.shootCount += 1;
        return ShootResult.SUCCESS;
    }

    public long getShootCoolDown() {
        if (data.currentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        FireMode fireMode = iGun.getFireMode(currentGunItem);
        if (fireMode == FireMode.BURST) {
            return gunIndex.map(index -> {
                long coolDown = (long) (index.getGunData().getBurstData().getMinInterval() * 1000f) - (System.currentTimeMillis() - data.shootTimestamp);
                // 给 5 ms 的窗口时间，以平衡延迟
                coolDown = coolDown - 5;
                return Math.max(coolDown, 0L);
            }).orElse(-1L);
        }
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval(this.shooter, fireMode) - (System.currentTimeMillis() - data.shootTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            return Math.max(coolDown, 0L);
        }).orElse(-1L);
    }
}
