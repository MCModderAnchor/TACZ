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

public class WeightModifier implements IAttachmentModifier<Modifier, Float> {
    public static final String ID = "weight_modifier";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    @SuppressWarnings("deprecation")
    public JsonProperty<Modifier> readJson(String json) {
        WeightModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, WeightModifier.Data.class);
        Modifier weightModifier = data.getWeightModifier();
        // 兼容旧版本写法
        if (weightModifier == null) {
            weightModifier = new Modifier();
            weightModifier.setAddend(data.getWeightAddend());
        }
        return new WeightModifier.WeightJsonProperty(weightModifier);
    }

    @Override
    public CacheValue<Float> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheValue<>(gunData.getWeight());
    }

    @Override
    public void eval(List<Modifier> modifiers, CacheValue<Float> cache) {
        double eval = AttachmentPropertyManager.eval(modifiers, cache.getValue());
        cache.setValue((float) eval);
    }

    @Override
    public String getOptionalFields() {
        return "weight";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        float weight = gunData.getWeight() ;
        float modified = cacheProperty.<Float>getCache(WeightModifier.ID) - weight;

        double percent = Math.min(weight / 20.0, 1);
        double modifierPercent = Math.min(modified / 20.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.weight";
        String positivelyString = String.format("%.2fkg §c(+%.2f)", weight, modified);
        String negativelyString = String.format("%.2fkg §a(%.2f)", weight, modified);
        String defaultString = String.format("%.2fkg", weight);
        boolean positivelyBetter = false;

        DiagramsData diagramsData = new DiagramsData(percent, modifierPercent, modified, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class WeightJsonProperty extends JsonProperty<Modifier> {
        public WeightJsonProperty(Modifier value) {
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
                components.add(Component.translatable("tooltip.tacz.attachment.weight.increase").withStyle(ChatFormatting.RED));
            } else if (adsAddendTime < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.weight.decrease").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class Data {
        @Nullable
        @SerializedName("weight_modifier")
        private Modifier weightModifier;

        @SerializedName("weight")
        @Deprecated
        private float weightAddend = 0;

        @Nullable
        public Modifier getWeightModifier() {
            return weightModifier;
        }

        @Deprecated
        public float getWeightAddend() {
            return weightAddend;
        }
    }
}
