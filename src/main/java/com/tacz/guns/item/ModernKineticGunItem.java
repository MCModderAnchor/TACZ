package com.tacz.guns.item;

import com.google.common.base.Suppliers;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.command.sub.DebugCommand;
import com.tacz.guns.debug.GunMeleeDebug;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunFire;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AimInaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.attachment.EffectData;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import com.tacz.guns.resource.pojo.data.gun.*;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.util.CycleTaskHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.DoubleFunction;
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
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return;
        }
        CommonGunIndex gunIndex = gunIndexOptional.get();
        BulletData bulletData = gunIndex.getBulletData();
        GunData gunData = gunIndex.getGunData();
        ResourceLocation ammoId = gunData.getAmmoId();
        FireMode fireMode = iGun.getFireMode(gunItem);
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
        if (cacheProperty == null) {
            return;
        }

        // 散射影响
        InaccuracyType inaccuracyType = InaccuracyType.getInaccuracyType(shooter);
        float inaccuracy = Math.max(0, cacheProperty.<Map<InaccuracyType, Float>>getCache(InaccuracyModifier.ID).get(inaccuracyType));
        if (inaccuracyType == InaccuracyType.AIM) {
            inaccuracy = Math.max(0, cacheProperty.<Map<InaccuracyType, Float>>getCache(AimInaccuracyModifier.ID).get(inaccuracyType));
        }
        final float finalInaccuracy = inaccuracy;

        // 消音器影响
        Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
        final int soundDistance = silence.first();
        final boolean useSilenceSound = silence.right();

        // 子弹飞行速度
        float speed = cacheProperty.<Float>getCache(AmmoSpeedModifier.ID);
        float finalSpeed = Mth.clamp(speed / 20, 0, Float.MAX_VALUE);
        // 弹丸数量
        int bulletAmount = Math.max(bulletData.getBulletAmount(), 1);
        // 连发数量
        int cycles = fireMode == FireMode.BURST ? gunData.getBurstData().getCount() : 1;
        // 连发间隔
        long period = fireMode == FireMode.BURST ? gunData.getBurstShootInterval() : 1;
        // 是否消耗弹药
        boolean consumeAmmo = IGunOperator.fromLivingEntity(shooter).consumesAmmoOrNot();

        CycleTaskHelper.addCycleTask(() -> {
            // 如果射击者死亡，取消射击
            if (shooter.isDeadOrDying()) {
                return false;
            }
            // 削减弹药数
            if (consumeAmmo) {
                Bolt boltType = gunData.getBolt();
                boolean hasAmmoInBarrel = this.hasBulletInBarrel(gunItem) && boltType != Bolt.OPEN_BOLT;
                int ammoCount = this.getCurrentAmmoCount(gunItem) + (hasAmmoInBarrel ? 1 : 0);
                if (ammoCount <= 0) {
                    return false;
                }
            }
            // 触发击发事件
            boolean fire = !MinecraftForge.EVENT_BUS.post(new GunFireEvent(shooter, gunItem, LogicalSide.SERVER));
            if (fire) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageGunFire(shooter.getId(), gunItem), shooter);
                if (consumeAmmo) {
                    // 削减弹药
                    this.reduceAmmo(gunItem);
                }
                // 生成子弹
                Level world = shooter.level();
                for (int i = 0; i < bulletAmount; i++) {
                    this.doSpawnBulletEntity(world, shooter, gunItem, pitch.get(), yaw.get(), finalSpeed, finalInaccuracy, ammoId, gunId, tracer, gunData, bulletData);
                }
                // 播放枪声
                if (soundDistance > 0) {
                    String soundId = useSilenceSound ? SoundManager.SILENCE_3P_SOUND : SoundManager.SHOOT_3P_SOUND;
                    SoundManager.sendSoundToNearby(shooter, soundDistance, gunId, soundId, 0.8f, 0.9f + shooter.getRandom().nextFloat() * 0.125f);
                }
            }
            return true;
        }, period, cycles);
    }

    @Override
    public void melee(LivingEntity user, ItemStack gunItem) {
        ResourceLocation gunId = this.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            GunMeleeData meleeData = gunIndex.getGunData().getMeleeData();
            float distance = meleeData.getDistance();

            ResourceLocation muzzleId = this.getAttachmentId(gunItem, AttachmentType.MUZZLE);
            MeleeData muzzleData = getMeleeData(muzzleId);
            if (muzzleData != null) {
                doMelee(user, distance, muzzleData.getDistance(), muzzleData.getRangeAngle(), muzzleData.getKnockback(), muzzleData.getDamage(), muzzleData.getEffects());
                return;
            }

            ResourceLocation stockId = this.getAttachmentId(gunItem, AttachmentType.STOCK);
            MeleeData stockData = getMeleeData(stockId);
            if (stockData != null) {
                doMelee(user, distance, stockData.getDistance(), stockData.getRangeAngle(), stockData.getKnockback(), stockData.getDamage(), stockData.getEffects());
                return;
            }

            GunDefaultMeleeData defaultData = meleeData.getDefaultMeleeData();
            if (defaultData == null) {
                return;
            }
            doMelee(user, distance, defaultData.getDistance(), defaultData.getRangeAngle(), defaultData.getKnockback(), defaultData.getDamage(), Collections.emptyList());
        });
    }

    private static final DoubleFunction<AttributeModifier> AM_FACTORY = amount -> new AttributeModifier(
            UUID.randomUUID(), "TACZ Melee Damage",
            amount, AttributeModifier.Operation.ADDITION
    );

    private void doMelee(LivingEntity user, float gunDistance, float meleeDistance, float rangeAngle, float knockback, float damage, List<EffectData> effects) {
        // 枪长 + 刺刀长 = 总长
        double distance = gunDistance + meleeDistance;
        float xRot = (float) Math.toRadians(-user.getXRot());
        float yRot = (float) Math.toRadians(-user.getYRot());
        // 视角向量
        Vec3 eyeVec = new Vec3(0, 0, 1).xRot(xRot).yRot(yRot).normalize().scale(distance);
        // 球心坐标
        Vec3 centrePos = user.getEyePosition().subtract(eyeVec);
        // 先获取范围内所有的实体
        List<LivingEntity> entityList = user.level().getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(distance));
        Supplier<Float> realDamage = Suppliers.memoize(() -> {
            var instance = user.getAttribute(Attributes.ATTACK_DAMAGE);
            if (instance == null) {
                return damage;
            }
            var oldBase = instance.getBaseValue();
            var modifier = AM_FACTORY.apply(damage);
            try {
                instance.setBaseValue(0);
                instance.addTransientModifier(modifier);
                return (float)instance.getValue();
            } finally {
                instance.setBaseValue(oldBase);
                instance.removeModifier(modifier);
            }
        });
        // 而后检查是否在锥形范围内
        for (LivingEntity living : entityList) {
            // 先计算出球心->目标向量
            Vec3 targetVec = living.getEyePosition().subtract(centrePos);
            // 目标到球心距离
            double targetLength = targetVec.length();
            // 距离在一倍距离之内的，在玩家背后，不进行伤害
            if (targetLength < distance) {
                continue;
            }
            // 计算出向量夹角
            double degree = Math.toDegrees(Math.acos(targetVec.dot(eyeVec) / (targetLength * distance)));
            // 向量夹角在范围内的，才能进行伤害
            if (degree < (rangeAngle / 2)) {
                // 判断实体和玩家之间是否有阻隔
                if (user.hasLineOfSight(living)) {
                    doPerLivingHurt(user, living, knockback, realDamage.get(), effects);
                }
            }
        }

        // 玩家扣饱食度
        if (user instanceof Player player) {
            player.causeFoodExhaustion(0.1F);
        }

        // Debug 模式
        if (DebugCommand.DEBUG) {
            GunMeleeDebug.showRange(user, (int) Math.round(distance), centrePos, eyeVec, rangeAngle);
        }
    }

    private static void doPerLivingHurt(LivingEntity user, LivingEntity target, float knockback, float damage, List<EffectData> effects) {
        if (target.equals(user)) {
            return;
        }
        target.knockback(knockback, (float) Math.sin(Math.toRadians(user.getYRot())), (float) -Math.cos(Math.toRadians(user.getYRot())));
        if (user instanceof Player player) {
            target.hurt(user.damageSources().playerAttack(player), damage);
        } else {
            target.hurt(user.damageSources().mobAttack(user), damage);
        }
        // 修复近战枪械不触发神化词条/宝石的bug
        user.doEnchantDamageEffects(user, target);

        if (!target.isAlive()) {
            return;
        }
        for (EffectData data : effects) {
            MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(data.getEffectId());
            if (mobEffect == null) {
                continue;
            }
            int time = Math.max(0, data.getTime() * 20);
            int amplifier = Math.max(0, data.getAmplifier());
            MobEffectInstance effectInstance = new MobEffectInstance(mobEffect, time, amplifier, false, data.isHideParticles());
            target.addEffect(effectInstance);
        }
        if (user.level() instanceof ServerLevel serverLevel) {
            int count = (int) (damage * 0.5);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), count, 0.1, 0, 0.1, 0.2);
        }
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


    /**
     * 生成子弹实体
     */
    protected void doSpawnBulletEntity(Level world, LivingEntity shooter, ItemStack gunItem, float pitch, float yaw, float speed, float inaccuracy, ResourceLocation ammoId, ResourceLocation gunId, boolean tracer, GunData gunData, BulletData bulletData) {
        EntityKineticBullet bullet = new EntityKineticBullet(world, shooter, gunItem, ammoId, gunId, tracer, gunData, bulletData);
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

    @Nullable
    private MeleeData getMeleeData(ResourceLocation attachmentId) {
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return null;
        }
        return TimelessAPI.getCommonAttachmentIndex(attachmentId).map(index -> index.getData().getMeleeData()).orElse(null);
    }
}
