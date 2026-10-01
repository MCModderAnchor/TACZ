package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class PierceModifier implements IAttachmentModifier<Modifier, Integer> {
    public static final String ID = GunProperties.PIERCE.name();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Modifier> readJson(String json) {
        PierceModifier.Data data = CommonAssetsManager.GSON.fromJson(json, PierceModifier.Data.class);
        return new PierceModifier.PierceJsonProperty(data.getPierce());
    }

    @Override
    public CacheValue<Integer> initCache(ItemStack gunItem, GunData gunData) {
        int pierce = gunData.getBulletData().getPierce();
        return new CacheValue<>(pierce);
    }

    @Override
    public void eval(List<Modifier> modifiers, CacheValue<Integer> cache) {
        double eval = AttachmentPropertyManager.eval(modifiers, cache.getValue());
        cache.setValue((int) Math.round(eval));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        int pierce = gunData.getBulletData().getPierce();
        int modifiedValue = cacheProperty.<Integer>getCache(PierceModifier.ID);
        int pierceModifier = modifiedValue - pierce;

        double piercePercent = Math.min(pierce / 5.0, 1);
        double pierceModifierPercent = Math.min(pierceModifier / 5.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.pierce";
        String positivelyString = String.format("%d §a(+%d)", modifiedValue, pierceModifier);
        String negativelyString = String.format("%d §c(%d)", modifiedValue, pierceModifier);
        String defaultString = String.format("%d", modifiedValue);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(piercePercent, pierceModifierPercent, pierceModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class PierceJsonProperty extends JsonProperty<Modifier> {
        public PierceJsonProperty(Modifier value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Modifier pierce = getValue();
            if (pierce != null) {
                long eval = Math.round(AttachmentPropertyManager.eval(pierce, 5));
                eval = Math.max(eval, 1);
                if (eval > 5) {
                    components.add(Component.translatable("tooltip.tacz.attachment.pierce.increase").withStyle(ChatFormatting.GREEN));
                } else if (eval < 5) {
                    components.add(Component.translatable("tooltip.tacz.attachment.pierce.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public static class Data {
        @SerializedName("pierce")
        @Nullable
        private Modifier pierce = null;

        @Nullable
        public Modifier getPierce() {
            return pierce;
        }
    }
}
