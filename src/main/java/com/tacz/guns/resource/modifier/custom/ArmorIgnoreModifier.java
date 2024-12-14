package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ArmorIgnoreModifier implements IAttachmentModifier<Modifier, Float> {
    public static final String ID = "armor_ignore";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Modifier> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        return new ArmorIgnoreJsonProperty(data.getArmorIgnore());
    }

    @Override
    public CacheValue<Float> initCache(ItemStack gunItem, GunData gunData) {
        // 必要数据获取
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        // 最终的 base
        float finalBase = extraDamage != null ? extraDamage.getArmorIgnore() : 0;
        finalBase = fireModeAdjustData != null ? finalBase + fireModeAdjustData.getArmorIgnore() : finalBase;
        finalBase *= SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get();
        return new CacheValue<>(finalBase);
    }

    @Override
    public void eval(List<Modifier> modifiers, CacheValue<Float> cache) {
        double eval = AttachmentPropertyManager.eval(modifiers, cache.getValue());
        cache.setValue((float) eval);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        // 必要数据获取
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        BulletData bulletData = gunData.getBulletData();
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);

        // 额外伤害
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        // 开火模式调整
        // 最终的 base
        float finalBase = extraDamage != null ? extraDamage.getArmorIgnore() : 0;
        finalBase = fireModeAdjustData != null ? finalBase + fireModeAdjustData.getArmorIgnore() : finalBase;
        finalBase *= SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get();
        float modifier = cacheProperty.<Float>getCache(ArmorIgnoreModifier.ID) - finalBase;

        double percent = Mth.clamp(finalBase, 0, 1);
        double modifierPercent = Mth.clamp(modifier, 0, 1);
        finalBase *= 100;
        modifier *= 100;

        String titleKey = "gui.tacz.gun_refit.property_diagrams.armor_ignore";
        String positivelyString = String.format("%.1f%% §a(+%.1f%%)", finalBase, modifier);
        String negativelyString = String.format("%.1f%% §c(%.1f%%)", finalBase, modifier);
        String defaultString = String.format("%.1f%%", finalBase);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(percent, modifierPercent, modifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class ArmorIgnoreJsonProperty extends JsonProperty<Modifier> {
        public ArmorIgnoreJsonProperty(Modifier value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Modifier value = this.getValue();
            if (value != null) {
                double eval = AttachmentPropertyManager.eval(value, 0.5);
                if (eval > 0.5) {
                    components.add(Component.translatable("tooltip.tacz.attachment.armor_ignore.increase").withStyle(ChatFormatting.GREEN));
                } else if (eval < 0.5) {
                    components.add(Component.translatable("tooltip.tacz.attachment.armor_ignore.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public static class Data {
        @SerializedName("armor_ignore")
        @Nullable
        private Modifier armorIgnore = null;

        @Nullable
        public Modifier getArmorIgnore() {
            return armorIgnore;
        }
    }
}
