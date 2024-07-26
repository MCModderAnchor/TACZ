package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RpmModifier implements IAttachmentModifier<RpmModifier.Data, Integer> {
    public static final String ID = "rpm";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Data, Integer> readJson(String json) {
        RpmModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, RpmModifier.Data.class);
        return new RpmModifier.RpmJsonProperty(data);
    }

    @Override
    public CacheProperty<Integer> initCache(ItemStack gunItem, GunData gunData) {
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        int roundsPerMinute = gunData.getRoundsPerMinute(fireMode);
        return new CacheProperty<>(roundsPerMinute);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);

        int rpm = gunData.getRoundsPerMinute(fireMode);
        int rpmModifier = cacheProperty.<Integer>getCache(RpmModifier.ID) - rpm;

        double rpmPercent = Math.min(rpm / 1200.0, 1);
        double rpmModifierPercent = Math.min(rpmModifier / 1200.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.rpm";
        String positivelyString = String.format("%drpm §a(+%d)", rpm, rpmModifier);
        String negativelyString = String.format("%drpm §c(%d)", rpm, rpmModifier);
        String defaultString = String.format("%drpm", rpm);
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(rpmPercent, rpmModifierPercent, rpmModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    public static class RpmJsonProperty extends JsonProperty<Data, Integer> {
        public RpmJsonProperty(Data data) {
            super(data);
        }

        @Override
        public void initComponents() {
            Data jsonData = getValue();
            if (jsonData.getRpm() != null) {
                double eval = AttachmentPropertyManager.eval(jsonData.getRpm(), 300, 300);
                int rpm = (int) Math.round(eval);
                if (rpm > 300) {
                    components.add(Component.translatable("tooltip.tacz.attachment.rpm.increase").withStyle(ChatFormatting.GREEN));
                } else if (rpm < 300) {
                    components.add(Component.translatable("tooltip.tacz.attachment.rpm.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }

        @Override
        public void eval(ItemStack gunItem, GunData gunData, CacheProperty<Integer> cache) {
            Data jsonData = getValue();
            Integer cacheValue = cache.getValue();
            IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
            FireMode fireMode = iGun.getFireMode(gunItem);
            if (jsonData.getRpm() != null) {
                double eval = AttachmentPropertyManager.eval(jsonData.getRpm(), cacheValue, gunData.getRoundsPerMinute(fireMode));
                cache.setValue((int) Math.round(eval));
            }
        }
    }

    public static class Data {
        @SerializedName("rpm")
        @Nullable
        private ModifiedValue rpm = null;

        @Nullable
        public ModifiedValue getRpm() {
            return rpm;
        }
    }
}
