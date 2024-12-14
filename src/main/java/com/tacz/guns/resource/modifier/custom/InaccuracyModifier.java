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

public class InaccuracyModifier implements IAttachmentModifier<Map<InaccuracyType, Modifier>, Map<InaccuracyType, Float>> {
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
    @SuppressWarnings("deprecation")
    public JsonProperty<Map<InaccuracyType, Modifier>> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        Modifier inaccuracy = data.getInaccuracy();
        // 兼容旧版本
        if (inaccuracy == null) {
            float inaccuracyAddendTime = data.getInaccuracyAddendTime();
            inaccuracy = new Modifier();
            inaccuracy.setAddend(inaccuracyAddendTime);
        }
        // 除去 aim 状态，全部写入一样的数值
        Map<InaccuracyType, Modifier> jsonProperties = Maps.newHashMap();
        for (InaccuracyType type : InaccuracyType.values()) {
            if (type.isAim()) {
                continue;
            }
            jsonProperties.put(type, inaccuracy);
        }
        return new InaccuracyJsonProperty(jsonProperties);
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
            if (type.isAim()) {
                continue;
            }
            for (Map<InaccuracyType, Modifier> value : modifiedValues) {
                tmpModified.computeIfAbsent(type, t -> Lists.newArrayList()).add(value.get(type));
            }
        }
        // 一次性把配件的数据计算完
        cache.getValue().forEach((type, value) -> {
            // 瞄准不应用此散布
            if (type.isAim()) {
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class InaccuracyJsonProperty extends JsonProperty<Map<InaccuracyType, Modifier>> {
        public InaccuracyJsonProperty(Map<InaccuracyType, Modifier> value) {
            super(value);
        }

        @Override
        public void initComponents() {
            var value = this.getValue();
            float inaccuracyAddend = 0;
            if (value != null && value.containsKey(InaccuracyType.STAND)) {
                // 传入默认值 5 进行测试，看看最终结果差值
                double eval = AttachmentPropertyManager.eval(value.get(InaccuracyType.STAND), 5);
                inaccuracyAddend = (float) (eval - 5);
            }
            // 添加文本提示
            if (inaccuracyAddend > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.inaccuracy.decrease").withStyle(ChatFormatting.RED));
            } else if (inaccuracyAddend < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.inaccuracy.increase").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class Data {
        @Nullable
        @SerializedName("inaccuracy")
        private Modifier inaccuracy;

        @SerializedName("inaccuracy_addend")
        @Deprecated
        private float adsAddendTime = 0;

        @Nullable
        public Modifier getInaccuracy() {
            return inaccuracy;
        }

        @Deprecated
        public float getInaccuracyAddendTime() {
            return adsAddendTime;
        }
    }
}
