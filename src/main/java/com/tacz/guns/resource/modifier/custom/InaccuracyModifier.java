package com.tacz.guns.resource.modifier.custom;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InaccuracyModifier implements IAttachmentModifier<InaccuracyModifier.Data, Map<InaccuracyType, Float>> {
    public static final String ID = "inaccuracy";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getOptionalFields() {
        return "inaccuracy_addend";
    }

    @Override
    public JsonProperty<Data, Map<InaccuracyType, Float>> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        return new InaccuracyModifier.AdsJsonProperty(data);
    }

    @Override
    public CacheProperty<Map<InaccuracyType, Float>> initCache(ItemStack gunItem, GunData gunData) {
        Map<InaccuracyType, Float> tmp = Maps.newHashMap();
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        gunData.getInaccuracy().forEach((type, value) -> {
            float inaccuracyAddend = 0;
            GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
            if (fireModeAdjustData != null) {
                if (type == InaccuracyType.AIM) {
                    inaccuracyAddend = fireModeAdjustData.getAimInaccuracy();
                } else {
                    inaccuracyAddend = fireModeAdjustData.getOtherInaccuracy();
                }
            }
            float inaccuracy = gunData.getInaccuracy(type, inaccuracyAddend);
            tmp.put(type, inaccuracy);
        });
        return new CacheProperty<>(tmp);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        // 腰射扩散
        float standInaccuracy = gunData.getInaccuracy(InaccuracyType.STAND);
        if (fireModeAdjustData != null) {
            standInaccuracy += fireModeAdjustData.getOtherInaccuracy();
        }

        float inaccuracyModifier = cacheProperty.<Map<InaccuracyType, Float>>getCache(InaccuracyModifier.ID).get(InaccuracyType.STAND) - standInaccuracy;
        double standInaccuracyPercent = Math.min(standInaccuracy / 10.0, 1);
        double inaccuracyModifierPercent = Math.min(inaccuracyModifier / 10.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.hipfire_inaccuracy";
        String positivelyString = String.format("%.2f §c(+%.2f)", standInaccuracy, inaccuracyModifier);
        String negativelyString = String.format("%.2f §a(%.2f)", standInaccuracy, inaccuracyModifier);
        String defaultString = String.format("%.2f", standInaccuracy);
        boolean positivelyBetter = false;

        DiagramsData diagramsData = new DiagramsData(standInaccuracyPercent, inaccuracyModifierPercent, inaccuracyModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    public static class AdsJsonProperty extends JsonProperty<Data, Map<InaccuracyType, Float>> {
        public AdsJsonProperty(Data value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Data jsonData = this.getValue();
            float inaccuracyAddend;
            if (jsonData.getInaccuracy() == null) {
                // 兼容旧版本写法
                inaccuracyAddend = jsonData.getInaccuracyAddendTime();
            } else {
                // 传入默认值 5 进行测试，看看最终结果差值
                double eval = AttachmentPropertyManager.eval(jsonData.getInaccuracy(), 5, 5);
                inaccuracyAddend = (float) (eval - 5);
            }

            // 添加文本提示
            if (inaccuracyAddend > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.inaccuracy.increase").withStyle(ChatFormatting.RED));
            } else if (inaccuracyAddend < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.inaccuracy.decrease").withStyle(ChatFormatting.GREEN));
            }
        }

        @Override
        public void eval(ItemStack gunItem, GunData gunData, CacheProperty<Map<InaccuracyType, Float>> cache) {
            Data jsonData = this.getValue();
            Map<InaccuracyType, Float> tmp = Maps.newHashMap();
            if (jsonData.getInaccuracy() == null) {
                // 兼容旧版本写法
                cache.getValue().forEach((type, value) -> {
                    // 瞄准不应用此散布
                    if (type.isAim()) {
                        tmp.put(type, value);
                        return;
                    }
                    tmp.put(type, value + jsonData.getInaccuracyAddendTime());
                });
            } else {
                cache.getValue().forEach((type, value) -> {
                    // 瞄准不应用此散布
                    if (type.isAim()) {
                        tmp.put(type, value);
                        return;
                    }
                    float eval = (float) AttachmentPropertyManager.eval(jsonData.getInaccuracy(), cache.getValue().get(type), gunData.getInaccuracy(type));
                    tmp.put(type, eval);
                });
            }
            cache.setValue(tmp);
        }
    }

    public static class Data {
        @SerializedName("inaccuracy")
        private ModifiedValue inaccuracy;

        @SerializedName("inaccuracy_addend")
        @Deprecated
        private float adsAddendTime = 0;

        public ModifiedValue getInaccuracy() {
            return inaccuracy;
        }

        @Deprecated
        public float getInaccuracyAddendTime() {
            return adsAddendTime;
        }
    }
}
