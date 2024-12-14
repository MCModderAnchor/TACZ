package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class EffectiveRangeModifier implements IAttachmentModifier<Modifier, Float> {
    public static final String ID = "effective_range";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Modifier> readJson(String json) {
        Data data = CommonGunPackLoader.GSON.fromJson(json, Data.class);
        return new EffectiveRangeJsonProperty(data.getEffectiveRange());
    }

    @Override
    public CacheValue<Float> initCache(ItemStack gunItem, GunData gunData) {
        LinkedList<DistanceDamagePair> damageAdjust = null;
        if (gunData.getBulletData().getExtraDamage() != null) {
            damageAdjust = gunData.getBulletData().getExtraDamage().getDamageAdjust();
        }
        float effectiveRange;
        if (damageAdjust != null) {
            effectiveRange = damageAdjust.get(0).getDistance();
        } else {
            effectiveRange = Integer.MAX_VALUE;
        }
        return new CacheValue<>(effectiveRange);
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
        float distanceModifier = cacheProperty.getCache(EffectiveRangeModifier.ID);
        LinkedList<DistanceDamagePair> damageAdjust = null;
        if (gunData.getBulletData().getExtraDamage() != null) {
            damageAdjust = gunData.getBulletData().getExtraDamage().getDamageAdjust();
        }
        float effectiveRange;
        if (damageAdjust != null) {
            effectiveRange = damageAdjust.get(0).getDistance();
        } else {
            effectiveRange = 0;
        }
        float modifier = distanceModifier - effectiveRange;

        double percent = Math.min(effectiveRange / 100.0, 1);
        double modifierPercent = Math.min(modifier / 100.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.effective_range";
        String positivelyString = String.format("%.1fm §a(+%.1f)", effectiveRange, modifier);
        String negativelyString = String.format("%.1fm §c(%.1f)", effectiveRange, modifier);
        String defaultString = String.format("%.1fm", effectiveRange);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(percent, modifierPercent, modifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class EffectiveRangeJsonProperty extends JsonProperty<Modifier> {
        public EffectiveRangeJsonProperty(Modifier value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Modifier value = getValue();
            if (value != null) {
                double eval = AttachmentPropertyManager.eval(value, 25);
                if (eval > 25) {
                    components.add(Component.translatable("tooltip.tacz.attachment.effective_range.increase").withStyle(ChatFormatting.GREEN));
                } else if (eval < 25) {
                    components.add(Component.translatable("tooltip.tacz.attachment.effective_range.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public static class Data {
        @SerializedName("effective_range")
        @Nullable
        private Modifier effectiveRange = null;

        @Nullable
        public Modifier getEffectiveRange() {
            return effectiveRange;
        }
    }
}
