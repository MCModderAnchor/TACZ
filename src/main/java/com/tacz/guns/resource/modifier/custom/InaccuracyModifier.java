package com.tacz.guns.resource.modifier.custom;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.minecraft.world.item.ItemStack;

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

    public static class AdsJsonProperty extends JsonProperty<Data, Map<InaccuracyType, Float>> {
        public AdsJsonProperty(Data value) {
            super(value);
        }

        @Override
        public void eval(GunData gunData, CacheProperty<Map<InaccuracyType, Float>> cache) {
            Data jsonData = this.getValue();
            if (jsonData.getInaccuracy() == null) {
                // 兼容旧版本写法
                Map<InaccuracyType, Float> tmp = Maps.newHashMap();
                cache.getValue().forEach((type, value) -> {
                    // 瞄准不应用此散布
                    if (type.isAim()) {
                        tmp.put(type, value);
                        return;
                    }
                    tmp.put(type, value + jsonData.getInaccuracyAddendTime());
                });
                cache.setValue(tmp);
            } else {
                Map<InaccuracyType, Float> tmp = Maps.newHashMap();
                cache.getValue().forEach((type, value) -> {
                    // 瞄准不应用此散布
                    if (type.isAim()) {
                        tmp.put(type, value);
                        return;
                    }
                    float eval = (float) AttachmentPropertyManager.eval(jsonData.getInaccuracy(), cache.getValue().get(type), gunData.getInaccuracy(type));
                    tmp.put(type, eval);
                });
                cache.setValue(tmp);
            }
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
