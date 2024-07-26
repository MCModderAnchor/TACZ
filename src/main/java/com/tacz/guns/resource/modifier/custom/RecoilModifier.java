package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheProperty;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.ModifiedValue;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

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
        public void eval(GunData gunData, CacheProperty<Pair<Float, Float>> cache) {
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
