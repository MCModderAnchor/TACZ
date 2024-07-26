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
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class RecoilModifier implements IAttachmentModifier<RecoilModifier.Data, Pair<Float, Float>> {
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
    public JsonProperty<Data, Pair<Float, Float>> readJson(String json) {
        RecoilModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, RecoilModifier.Data.class);
        return new RecoilModifier.RecoilJsonProperty(data);
    }

    @Override
    public CacheProperty<Pair<Float, Float>> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheProperty<>(Pair.of(0f, 0f));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        Pair<Float, Float> propertyCache = cacheProperty.getCache(RecoilModifier.ID);
        GunRecoil recoil = gunData.getRecoil();

        float yaw = getMaxInGunRecoilKeyFrame(recoil.getYaw());
        float yawModifier = propertyCache.right();
        double yawPercent = Math.min(yaw / 5.0, 1);
        double yawModifierPercent = Math.min(yawModifier / 5.0, 1);
        String yawTitleKey = "gui.tacz.gun_refit.property_diagrams.yaw";
        String yawPositivelyString = String.format("%.2f §c(+%.2f)", yaw, yawModifier);
        String yawNegativelyString = String.format("%.2f §c(%.2f)", yaw, yawModifier);
        String yawDefaultString = String.format("%.2f", yaw);

        float pitch = getMaxInGunRecoilKeyFrame(recoil.getPitch());
        float pitchModifier = propertyCache.left();
        double pitchPercent = Math.min(pitch / 5.0, 1);
        double pitchModifierPercent = Math.min(pitchModifier / 5.0, 1);
        String pitchTitleKey = "gui.tacz.gun_refit.property_diagrams.pitch";
        String pitchPositivelyString = String.format("%.2f §c(+%.2f)", pitch, pitchModifier);
        String pitchNegativelyString = String.format("%.2f §c(%.2f)", pitch, pitchModifier);
        String pitchDefaultString = String.format("%.2f", pitch);

        boolean positivelyBetter = false;

        DiagramsData yawData = new DiagramsData(yawPercent, yawModifierPercent, yawModifier, yawTitleKey, yawPositivelyString, yawNegativelyString, yawDefaultString, positivelyBetter);
        DiagramsData pitchData = new DiagramsData(pitchPercent, pitchModifierPercent, pitchModifier, pitchTitleKey, pitchPositivelyString, pitchNegativelyString, pitchDefaultString, positivelyBetter);
        return List.of(yawData, pitchData);
    }

    @Override
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

    public static class RecoilJsonProperty extends JsonProperty<Data, Pair<Float, Float>> {
        public RecoilJsonProperty(Data value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Data jsonData = this.getValue();
            OldRecoilData oldRecoilData = jsonData.oldRecoilData;
            NewRecoilData newRecoilData = jsonData.newRecoilData;
            float pitch = 0f;
            float yaw = 0f;

            // 兼容旧版本
            if (newRecoilData == null && oldRecoilData != null) {
                pitch = oldRecoilData.getPitch();
                yaw = oldRecoilData.getYaw();
            }
            // 新版本读取
            if (newRecoilData != null) {
                pitch = (float) AttachmentPropertyManager.eval(newRecoilData.getPitch(), 0, 0);
                yaw = (float) AttachmentPropertyManager.eval(newRecoilData.getYaw(), 0, 0);
            }

            if (pitch > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.pitch.increase").withStyle(ChatFormatting.RED));
            } else if (pitch < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.pitch.decrease").withStyle(ChatFormatting.GREEN));
            }

            if (yaw > 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.yaw.increase").withStyle(ChatFormatting.RED));
            } else if (yaw < 0) {
                components.add(Component.translatable("tooltip.tacz.attachment.yaw.decrease").withStyle(ChatFormatting.GREEN));
            }
        }

        @Override
        public void eval(ItemStack gunItem, GunData gunData, CacheProperty<Pair<Float, Float>> cache) {
            Data jsonData = this.getValue();
            OldRecoilData oldRecoilData = jsonData.oldRecoilData;
            NewRecoilData newRecoilData = jsonData.newRecoilData;
            Pair<Float, Float> cacheValue = cache.getValue();

            // 兼容旧版本
            if (newRecoilData == null && oldRecoilData != null) {
                float pitch = oldRecoilData.getPitch() + cacheValue.left();
                float yaw = oldRecoilData.getYaw() + cacheValue.right();
                cache.setValue(Pair.of(pitch, yaw));
                return;
            }

            // 新版本读取
            if (newRecoilData != null) {
                double pitch = AttachmentPropertyManager.eval(newRecoilData.getPitch(), cacheValue.left(), 0);
                double yaw = AttachmentPropertyManager.eval(newRecoilData.getYaw(), cacheValue.right(), 0);
                cache.setValue(Pair.of((float) pitch, (float) yaw));
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
        private ModifiedValue pitch = new ModifiedValue();

        @SerializedName("yaw")
        private ModifiedValue yaw = new ModifiedValue();

        public ModifiedValue getPitch() {
            return pitch;
        }

        public ModifiedValue getYaw() {
            return yaw;
        }
    }
}
