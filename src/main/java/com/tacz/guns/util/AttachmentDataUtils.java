package com.tacz.guns.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.modifier.custom.*;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/**
 * 配件数据工具类，用于离线计算物品属性<br>
 * 不应该频繁调用，应尽可能调用实体缓存<br>
 * 参见 {@link AttachmentCacheProperty}
 */
public final class AttachmentDataUtils {
    public static void getAllAttachmentData(ItemStack gunItem, GunData gunData, Consumer<AttachmentData> dataConsumer) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, type);
            if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
                continue;
            }
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
            if (attachmentData != null) {
                dataConsumer.accept(attachmentData);
            } else {
                TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> dataConsumer.accept(index.getData()));
            }
        }
    }

    public static int getMagExtendLevel(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 0;
        }
        ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, AttachmentType.EXTENDED_MAG);
        if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return 0;
        }
        AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
        if (attachmentData != null) {
            int level = attachmentData.getExtendedMagLevel();
            if (level <= 0) {
                return 0;
            } else return Math.min(level, 3);
        } else {
            return TimelessAPI.getCommonAttachmentIndex(attachmentId).map(index -> {
                int level = index.getData().getExtendedMagLevel();
                if (level <= 0) {
                    return 0;
                } else return Math.min(level, 3);
            }).orElse(0);
        }
    }

    public static int getAmmoCountWithAttachment(ItemStack gunItem, GunData gunData) {
        int[] extendedMagAmmoAmount = gunData.getExtendedMagAmmoAmount();
        if (extendedMagAmmoAmount == null) {
            return gunData.getAmmoAmount();
        }
        int level = getMagExtendLevel(gunItem, gunData);
        if (level == 0) {
            return gunData.getAmmoAmount();
        }
        return extendedMagAmmoAmount[level - 1];
    }

    public static double getWightWithAttachment(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return gunData.getWeight();
        }

        List<Modifier> modifiers = new ArrayList<>();
        for (AttachmentType type : AttachmentType.values()){
            ResourceLocation id = iGun.getAttachmentId(gunItem, type);
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(id);
            if (attachmentData != null) {
                var m = attachmentData.getModifier().get(WeightModifier.ID);
                if(m != null && m.getValue() instanceof Modifier modifier) {
                    modifiers.add(modifier);
                } else {
                    Modifier modifier = new Modifier();
                    modifier.setAddend(attachmentData.getWeight());
                    modifiers.add(modifier);
                }
            } else {
                TimelessAPI.getCommonAttachmentIndex(id).ifPresent(index -> {
                    var m = index.getData().getModifier().get(WeightModifier.ID);
                    if(m != null && m.getValue() instanceof Modifier modifier) {
                        modifiers.add(modifier);
                    } else {
                        Modifier modifier = new Modifier();
                        modifier.setAddend(index.getData().getWeight());
                        modifiers.add(modifier);
                    }
                });
            }
        }
        return AttachmentPropertyManager.eval(modifiers, gunData.getWeight());
    }

    public static boolean isExplodeEnabled(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            if (gunData.getBulletData().getExplosionData() != null) {
                return gunData.getBulletData().getExplosionData().isExplode();
            } else {
                return false;
            }
        }
        return calcBooleanValue(gunItem, gunData, ExplosionModifier.ID, ExplosionModifier.ExplosionModifierValue.class,
                ExplosionModifier.ExplosionModifierValue::isExplode);
    }

    public static double getArmorIgnoreWithAttachment(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 0;
        }
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        // 最终的 base
        float finalBase = extraDamage != null ? extraDamage.getArmorIgnore() : 0f;
        finalBase = fireModeAdjustData != null ? finalBase + fireModeAdjustData.getArmorIgnore() : finalBase;
        finalBase *= SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get();

        List<Modifier> modifiers = getModifiers(gunItem, gunData, ArmorIgnoreModifier.ID);
        return AttachmentPropertyManager.eval(modifiers, finalBase);
    }

    public static double getHeadshotMultiplier(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 0;
        }
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        // 最终的 base
        float finalBase = extraDamage != null ? extraDamage.getHeadShotMultiplier() : 0f;
        finalBase = fireModeAdjustData != null ? finalBase + fireModeAdjustData.getHeadShotMultiplier() : finalBase;
        finalBase *= SyncConfig.HEAD_SHOT_BASE_MULTIPLIER.get();

        List<Modifier> modifiers = getModifiers(gunItem, gunData, HeadShotModifier.ID);
        return AttachmentPropertyManager.eval(modifiers, finalBase);
    }

    public static double getDamageWithAttachment(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 0;
        }
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        float rawDamage = bulletData.getDamageAmount();
        // 开火模式调整
        // 最终的 base 伤害
        float finalBase = fireModeAdjustData != null ? fireModeAdjustData.getDamageAmount() : 0f;
        if (extraDamage != null && extraDamage.getDamageAdjust() != null) {
            finalBase += extraDamage.getDamageAdjust().get(0).getDamage();
        } else {
            finalBase += rawDamage;
        }
        finalBase *= SyncConfig.DAMAGE_BASE_MULTIPLIER.get();

        List<Modifier> modifiers = getModifiers(gunItem, gunData, DamageModifier.ID);
        return AttachmentPropertyManager.eval(modifiers, finalBase);
    }

    /**
     * 以指定id获取枪械物品的modifier列表
     * @param gunItem
     * @param gunData
     * @param id
     * @return
     */
    private static List<Modifier> getModifiers(ItemStack gunItem, GunData gunData, String id) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return new ArrayList<>();
        }
        List<Modifier> modifiers = new ArrayList<>();
        for (AttachmentType type : AttachmentType.values()) {
            ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, type);
            if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
                continue;
            }
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
            if (attachmentData != null) {
                var m = attachmentData.getModifier().get(id);
                if(m != null && m.getValue() instanceof Modifier modifier) {
                    modifiers.add(modifier);
                }
            } else {
                CommonAttachmentIndex index = TimelessAPI.getCommonAttachmentIndex(attachmentId).orElse(null);
                if (index != null) {
                    var m = index.getData().getModifier().get(id);
                    if(m != null && m.getValue() instanceof Modifier modifier) {
                        modifiers.add(modifier);
                    }
                }
            }
        }
        return modifiers;
    }

    /**
     * 计算布尔值，取或
     * @param gunItem 枪械物品
     * @param gunData 枪械原始数据
     * @param id modifier id
     * @param clazz data数据结构类
     * @param resolver 获取布尔值的方法
     * @param <T> data数据结构泛型
     * @return 计算结果
     */
    private static <T> boolean calcBooleanValue(ItemStack gunItem, GunData gunData, String id, Class<T> clazz, BooleanResolver<T> resolver) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return false;
        }
        for (AttachmentType type : AttachmentType.values()) {
            ResourceLocation attachmentId = iGun.getAttachmentId(gunItem, type);
            if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
                continue;
            }
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
            if (attachmentData != null) {
                var m = attachmentData.getModifier().get(id);
                boolean value = resolve(m, resolver, clazz);
                if (value) {
                    return true;
                }
            } else {
                CommonAttachmentIndex index = TimelessAPI.getCommonAttachmentIndex(attachmentId).orElse(null);
                if (index != null) {
                    var m = index.getData().getModifier().get(id);
                    boolean value = resolve(m, resolver, clazz);
                    if (value) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static <T> boolean resolve(JsonProperty<?> raw, BooleanResolver<T> data, Class<T> type){
        if (raw != null && raw.getValue() != null && raw.getValue().getClass().equals(type)) {
            return data.apply((T) raw.getValue());
        }
        return false;
    }

    @FunctionalInterface
    public interface BooleanResolver<T> {
        boolean apply(T data);
    }
}
