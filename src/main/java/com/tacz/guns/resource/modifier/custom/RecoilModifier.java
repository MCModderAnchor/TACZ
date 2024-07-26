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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderPropertyDiagrams(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty,
                                       int barStartX, int barEndX, int barMaxWidth,
                                       int barBackgroundColor, int barBaseColor, int barPositivelyColor, int barNegativeColor,
                                       int fontColor, int nameTextStartX, int valueTextStartX,
                                       GuiGraphics graphics, Font font, int yOffset
    ) {
        // 水平后坐力和垂直后坐力
        Pair<Float, Float> attachmentRecoilModifier = cacheProperty.getCache(RecoilModifier.ID);
        GunRecoil recoil = gunData.getRecoil();

        float yawRecoil = getMaxInGunRecoilKeyFrame(recoil.getYaw());
        double yawRecoilPercent = Math.min(yawRecoil / 5.0, 1);
        int yawLength = (int) (barStartX + barMaxWidth * yawRecoilPercent);
        int yawModifierLength = Mth.clamp(yawLength + (int) (barMaxWidth * attachmentRecoilModifier.right() / 5.0), barStartX, barEndX);

        float pitchRecoil = getMaxInGunRecoilKeyFrame(recoil.getPitch());
        double pitchRecoilPercent = Math.min(pitchRecoil / 5.0, 1);
        int pitchLength = (int) (barStartX + barMaxWidth * pitchRecoilPercent);
        int pitchModifierLength = Mth.clamp(pitchLength + (int) (barMaxWidth * attachmentRecoilModifier.left() / 5.0), barStartX, barEndX);

        graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.yaw"), nameTextStartX, yOffset, fontColor, false);
        graphics.fill(barStartX, yOffset + 2, barEndX, yOffset + 6, barBackgroundColor);
        graphics.fill(barStartX, yOffset + 2, yawLength, yOffset + 6, barBaseColor);
        if (attachmentRecoilModifier.right() > 0) {
            graphics.fill(yawLength, yOffset + 2, yawModifierLength, yOffset + 6, barNegativeColor);
            graphics.drawString(font, String.format("%.2f §c(+%.2f)", yawRecoil, attachmentRecoilModifier.right()), valueTextStartX, yOffset, fontColor, false);
        } else if (attachmentRecoilModifier.right() < 0) {
            graphics.fill(yawModifierLength, yOffset + 2, yawLength, yOffset + 6, barPositivelyColor);
            graphics.drawString(font, String.format("%.2f §a(%.2f)", yawRecoil, attachmentRecoilModifier.right()), valueTextStartX, yOffset, fontColor, false);
        } else {
            graphics.drawString(font, String.format("%.2f", yawRecoil), valueTextStartX, yOffset, fontColor, false);
        }

        yOffset += 10;

        graphics.drawString(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.pitch"), nameTextStartX, yOffset, fontColor, false);
        graphics.fill(barStartX, yOffset + 2, barEndX, yOffset + 6, barBackgroundColor);
        graphics.fill(barStartX, yOffset + 2, pitchLength, yOffset + 6, barBaseColor);
        if (attachmentRecoilModifier.left() > 0) {
            graphics.fill(pitchLength, yOffset + 2, pitchModifierLength, yOffset + 6, barNegativeColor);
            graphics.drawString(font, String.format("%.2f §c(+%.2f)", pitchRecoil, attachmentRecoilModifier.left()), valueTextStartX, yOffset, fontColor, false);
        } else if (attachmentRecoilModifier.left() < 0) {
            graphics.fill(pitchModifierLength, yOffset + 2, pitchLength, yOffset + 6, barPositivelyColor);
            graphics.drawString(font, String.format("%.2f §a(%.2f)", pitchRecoil, attachmentRecoilModifier.left()), valueTextStartX, yOffset, fontColor, false);
        } else {
            graphics.drawString(font, String.format("%.2f", pitchRecoil), valueTextStartX, yOffset, fontColor, false);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getYOffset() {
        return 20;
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
