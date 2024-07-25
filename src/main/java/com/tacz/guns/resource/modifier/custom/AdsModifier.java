package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;

public class AdsModifier implements IAttachmentModifier<AdsModifier.AdsData, Float> {
    public static final String ID = "ads";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<AdsData, Float> readJson(String json) {
        AdsData data = CommonGunPackLoader.GSON.fromJson(json, AdsData.class);
        return new AdsJsonProperty(data);
    }

    @Override
    public CacheProperty<Float> initCache(GunData gunData) {
        return new CacheProperty<>(gunData.getAimTime());
    }

    public static class AdsJsonProperty extends JsonProperty<AdsData, Float> {
        public AdsJsonProperty(AdsData value) {
            super(value);
        }

        @Override
        public void eval(GunData gunData, CacheProperty<Float> cache) {
            AdsData jsonData = this.getValue();
            if (jsonData.getAds() == null) {
                // 兼容旧版本写法
                cache.setValue(cache.getValue() + jsonData.getAdsAddendTime());
            } else {
                cache.setValue((float) AttachmentPropertyManager.eval(jsonData.getAds(), cache.getValue(), gunData.getAimTime()));
            }
        }
    }

    public static class AdsData {
        @SerializedName("ads")
        private ModifiedValue ads;

        @SerializedName("ads_addend")
        @Deprecated
        private float adsAddendTime = 0;

        public ModifiedValue getAds() {
            return ads;
        }

        @Deprecated
        public float getAdsAddendTime() {
            return adsAddendTime;
        }
    }
}
