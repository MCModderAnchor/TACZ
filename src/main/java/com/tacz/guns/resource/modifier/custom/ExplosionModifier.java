package com.tacz.guns.resource.modifier.custom;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class ExplosionModifier implements IAttachmentModifier<ExplosionModifier.ExplosionModifierValue, ExplosionData> {
    public static final String ID = "explosion";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<ExplosionModifierValue> readJson(String json) {
        ExplosionModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, ExplosionModifier.Data.class);
        return new ExplosionModifier.ExplosionJsonProperty(data.getExplosion());
    }

    @Override
    public CacheValue<ExplosionData> initCache(ItemStack gunItem, GunData gunData) {
        ExplosionData explosionData = gunData.getBulletData().getExplosionData();
        if (explosionData == null) {
            explosionData = new ExplosionData(false, 0.5f, 2, false, 30, false);
        }
        return new CacheValue<>(explosionData);
    }

    @Override
    public void eval(List<ExplosionModifierValue> modifiedValues, CacheValue<ExplosionData> cache) {
        ExplosionData cacheValue = cache.getValue();

        List<Boolean> explodeValues = Lists.newArrayList();
        explodeValues.add(cacheValue.isExplode());
        List<Modifier> radiusValues = Lists.newArrayList();
        List<Modifier> damageValues = Lists.newArrayList();
        List<Boolean> knockbackValues = Lists.newArrayList();
        knockbackValues.add(cacheValue.isKnockback());
        List<Boolean> destroyBlockValues = Lists.newArrayList();
        destroyBlockValues.add(cacheValue.isDestroyBlock());
        List<Modifier> delayValues = Lists.newArrayList();

        modifiedValues.forEach(v -> {
            explodeValues.add(v.explode);
            radiusValues.add(v.radius);
            damageValues.add(v.damage);
            knockbackValues.add(v.knockback);
            destroyBlockValues.add(v.destroyBlock);
            delayValues.add(v.delay);
        });

        boolean explode = cacheValue.isExplode() || AttachmentPropertyManager.eval(explodeValues, false);
        // 如果还是没有爆炸，那就没必要计算后面数值了
        if (!explode) {
            return;
        }
        float radius = (float) AttachmentPropertyManager.eval(radiusValues, cacheValue.getRadius());
        float damage = (float) AttachmentPropertyManager.eval(damageValues, cacheValue.getDamage());
        boolean knockback = AttachmentPropertyManager.eval(knockbackValues, false);
        boolean destroyBlock = AttachmentPropertyManager.eval(destroyBlockValues, false);
        int delay = (int) AttachmentPropertyManager.eval(delayValues, cacheValue.getDelay());
        ExplosionData explosionData = new ExplosionData(true, radius, damage, knockback, delay, destroyBlock);
        cache.setValue(explosionData);
    }

    public static class ExplosionJsonProperty extends JsonProperty<ExplosionModifierValue> {
        public ExplosionJsonProperty(ExplosionModifier.ExplosionModifierValue value) {
            super(value);
        }

        @Override
        public void initComponents() {
            ExplosionModifierValue modifierValue = getValue();
            if (modifierValue != null && modifierValue.explode) {
                components.add(Component.translatable("tooltip.tacz.attachment.explosion").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static class Data {
        @Nullable
        @SerializedName("explosion")
        private ExplosionModifierValue explosion = null;

        @Nullable
        public ExplosionModifierValue getExplosion() {
            return explosion;
        }
    }

    public static class ExplosionModifierValue {
        /**
         * 需要显式开启爆炸！
         */
        @SerializedName("explode")
        private boolean explode = false;

        @SerializedName("radius")
        private Modifier radius = new Modifier();

        @SerializedName("damage")
        private Modifier damage = new Modifier();

        @SerializedName("knockback")
        private boolean knockback = false;

        @SerializedName("destroy_block")
        private boolean destroyBlock = false;

        @SerializedName("delay")
        private Modifier delay = new Modifier();

        public boolean isExplode() {
            return explode;
        }
    }
}
