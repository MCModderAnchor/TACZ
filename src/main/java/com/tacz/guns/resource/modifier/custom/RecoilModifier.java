package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.*;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

/**
 * left 是 Pitch
 * right 是 Yaw
 */
public class RecoilModifier implements IAttachmentModifier<Pair<Modifier, Modifier>, ParameterizedCachePair<Float, Float>> {
    public static final String ID = "recoil";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getOptionalFields() {
        return "recoil_modifier";
    }

    @Override
    @SuppressWarnings("deprecation")
    public JsonProperty<Pair<Modifier, Modifier>> readJson(String json) {
        RecoilModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, RecoilModifier.Data.class);
        NewRecoilData newRecoilData = data.newRecoilData;
        OldRecoilData oldRecoilData = data.oldRecoilData;
        // 兼容旧版本写法
        if (newRecoilData == null && oldRecoilData != null) {
            Modifier pitch = new Modifier();
            Modifier yaw = new Modifier();
            pitch.setPercent(oldRecoilData.getPitch());
            yaw.setPercent(oldRecoilData.getYaw());
            return new RecoilModifier.RecoilJsonProperty(Pair.of(pitch, yaw));
        }
        assert newRecoilData != null;
        return new RecoilModifier.RecoilJsonProperty(Pair.of(newRecoilData.getPitch(), newRecoilData.getYaw()));
    }

    @Override
    public CacheValue<ParameterizedCachePair<Float, Float>> initCache(ItemStack gunItem, GunData gunData) {
        GunRecoil recoil = gunData.getRecoil();
        if (recoil == null) {
            return new CacheValue<>(ParameterizedCachePair.of(0f, 0f));
        }
        float pitch = getMaxInGunRecoilKeyFrame(recoil.getPitch());
        float yaw = getMaxInGunRecoilKeyFrame(recoil.getYaw());
        return new CacheValue<>(ParameterizedCachePair.of(pitch, yaw));
    }

    @Override
    public void eval(List<Pair<Modifier, Modifier>> modifiedValues, CacheValue<ParameterizedCachePair<Float, Float>> cache) {
        List<Modifier> yaw = Lists.newArrayList();
        List<Modifier> pitch = Lists.newArrayList();
        for (var modifiedValue : modifiedValues) {
            pitch.add(modifiedValue.left());
            yaw.add(modifiedValue.right());
        }
        var newCache = ParameterizedCachePair.of(pitch, yaw, cache.getValue().left().getDefaultValue(),
                cache.getValue().right().getDefaultValue());
        cache.setValue(newCache);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        ParameterizedCachePair<Float, Float> propertyCache = cacheProperty.getCache(RecoilModifier.ID);
        GunRecoil recoil = gunData.getRecoil();

        double pitch = propertyCache.left().getDefaultValue();
        double pitchModifier = propertyCache.left().eval(getMaxInGunRecoilKeyFrame(recoil.getPitch())) - pitch;
        double pitchPercent = Math.min(pitch / 5.0, 1);
        double pitchModifierPercent = Math.min(pitchModifier / 5.0, 1);
        String pitchTitleKey = "gui.tacz.gun_refit.property_diagrams.pitch";
        String pitchPositivelyString = String.format("%.2f §c(+%.2f)", pitch, pitchModifier);
        String pitchNegativelyString = String.format("%.2f §a(%.2f)", pitch, pitchModifier);
        String pitchDefaultString = String.format("%.2f", pitch);

        double yaw = propertyCache.right().getDefaultValue();
        double yawModifier = propertyCache.right().eval(getMaxInGunRecoilKeyFrame(recoil.getYaw())) - yaw;
        double yawPercent = Math.min(yaw / 5.0, 1);
        double yawModifierPercent = Math.min(yawModifier / 5.0, 1);
        String yawTitleKey = "gui.tacz.gun_refit.property_diagrams.yaw";
        String yawPositivelyString = String.format("%.2f §c(+%.2f)", yaw, yawModifier);
        String yawNegativelyString = String.format("%.2f §a(%.2f)", yaw, yawModifier);
        String yawDefaultString = String.format("%.2f", yaw);

        boolean positivelyBetter = false;

        DiagramsData pitchData = new DiagramsData(pitchPercent, pitchModifierPercent, pitchModifier, pitchTitleKey, pitchPositivelyString, pitchNegativelyString, pitchDefaultString, positivelyBetter);
        DiagramsData yawData = new DiagramsData(yawPercent, yawModifierPercent, yawModifier, yawTitleKey, yawPositivelyString, yawNegativelyString, yawDefaultString, positivelyBetter);
        return List.of(pitchData, yawData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getDiagramsDataSize() {
        return 2;
    }

    private static float getMaxInGunRecoilKeyFrame(GunRecoilKeyFrame[] frames) {
        if (frames.length == 0) {
            return 0;
        }
        float[] value = frames[0].getValue();
        float leftValue = Math.abs(value[0]);
        float rightValue = Math.abs(value[1]);
        return Math.max(leftValue, rightValue);
    }

    public static class RecoilJsonProperty extends JsonProperty<Pair<Modifier, Modifier>> {
        public RecoilJsonProperty(Pair<Modifier, Modifier> value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Pair<Modifier, Modifier> modified = this.getValue();
            float pitch = 1;
            float yaw = 1;

            if (modified != null) {
                pitch = (float) AttachmentPropertyManager.eval(modified.left(), 1);
                yaw = (float) AttachmentPropertyManager.eval(modified.right(), 1);
            }

            if (pitch > 1) {
                components.add(Component.translatable("tooltip.tacz.attachment.pitch.increase").withStyle(ChatFormatting.RED));
            } else if (pitch < 1) {
                components.add(Component.translatable("tooltip.tacz.attachment.pitch.decrease").withStyle(ChatFormatting.GREEN));
            }
            if (yaw > 1) {
                components.add(Component.translatable("tooltip.tacz.attachment.yaw.increase").withStyle(ChatFormatting.RED));
            } else if (yaw < 1) {
                components.add(Component.translatable("tooltip.tacz.attachment.yaw.decrease").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class Data {
        @SerializedName("recoil_modifier")
        @Deprecated
        @Nullable
        private OldRecoilData oldRecoilData = null;

        @SerializedName("recoil")
        @Nullable
        private NewRecoilData newRecoilData = null;
    }

    @Deprecated
    private static class OldRecoilData {
        @SerializedName("pitch")
        private float pitch = 0;

        @SerializedName("yaw")
        private float yaw = 0;

        public float getPitch() {
            return pitch;
        }

        public float getYaw() {
            return yaw;
        }
    }

    private static class NewRecoilData {
        @SerializedName("pitch")
        private Modifier pitch = new Modifier();

        @SerializedName("yaw")
        private Modifier yaw = new Modifier();

        public Modifier getPitch() {
            return pitch;
        }

        public Modifier getYaw() {
            return yaw;
        }
    }
}
