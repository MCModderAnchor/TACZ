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
import com.tacz.guns.resource.pojo.data.gun.GunFireModeAdjustData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AmmoSpeedModifier implements IAttachmentModifier<AmmoSpeedModifier.Data, Float> {
    public static final String ID = "ammo_speed";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Data, Float> readJson(String json) {
        AmmoSpeedModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, AmmoSpeedModifier.Data.class);
        return new AmmoSpeedModifier.BulletSpeedJsonProperty(data);
    }

    @Override
    public CacheProperty<Float> initCache(ItemStack gunItem, GunData gunData) {
        IGun iGun = Objects.requireNonNull(IGun.getIGunOrNull(gunItem));
        FireMode fireMode = iGun.getFireMode(gunItem);
        GunFireModeAdjustData fireModeAdjustData = gunData.getFireModeAdjustData(fireMode);
        float speed = gunData.getBulletData().getSpeed();
        if (fireModeAdjustData != null) {
            speed += fireModeAdjustData.getSpeed();
        }
        return new CacheProperty<>(speed);
    }

    @Override
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        float ammoSpeed = gunData.getBulletData().getSpeed();
        float ammoSpeedModifier = cacheProperty.<Float>getCache(AmmoSpeedModifier.ID) - ammoSpeed;

        double ammoSpeedPercent = Math.min(ammoSpeed / 600.0, 1);
        double ammoSpeedModifierPercent = Math.min(ammoSpeedModifier / 600.0, 1);

        String titleKey = "gui.tacz.gun_refit.property_diagrams.ammo_speed";
        String positivelyString = String.format("%dm/s §a(+%d)", Math.round(ammoSpeed), Math.round(ammoSpeedModifier));
        String negativelyString = String.format("%dm/s §c(%d)", Math.round(ammoSpeed), Math.round(ammoSpeedModifier));
        String defaultString = String.format("%dm/s", Math.round(ammoSpeed));
        boolean positivelyBetter = true;

        DiagramsData diagramsData = new DiagramsData(ammoSpeedPercent, ammoSpeedModifierPercent, ammoSpeedModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        return Collections.singletonList(diagramsData);
    }

    public static class BulletSpeedJsonProperty extends JsonProperty<Data, Float> {
        public BulletSpeedJsonProperty(Data data) {
            super(data);
        }

        @Override
        public void initComponents() {
            ModifiedValue ammoSpeed = this.getValue().getAmmoSpeed();
            if (ammoSpeed != null) {
                double eval = AttachmentPropertyManager.eval(ammoSpeed, 300, 300);
                if (eval > 300) {
                    components.add(Component.translatable("tooltip.tacz.attachment.ammo_speed.increase").withStyle(ChatFormatting.GREEN));
                } else if (eval < 300) {
                    components.add(Component.translatable("tooltip.tacz.attachment.ammo_speed.decrease").withStyle(ChatFormatting.RED));
                }
            }
        }

        @Override
        public void eval(ItemStack gunItem, GunData gunData, CacheProperty<Float> cache) {
            ModifiedValue ammoSpeed = this.getValue().getAmmoSpeed();
            float cacheValue = cache.getValue();
            float defaultSpeed = gunData.getBulletData().getSpeed();
            if (ammoSpeed != null) {
                double eval = AttachmentPropertyManager.eval(ammoSpeed, cacheValue, defaultSpeed);
                cache.setValue((float) eval);
            }
        }
    }

    public static class Data {
        @SerializedName("ammo_speed")
        @Nullable
        private ModifiedValue ammoSpeed = null;

        @Nullable
        public ModifiedValue getAmmoSpeed() {
            return ammoSpeed;
        }
    }
}
