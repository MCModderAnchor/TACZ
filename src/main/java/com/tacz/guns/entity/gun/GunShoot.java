package com.tacz.guns.entity.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.gun.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.entity.EntityBullet;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.Silence;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;

public class GunShoot {
    private final LivingEntity shooter;
    private final ModDataHolder data;
    private final GunDraw draw;
    private final GunAmmoCheck ammoCheck;

    public GunShoot(LivingEntity shooter, ModDataHolder data, GunDraw draw, GunAmmoCheck ammoCheck) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
        this.ammoCheck = ammoCheck;
    }

    public ShootResult shoot(float pitch, float yaw) {
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
        // 创造模式不判断子弹数
        if (ammoCheck.needCheckAmmo() && ammoCount < 1) {
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
        // 调用射击方法
        this.doShoot(pitch, yaw, iGun, gunIndex, currentGunItem, gunId, boltType);
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
        return gunIndex.map(index -> {
            long coolDown = index.getGunData().getShootInterval() - (System.currentTimeMillis() - data.shootTimestamp);
            // 给 25 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 25;
            return Math.max(coolDown, 0L);
        }).orElse(-1L);
    }

    private void doShoot(float pitch, float yaw, IGun iGun, CommonGunIndex gunIndex, ItemStack currentGunItem, ResourceLocation gunId, Bolt boltType) {
        // 生成子弹实体
        this.spawnBullet(pitch, yaw, gunIndex, currentGunItem, gunId);
        // 削减弹药数
        if (ammoCheck.consumesAmmoOrNot()) {
            this.reduceAmmo(iGun, currentGunItem, boltType);
        }
        data.shootTimestamp = System.currentTimeMillis();
        data.shootCount += 1;
    }

    private void spawnBullet(float pitch, float yaw, CommonGunIndex gunIndex, ItemStack currentGunItem, ResourceLocation gunId) {
        // 散射影响
        InaccuracyType inaccuracyState = InaccuracyType.getInaccuracyType(shooter);
        final float[] inaccuracy = new float[]{gunIndex.getGunData().getInaccuracy(inaccuracyState)};

        // 消音器影响
        final int[] soundDistance = new int[]{GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get()};
        final boolean[] useSilenceSound = new boolean[]{false};

        // 配件属性的读取计算
        AttachmentDataUtils.getAllAttachmentData(currentGunItem, gunIndex.getGunData(), attachmentData ->
                calculateAttachmentData(attachmentData, inaccuracyState, inaccuracy, soundDistance, useSilenceSound));
        inaccuracy[0] = Math.max(0, inaccuracy[0]);

        // 其他数据的读取，预先校验一次
        BulletData bulletData = gunIndex.getBulletData();
        float speed = Mth.clamp(bulletData.getSpeed() / 20, 0, Float.MAX_VALUE);
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);
        boolean isTracerAmmo = bulletData.hasTracerAmmo() && (data.shootCount % (bulletData.getTracerCountInterval() + 1) == 0);
        ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();

        // 开始生成子弹
        Level world = shooter.getLevel();
        for (int i = 0; i < bulletAmount; i++) {
            EntityBullet bullet = new EntityBullet(world, shooter, ammoId, bulletData, isTracerAmmo, gunId);
            bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, speed, inaccuracy[0]);
            world.addFreshEntity(bullet);
        }

        // 播放枪声
        if (soundDistance[0] > 0) {
            String soundId = useSilenceSound[0] ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
            SoundManager.sendSoundToNearby(shooter, soundDistance[0], gunId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
        }
    }

    private void calculateAttachmentData(AttachmentData attachmentData, InaccuracyType inaccuracyState, float[] inaccuracy, int[] soundDistance, boolean[] useSilenceSound) {
        // 影响除瞄准外所有的不准确度
        if (!inaccuracyState.isAim()) {
            inaccuracy[0] += attachmentData.getInaccuracyAddend();
        }
        Silence silence = attachmentData.getSilence();
        if (silence != null) {
            soundDistance[0] += silence.getDistanceAddend();
            if (silence.isUseSilenceSound()) {
                useSilenceSound[0] = true;
            }
        }
    }

    private void reduceAmmo(IGun iGun, ItemStack currentGunItem, Bolt boltType) {
        if (boltType == Bolt.MANUAL_ACTION) {
            iGun.setBulletInBarrel(currentGunItem, false);
        } else if (boltType == Bolt.CLOSED_BOLT) {
            if (iGun.getCurrentAmmoCount(currentGunItem) > 0) {
                iGun.reduceCurrentAmmoCount(currentGunItem);
            } else {
                iGun.setBulletInBarrel(currentGunItem, false);
            }
        } else {
            iGun.reduceCurrentAmmoCount(currentGunItem);
        }
    }
}
