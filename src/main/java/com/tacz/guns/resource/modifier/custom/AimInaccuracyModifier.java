package com.tacz.guns.resource.modifier.custom;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AimInaccuracyModifier implements IAttachmentModifier<Map<InaccuracyType, Modifier>, Map<InaccuracyType, Float>> {
    public static final String ID = "aim_inaccuracy";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Map<InaccuracyType, Modifier>> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        Modifier inaccuracy = data.getAimInaccuracy();
        // 除去 aim 状态，全部写入一样的数值
        Map<InaccuracyType, Modifier> jsonProperties = Maps.newHashMap();
        for (InaccuracyType type : InaccuracyType.values()) {
            if (!type.isAim()) {
                continue;
            }
            jsonProperties.put(type, inaccuracy);
        }
        return new AimInaccuracyJsonProperty(jsonProperties);
    }

    @Override
    public CacheValue<Map<InaccuracyType, Float>> initCache(ItemStack gunItem, GunData gunData) {
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
        return new CacheValue<>(tmp);
    }

    @Override
    public void eval(List<Map<InaccuracyType, Modifier>> modifiedValues, CacheValue<Map<InaccuracyType, Float>> cache) {
        Map<InaccuracyType, Float> result = Maps.newHashMap();
        Map<InaccuracyType, List<Modifier>> tmpModified = Maps.newHashMap();
        // 先遍历，把配件的数据集中在一起
        for (InaccuracyType type : InaccuracyType.values()) {
            if (!type.isAim()) {
                continue;
            }
            for (Map<InaccuracyType, Modifier> value : modifiedValues) {
                tmpModified.computeIfAbsent(type, t -> Lists.newArrayList()).add(value.get(type));
            }
        }
        // 一次性把配件的数据计算完
        cache.getValue().forEach((type, value) -> {
            // 腰射不应用此散布
            if (!type.isAim()) {
                result.put(type, value);
                return;
            }
            double eval = AttachmentPropertyManager.eval(tmpModified.get(type), cache.getValue().get(type));
            result.put(type, (float) eval);
        });
        // 写入缓存
        cache.setValue(result);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        // 瞄准扩散
        float aimInaccuracy = gunData.getInaccuracy(InaccuracyType.AIM);
        if (fireModeAdjustData != null) {
            aimInaccuracy += fireModeAdjustData.getAimInaccuracy();
        }

        aimInaccuracy = (1f - aimInaccuracy) * 100f;
        float inaccuracyModifier = cacheProperty.<Map<InaccuracyType, Float>>getCache(AimInaccuracyModifier.ID).get(InaccuracyType.AIM);
        inaccuracyModifier = (1f - inaccuracyModifier) * 100f - aimInaccuracy;
        double aimInaccuracyPercent = Math.min(aimInaccuracy / 100.0, 1f);
        double inaccuracyModifierPercent = Math.min(inaccuracyModifier / 100.0, 1f);


        String titleKey = "gui.tacz.gun_refit.property_diagrams.aim_inaccuracy";
        String positivelyString = String.format("%.1f%% §a(+%.1f%%)", aimInaccuracy, inaccuracyModifier);
        String negativelyString = String.format("%.1f%% §c(%.1f%%)", aimInaccuracy, inaccuracyModifier);
        String defaultString = String.format("%.1f%%", aimInaccuracy);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(aimInaccuracyPercent, inaccuracyModifierPercent, inaccuracyModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class AimInaccuracyJsonProperty extends JsonProperty<Map<InaccuracyType, Modifier>> {
        public AimInaccuracyJsonProperty(Map<InaccuracyType, Modifier> value) {
            super(value);
        }

        @Override
        public void initComponents() {
            var value = this.getValue();
            float inaccuracyAddend = 0;
            if (value != null && value.containsKey(InaccuracyType.AIM)) {
                // 传入默认值 5 进行测试，看看最终结果差值
                double eval = AttachmentPropertyManager.eval(value.get(InaccuracyType.AIM), 0.15f);
                inaccuracyAddend = (float) (eval - 0.15f);
            }
            // 添加文本提示
            if (inaccuracyAddend > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.aim_inaccuracy.decrease").withStyle(ChatFormatting.RED));
            } else if (inaccuracyAddend < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.aim_inaccuracy.increase").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class Data {
        @Nullable
        @SerializedName("aim_inaccuracy")
        private Modifier aimInaccuracy;

        @Nullable
        public Modifier getAimInaccuracy() {
            return aimInaccuracy;
        }
    }
}
