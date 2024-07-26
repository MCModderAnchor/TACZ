package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AdsModifier implements IAttachmentModifier<AdsModifier.Data, Float> {
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
    public JsonProperty<Data, Float> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        return new AdsJsonProperty(data);
    }

    @Override
    public CacheProperty<Float> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheProperty<>(gunData.getAimTime());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderPropertyDiagrams(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty,
                                       int barStartX, int barEndX, int barMaxWidth,
                                       int barBackgroundColor, int barBaseColor, int barPositivelyColor, int barNegativeColor,
                                       int fontColor, int nameTextStartX, int valueTextStartX,
                                       GuiGraphics graphics, Font font, int yOffset
    ) {
        // 开镜时间
        float aimTime = gunData.getAimTime();
        float adsTimeModifier = cacheProperty.<Float>getCache(AdsModifier.ID) - aimTime;
        double aimTimePercent = Math.min(aimTime, 1);
        int aimeTimeLength = (int) (barStartX + barMaxWidth * aimTimePercent);
        int adsModifierLength = Mth.clamp(aimeTimeLength + (int) (barMaxWidth * adsTimeModifier), barStartX, barEndX);

        graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ads"), nameTextStartX, yOffset, fontColor, false);
        graphics.fill(barStartX, yOffset + 2, barEndX, yOffset + 6, barBackgroundColor);
        graphics.fill(barStartX, yOffset + 2, aimeTimeLength, yOffset + 6, barBaseColor);
        if (adsTimeModifier > 0) {
            graphics.fill(aimeTimeLength, yOffset + 2, adsModifierLength, yOffset + 6, barNegativeColor);
            graphics.drawString(font, String.format("%.2fs §c(+%.2f)", aimTime, adsTimeModifier), valueTextStartX, yOffset, fontColor, false);
        } else if (adsTimeModifier < 0) {
            graphics.fill(adsModifierLength, yOffset + 2, aimeTimeLength, yOffset + 6, barPositivelyColor);
            graphics.drawString(font, String.format("%.2fs §a(%.2f)", aimTime, adsTimeModifier), valueTextStartX, yOffset, fontColor, false);
        } else {
            graphics.drawString(font, String.format("%.2fs", aimTime), valueTextStartX, yOffset, fontColor, false);
        }
    }

    public static class AdsJsonProperty extends JsonProperty<Data, Float> {
        public AdsJsonProperty(Data value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Data jsonData = this.getValue();
            float adsAddendTime;
            if (jsonData.getAds() == null) {
                // 兼容旧版本写法
                adsAddendTime = jsonData.getAdsAddendTime();
            } else {
                // 传入默认值 0.2 进行测试，看看最终结果差值
                double eval = AttachmentPropertyManager.eval(jsonData.getAds(), 0.2, 0.2);
                adsAddendTime = (float) (eval - 0.2);
            }

            // 添加文本提示
            if (adsAddendTime > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.ads.increase").withStyle(ChatFormatting.RED));
            } else if (adsAddendTime < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.ads.decrease").withStyle(ChatFormatting.GREEN));
            }
        }

        @Override
        public void eval(ItemStack gunItem, GunData gunData, CacheProperty<Float> cache) {
            Data jsonData = this.getValue();
            if (jsonData.getAds() == null) {
                // 兼容旧版本写法
                cache.setValue(cache.getValue() + jsonData.getAdsAddendTime());
            } else {
                cache.setValue((float) AttachmentPropertyManager.eval(jsonData.getAds(), cache.getValue(), gunData.getAimTime()));
            }
        }
    }

    public static class Data {
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
