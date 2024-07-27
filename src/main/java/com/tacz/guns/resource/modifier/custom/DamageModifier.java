package com.tacz.guns.resource.modifier.custom;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DamageModifier implements IAttachmentModifier<ModifiedValue, LinkedList<DistanceDamagePair>> {
    public static final String ID = "damage";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<ModifiedValue> readJson(String json) {
        DamageModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, DamageModifier.Data.class);
        return new DamageJsonProperty(data.getDamage());
    }

    @Override
    public CacheValue<LinkedList<DistanceDamagePair>> initCache(ItemStack gunItem, GunData gunData) {
        // 必要数据获取
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

        // 获取最原始的数值
        float rawDamage = bulletData.getDamageAmount();
        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        float fireAdjustDamageAmount = fireModeAdjustData != null ? fireModeAdjustData.getDamageAmount() : 0;

        // 开始存入我们的数据
        LinkedList<DistanceDamagePair> cacheValue = Lists.newLinkedList();
        if (extraDamage != null) {
            for (DistanceDamagePair pair : extraDamage.getDamageAdjust()) {
                float finalBaseDamage = pair.getDamage() + fireAdjustDamageAmount;
                cacheValue.add(new DistanceDamagePair(pair.getDistance(), finalBaseDamage));
            }
        } else {
            cacheValue.add(new DistanceDamagePair(Integer.MAX_VALUE, rawDamage + fireAdjustDamageAmount));
        }
        return new CacheValue<>(cacheValue);
    }

    @Override
    public void eval(List<ModifiedValue> modifiedValues, CacheValue<LinkedList<DistanceDamagePair>> cache) {
        LinkedList<DistanceDamagePair> cacheValue = cache.getValue();
        LinkedList<DistanceDamagePair> modifiedValue = new LinkedList<>();
        for (DistanceDamagePair pair : cacheValue) {
            float base = pair.getDamage();
            float eval = (float) AttachmentPropertyManager.eval(modifiedValues, base);
            modifiedValue.add(new DistanceDamagePair(pair.getDistance(), eval));
        }
        cache.setValue(modifiedValue);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        // 必要数据获取
        LinkedList<DistanceDamagePair> damagePairModifier = cacheProperty.getCache(DamageModifier.ID);
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

        // 获取最原始的数值
        float rawDamage = bulletData.getDamageAmount();
        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        float fireAdjustDamageAmount = fireModeAdjustData != null ? fireModeAdjustData.getDamageAmount() : 0;
        // 最终的 base 伤害
        float finalBaseDamage;
        if (extraDamage != null && !extraDamage.getDamageAdjust().isEmpty()) {
            finalBaseDamage = extraDamage.getDamageAdjust().get(0).getDamage() + fireAdjustDamageAmount;
        } else {
            finalBaseDamage = rawDamage + fireAdjustDamageAmount;
        }

        float damageModifier = damagePairModifier.get(0).getDamage() - finalBaseDamage;
        double damagePercent = Math.min(finalBaseDamage / 100.0, 1);
        double damageModifierPercent = Math.min(damageModifier / 100.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.damage";
        String positivelyString = String.format("%.2f §a(+%.2f)", finalBaseDamage, damageModifier);
        String negativelyString = String.format("%.2f §c(%.2f)", finalBaseDamage, damageModifier);
        String defaultString = String.format("%.2f", finalBaseDamage);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(damagePercent, damageModifierPercent, damageModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class DamageJsonProperty extends JsonProperty<ModifiedValue> {
        public DamageJsonProperty(ModifiedValue value) {
            super(value);
        }

        @Override
        public void initComponents() {
            ModifiedValue value = getValue();
            if (value != null) {
                double eval = AttachmentPropertyManager.eval(value, 9);
                int damage = (int) Math.round(eval);
                if (damage > 9) {
                    components.add(Component.translatable("tooltip.tacz.attachment.damage.increase").withStyle(ChatFormatting.GREEN));
                } else if (damage < 9) {
                    components.add(Component.translatable("tooltip.tacz.attachment.damage.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public static class Data {
        @SerializedName("damage")
        @Nullable
        private ModifiedValue damage = null;

        @Nullable
        public ModifiedValue getDamage() {
            return damage;
        }
    }
}
