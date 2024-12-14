package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.MoveSpeed;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;


import java.util.List;

/**
 * 这个字段使用modifier还是太奇怪了，姑且只用于缓存
 */
public class ExtraMovementModifier implements IAttachmentModifier<MoveSpeed, MoveSpeed> {
    public static final String ID = "movement_speed";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    @SuppressWarnings("deprecation")
    public JsonProperty<MoveSpeed> readJson(String json) {
        ExtraMovementModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, ExtraMovementModifier.Data.class);
        MoveSpeed moveSpeed = data.getMoveSpeed();
        return  new ExtraSpeedJsonProperty(moveSpeed);
    }

    @Override
    public CacheValue<MoveSpeed> initCache(ItemStack gunItem, GunData gunData) {
        return new CacheValue<>(gunData.getMoveSpeed());
    }

    @Override
    public void eval(List<MoveSpeed> modifiers, CacheValue<MoveSpeed> cache) {
        cache.setValue(MoveSpeed.of(cache.getValue(), modifiers));
    }

    public static class ExtraSpeedJsonProperty extends JsonProperty<MoveSpeed> {
        public ExtraSpeedJsonProperty(MoveSpeed value) {
            super(value);
        }

        @Override
        public void initComponents() {
            MoveSpeed speed = getValue();
            if(speed == null)return;
            resolveComponent(speed.getBaseMultiplier(), "movement_speed");
            resolveComponent(speed.getAimMultiplier(), "aim_speed");
            resolveComponent(speed.getReloadMultiplier(), "reload_speed");
        }

        private void resolveComponent(float amount, String key) {
            if (amount > 0) {
                components.add(Component.translatable(String.format("tooltip.tacz.attachment.%s.increase", key)).withStyle(ChatFormatting.GREEN));
            } else if (amount < 0) {
                components.add(Component.translatable(String.format("tooltip.tacz.attachment.%s.decrease", key)).withStyle(ChatFormatting.RED));
            }
        }
    }

    public static class Data {
        @SerializedName("movement_speed")
        @Nullable
        private MoveSpeed moveSpeed = null;

        @Nullable
        public MoveSpeed getMoveSpeed() {
            return moveSpeed;
        }
    }
}
