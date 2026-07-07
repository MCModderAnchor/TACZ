package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilPushData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class RecoilPushModifier implements IAttachmentModifier<GunRecoilPushData, GunRecoilPushData> {
    public static final String ID = GunProperties.RECOIL_PUSH.name();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<GunRecoilPushData> readJson(String json) {
        RecoilPushModifier.Data data = CommonAssetsManager.GSON.fromJson(json, RecoilPushModifier.Data.class);
        return new RecoilPushModifier.RecoilPushJsonProperty(data == null ? null : data.getRecoilPush());
    }

    @Override
    public CacheValue<GunRecoilPushData> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheValue<>(gunData.getRecoilPushData());
    }

    @Override
    public void eval(List<GunRecoilPushData> modifiedValues, CacheValue<GunRecoilPushData> cache) {
        for (GunRecoilPushData modifiedValue : modifiedValues) {
            if (modifiedValue != null) {
                cache.setValue(modifiedValue);
            }
        }
    }

    public static class RecoilPushJsonProperty extends JsonProperty<GunRecoilPushData> {
        public RecoilPushJsonProperty(@Nullable GunRecoilPushData value) {
            super(value);
        }

        @Override
        public void initComponents() {
        }
    }

    private static class Data {
        @Nullable
        @SerializedName("recoil_push")
        private GunRecoilPushData recoilPush = null;

        @Nullable
        public GunRecoilPushData getRecoilPush() {
            return recoilPush;
        }
    }
}
