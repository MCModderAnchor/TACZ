package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunReload;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

public class LivingEntityReload {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;
    private final LivingEntityShoot shoot;

    public LivingEntityReload(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw, LivingEntityShoot shoot) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
        this.shoot = shoot;
    }

    public void reload() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        ResourceLocation gunId = gunItem.getGunId(currentGunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            // 检查换弹是否还未完成
            if (data.reloadStateType.isReloading()) {
                return;
            }
            // 检查是否正在开火冷却
            if (shoot.getShootCoolDown() != 0) {
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
            // 检查弹药
            if (IGunOperator.fromLivingEntity(shooter).needCheckAmmo() && !gunItem.canReload(shooter, currentGunItem)) {
                return;
            }
            // 触发装弹事件
            if (MinecraftForge.EVENT_BUS.post(new GunReloadEvent(shooter, currentGunItem, LogicalSide.SERVER))) {
                return;
            }
            NetworkHandler.sendToTrackingEntity(new ServerMessageGunReload(shooter.getId(), currentGunItem), shooter);
            Bolt boltType = gunIndex.getGunData().getBolt();
            int ammoCount = gunItem.getCurrentAmmoCount(currentGunItem) + (gunItem.hasBulletInBarrel(currentGunItem) && boltType != Bolt.OPEN_BOLT ? 1 : 0);
            if (ammoCount <= 0) {
                // 初始化空仓换弹的 tick 的状态
                data.reloadStateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
            } else {
                // 初始化战术换弹的 tick 的状态
                data.reloadStateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
            }
            data.reloadTimestamp = System.currentTimeMillis();
            // 调用枪械逻辑
            if (!gunItem.startReload(data, currentGunItem, shooter)) {
                data.reloadStateType = ReloadState.StateType.NOT_RELOADING;
                data.reloadTimestamp = -1;
            }
        });
    }

    public void cancelReload() {
        if (data.currentGunItem == null) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof AbstractGunItem gunItem)) {
            return;
        }
        // 检查是否在换弹
        if (!data.reloadStateType.isReloading()) {
            return;
        }
        gunItem.interruptReload(data, currentGunItem, shooter);
    }

    public ReloadState tickReloadState() {
        ReloadState result = new ReloadState();
        // 如果没有在换弹，直接返回
        if (data.reloadTimestamp == -1) {
            return result;
        }
        // 调用枪械逻辑
        if (data.currentGunItem != null) {
            ItemStack currentGunItem = data.currentGunItem.get();
            if (currentGunItem != null && currentGunItem.getItem() instanceof AbstractGunItem abstractGunItem) {
                 result = abstractGunItem.tickReload(data, currentGunItem, shooter);
            }
        }
        // 将 tick 的结果保存到 data holder
        data.reloadStateType = result.getStateType();
        if (!result.getStateType().isReloading()) {
            data.reloadTimestamp = -1;
        }
        return result;
    }
}
