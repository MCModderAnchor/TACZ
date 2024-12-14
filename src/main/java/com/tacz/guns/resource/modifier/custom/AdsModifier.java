package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class AdsModifier implements IAttachmentModifier<Modifier, Float> {
    public static final String ID = "ads";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getOptionalFields() {
        return "ads_addend";
    }


    @Override
    @SuppressWarnings("deprecation")
    public JsonProperty<Modifier> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        Modifier ads = data.getAds();
        // 兼容旧版本写法
        if (ads == null) {
            ads = new Modifier();
            ads.setAddend(data.getAdsAddendTime());
        }
        return new AdsJsonProperty(ads);
    }

    @Override
    public CacheValue<Float> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheValue<>(gunData.getAimTime());
    }

    @Override
    public void eval(List<Modifier> modifiers, CacheValue<Float> cache) {
        double eval = AttachmentPropertyManager.eval(modifiers, cache.getValue());
        cache.setValue((float) eval);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        float aimTime = gunData.getAimTime();
        float adsTimeModifier = cacheProperty.<Float>getCache(AdsModifier.ID) - aimTime;

        String titleKey = "gui.tacz.gun_refit.property_diagrams.ads";
        String positivelyString = String.format("%.2fs §c(+%.2f)", aimTime, adsTimeModifier);
        String negativelyString = String.format("%.2fs §a(%.2f)", aimTime, adsTimeModifier);
        String defaultString = String.format("%.2fs", aimTime);
        boolean positivelyBetter = false;

        DiagramsData diagramsData = new DiagramsData(aimTime, adsTimeModifier, adsTimeModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class AdsJsonProperty extends JsonProperty<Modifier> {
        public AdsJsonProperty(Modifier value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Modifier value = this.getValue();
            float adsAddendTime = 0;
            if (value != null) {
                // 传入默认值 0.2 进行测试，看看最终结果差值
                double eval = AttachmentPropertyManager.eval(value, 0.2);
                adsAddendTime = (float) (eval - 0.2);
            }
            // 添加文本提示
            if (adsAddendTime > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.ads.increase").withStyle(ChatFormatting.RED));
            } else if (adsAddendTime < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.ads.decrease").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class Data {
        @Nullable
        @SerializedName("ads")
        private Modifier ads;

        @SerializedName("ads_addend")
        @Deprecated
        private float adsAddendTime = 0;

        @Nullable
        public Modifier getAds() {
            return ads;
        }

        @Deprecated
        public float getAdsAddendTime() {
            return adsAddendTime;
        }
    }
}
