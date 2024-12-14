package com.tacz.guns.item;

import com.google.common.base.Suppliers;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.command.sub.DebugCommand;
import com.tacz.guns.debug.GunMeleeDebug;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.EffectData;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import com.tacz.guns.resource.pojo.data.gun.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * 现代枪的逻辑实现
 */
public class ModernKineticGunItem extends AbstractGunItem implements GunItemDataAccessor {
    public static final String TYPE_NAME = "modern_kinetic";

    private static final DoubleFunction<AttributeModifier> AM_FACTORY = amount -> new AttributeModifier(
            UUID.randomUUID(), "TACZ Melee Damage",
            amount, AttributeModifier.Operation.ADDITION
    );

    public ModernKineticGunItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean startBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return false;
        }
        return Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("start_bolt")))
                .map(func -> func.call(CoerceJavaToLua.coerce(api)).checkboolean())
                .orElse(true);
    }

    @Override
    public boolean tickBolt(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return false;
        }
        return Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("tick_bolt")))
                .map(func -> func.call(CoerceJavaToLua.coerce(api)).checkboolean())
                .orElseGet(() -> defaultTickBolt(api));
    }

    @Override
    public void shoot(ShooterDataHolder dataHolder, ItemStack gunItem, Supplier<Float> pitch, Supplier<Float> yaw, LivingEntity shooter) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);
        api.setPitchSupplier(pitch);
        api.setYawSupplier(yaw);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return;
        }
        Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("shoot")))
                .ifPresentOrElse(
                        func -> func.call(CoerceJavaToLua.coerce(api)),
                        ()   -> api.shootOnce(api.isShootingNeedConsumeAmmo()));
    }

    @Override
    public boolean startReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter){
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return false;
        }
        return Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("start_reload")))
                .map(func -> func.call(CoerceJavaToLua.coerce(api)).checkboolean())
                .orElse(true);
    }

    @Override
    public ReloadState tickReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return new ReloadState();
        }
        return Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("tick_reload")))
                .map(func -> {
                    ReloadState reloadState = new ReloadState();
                    Varargs varargs = func.invoke(CoerceJavaToLua.coerce(api));
                    int typeOrdinary = varargs.arg(1).checkint();
                    long countDown = varargs.arg(2).checklong();
                    reloadState.setStateType(ReloadState.StateType.values()[typeOrdinary]);
                    reloadState.setCountDown(countDown);
                    return reloadState;
                })
                .orElseGet(() -> defaultTickReload(api));
    }

    @Override
    public void interruptReload(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter) {
        ModernKineticGunScriptAPI api = new ModernKineticGunScriptAPI();
        api.setItemStack(gunItem);
        api.setShooter(shooter);
        api.setDataHolder(dataHolder);

        CommonGunIndex gunIndex = api.getGunIndex();
        if (gunIndex == null) {
            return;
        }
        Optional.ofNullable(gunIndex.getScript())
                .map(script -> checkFunction(script.get("interrupt_reload")))
                .ifPresent(func -> func.call(CoerceJavaToLua.coerce(api)));
    }


    @Override
    public void melee(ShooterDataHolder dataHolder, LivingEntity user, ItemStack gunItem) {
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


    private boolean defaultTickBolt(ModernKineticGunScriptAPI api) {
        GunData gunData = api.getGunIndex().getGunData();
        long boltActionTime = (long) (gunData.getBoltActionTime() * 1000);
        float rawBoltFeedTime = gunData.getBoltFeedTime();
        long boltFeedTime = rawBoltFeedTime == -1 ? boltActionTime : (long) (gunData.getBoltFeedTime() * 1000);
        if (api.getBoltTime() < boltFeedTime) {
            return true;
        } else {
            if (!api.hasAmmoInBarrel()) {
                if (api.removeAmmoFromMagazine(1) != 0) {
                    api.setAmmoInBarrel(true);
                }
            }
            return api.getBoltTime() < boltActionTime;
        }
    }

    private ReloadState defaultTickReload(ModernKineticGunScriptAPI api) {
        CommonGunIndex gunIndex = api.getGunIndex();
        // 获取 ReloadData
        GunData gunData = gunIndex.getGunData();
        GunReloadData reloadData = gunData.getReloadData();
        // 计算新的 stateType 和 countDown
        long countDown;
        ReloadState.StateType stateType;
        ReloadState.StateType oldStateType = ReloadState.StateType.values()[api.getReloadStateType()];
        long progressTime = api.getReloadTime();
        if (oldStateType.isReloadingEmpty()) {
            long feedTime = (long) (reloadData.getFeed().getEmptyTime() * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getEmptyTime() * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
            }
        } else if (oldStateType.isReloadingTactical()) {
            long feedTime = (long) (reloadData.getFeed().getTacticalTime() * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getTacticalTime() * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
            }
        } else {
            stateType = ReloadState.StateType.NOT_RELOADING;
            countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
        }
        // 如果换弹状态发生 装填 -> 收尾 的变化，则需要调用补弹
        if (oldStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING && oldStateType != stateType) {
            this.defaultReloadFinishing(api, false);
        }
        if (oldStateType == ReloadState.StateType.TACTICAL_RELOAD_FEEDING && oldStateType != stateType) {
            this.defaultReloadFinishing(api, true);
        }
        // 返回 tick 结果
        ReloadState reloadState = new ReloadState();
        reloadState.setStateType(stateType);
        reloadState.setCountDown(countDown);
        return reloadState;
    }

    private void defaultReloadFinishing(ModernKineticGunScriptAPI api, boolean isTactical) {
        GunData data = api.getGunIndex().getGunData();
        int needAmmoCount = api.getNeededAmmoAmount();
        boolean needConsumeAmmo = api.isReloadingNeedConsumeAmmo();
        switch (data.getReloadData().getType()) {
            case MAGAZINE -> {
                if (needConsumeAmmo) {
                    int consumedAmount = api.consumeAmmoFromPlayer(needAmmoCount);
                    api.putAmmoInMagazine(consumedAmount);
                } else {
                    api.putAmmoInMagazine(needAmmoCount);
                }
            }
            case FUEL -> {
                if (needConsumeAmmo) {
                    int consumedAmount = api.consumeAmmoFromPlayer(1);
                    api.putAmmoInMagazine(needAmmoCount * consumedAmount);
                } else {
                    api.putAmmoInMagazine(needAmmoCount);
                }
            }
            default -> {
                // 未实现
            }
        }
        // 如果不是战术换弹，需要将弹匣中的一枚子弹放到枪膛中
        Bolt boltType = api.getGunIndex().getGunData().getBolt();
        if (!isTactical && (boltType == Bolt.MANUAL_ACTION || boltType == Bolt.CLOSED_BOLT)) {
            int i = api.removeAmmoFromMagazine(1);
            if (i != 0) {
                api.setAmmoInBarrel(true);
            }
        }
    }

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
        List<LivingEntity> entityList = user.level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(distance));
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
            target.hurt(DamageSource.playerAttack(player), damage);
        } else {
            target.hurt(DamageSource.mobAttack(user), damage);
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
        if (user.level instanceof ServerLevel serverLevel) {
            int count = (int) (damage * 0.5);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), count, 0.1, 0, 0.1, 0.2);
        }
    }

    @Nullable
    private MeleeData getMeleeData(ResourceLocation attachmentId) {
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return null;
        }
        return TimelessAPI.getCommonAttachmentIndex(attachmentId).map(index -> index.getData().getMeleeData()).orElse(null);
    }

    private LuaFunction checkFunction(LuaValue luaValue) {
        if (luaValue.isfunction()) {
            return (LuaFunction) luaValue;
        } else if (luaValue.isnil()) {
            return null;
        } else {
            throw new LuaError("bad argument: function or nil expected, got " + luaValue.typename());
        }
    }

    @Override
    public void fireSelect(ShooterDataHolder dataHolder, ItemStack gunItem) {
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
}
