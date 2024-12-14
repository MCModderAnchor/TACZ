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

public class PierceModifier implements IAttachmentModifier<Modifier, Integer> {
    public static final String ID = "pierce";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Modifier> readJson(String json) {
        PierceModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, PierceModifier.Data.class);
        return new PierceModifier.DamageJsonProperty(data.getPierce());
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
        int pierceModifier = cacheProperty.<Integer>getCache(PierceModifier.ID) - pierce;

        double piercePercent = Math.min(pierce / 10.0, 1);
        double pierceModifierPercent = Math.min(pierceModifier / 10.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.pierce";
        String positivelyString = String.format("%d §a(+%d)", pierce, pierceModifier);
        String negativelyString = String.format("%d §c(%d)", pierce, pierceModifier);
        String defaultString = String.format("%d", pierce);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(piercePercent, pierceModifierPercent, pierceModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 1;
    }

    public static class DamageJsonProperty extends JsonProperty<Modifier> {
        public DamageJsonProperty(Modifier value) {
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