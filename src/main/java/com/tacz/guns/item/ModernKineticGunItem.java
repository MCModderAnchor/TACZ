package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.Silence;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.CycleTaskHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 现代枪的逻辑实现
 */
public class ModernKineticGunItem extends AbstractGunItem implements GunItemDataAccessor {
    public static final String TYPE_NAME = "modern_kinetic";

    public ModernKineticGunItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void bolt(ItemStack gunItem) {
        if (this.getCurrentAmmoCount(gunItem) > 0) {
            this.reduceCurrentAmmoCount(gunItem);
            this.setBulletInBarrel(gunItem, true);
        }
    }

    @Override
    public void shoot(ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, boolean tracer, LivingEntity shooter) {
        ResourceLocation gunId = getGunId(gunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        // 散射影响
        InaccuracyType inaccuracyState = InaccuracyType.getInaccuracyType(shooter);
        final float[] inaccuracy = new float[]{gunIndex.getGunData().getInaccuracy(inaccuracyState)};

        // 消音器影响
        final int[] soundDistance = new int[]{GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get()};
        final boolean[] useSilenceSound = new boolean[]{false};

        // 配件属性的读取计算
        AttachmentDataUtils.getAllAttachmentData(gunItem, gunIndex.getGunData(), attachmentData ->
                calculateAttachmentData(attachmentData, inaccuracyState, inaccuracy, soundDistance, useSilenceSound));
        inaccuracy[0] = Math.max(0, inaccuracy[0]);

        BulletData bulletData = gunIndex.getBulletData();
        ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();
        FireMode fireMode = getFireMode(gunItem);
        // 子弹飞行速度
        float speed = Mth.clamp(bulletData.getSpeed() / 20, 0, Float.MAX_VALUE);
        // 弹丸数量
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);
        // 连发数量
        int cycles = fireMode == FireMode.BURST ? gunIndex.getGunData().getBurstData().getCount() : 1;
        // 连发间隔
        long period = fireMode == FireMode.BURST ? gunIndex.getGunData().getBurstShootInterval() : 1;
        // 是否消耗弹药
        boolean consumeAmmo = IGunOperator.fromLivingEntity(shooter).consumesAmmoOrNot();

        // 将连发任务委托到循环任务工具
        CycleTaskHelper.addCycleTask(() -> {
            // 削减弹药数
            if (consumeAmmo) {
                Bolt boltType = gunIndex.getGunData().getBolt();
                boolean hasAmmoInBarrel = this.hasBulletInBarrel(gunItem) && boltType != Bolt.OPEN_BOLT;
                int ammoCount = this.getCurrentAmmoCount(gunItem) + (hasAmmoInBarrel ? 1 : 0);
                if (ammoCount <= 0) {
                    return false;
                }
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(shooter, gunItem, LogicalSide.SERVER));
            if (fire) {
                if (consumeAmmo) {
                    // 削减弹药
                    this.reduceAmmo(gunItem);
                }
                // 生成子弹
                Level world = shooter.level();
                for (int i = 0; i < bulletAmount; i++) {
                    this.doSpawnBulletEntity(world, shooter, pitch.get(), yaw.get(), speed, inaccuracy[0], ammoId, gunId, tracer, bulletData);
                }
                // 播放枪声
                if (soundDistance[0] > 0) {
                    String soundId = useSilenceSound[0] ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(shooter, soundDistance[0], gunId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
    }

    @Override
    public void fireSelect(ItemStack gunItem) {
        ResourceLocation gunId = this.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> {
            FireMode fireMode = this.getFireMode(gunItem);
            List<FireMode> fireModeSet = gunIndex.getGunData().getFireModeSet();
            // 即使玩家拿的是没有的 FireMode，这里也能切换到正常情况
            int nextIndex = (fireModeSet.indexOf(fireMode) + 1) % fireModeSet.size();
            FireMode nextFireMode = fireModeSet.get(nextIndex);
            this.setFireMode(gunItem, nextFireMode);
            return nextFireMode;
        });
    }

    @Override
    public void reloadAmmo(ItemStack gunItem, int ammoCount, boolean loadBarrel) {
        ResourceLocation gunId = getGunId(gunItem);
        Bolt boltType = TimelessAPI.getCommonGunIndex(gunId).map(index -> index.getGunData().getBolt()).orElse(null);
        this.setCurrentAmmoCount(gunItem, ammoCount);
        if (loadBarrel && (boltType == Bolt.MANUAL_ACTION || boltType == Bolt.CLOSED_BOLT)) {
            this.reduceCurrentAmmoCount(gunItem);
            this.setBulletInBarrel(gunItem, true);
        }
    }

    /**
     * 生成子弹实体
     */
    protected void doSpawnBulletEntity(Level world, LivingEntity shooter, float pitch, float yaw, float speed, float inaccuracy,
                                       ResourceLocation ammoId, ResourceLocation gunId, boolean tracer, BulletData bulletData) {
        EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, ammoId, gunId, tracer, bulletData);
        bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, speed, inaccuracy);
        world.addFreshEntity(bullet);
    }

    @Override
    public int getLevel(int exp) {
        return 0;
    }

    @Override
    public int getExp(int level) {
        return 0;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    /**
     * 将枪内的弹药数减少。
     *
     * @param currentGunItem 枪械物品
     */
    protected void reduceAmmo(ItemStack currentGunItem) {
        Bolt boltType = TimelessAPI.getCommonGunIndex(getGunId(currentGunItem)).map(index -> index.getGunData().getBolt()).orElse(null);
        if (boltType == null) {
            return;
        }
        if (boltType == Bolt.MANUAL_ACTION) {
            this.setBulletInBarrel(currentGunItem, false);
        } else if (boltType == Bolt.CLOSED_BOLT) {
            if (this.getCurrentAmmoCount(currentGunItem) > 0) {
                this.reduceCurrentAmmoCount(currentGunItem);
            } else {
                this.setBulletInBarrel(currentGunItem, false);
            }
        } else {
            this.reduceCurrentAmmoCount(currentGunItem);
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
}
