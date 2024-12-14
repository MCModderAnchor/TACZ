package com.tacz.guns.resource.modifier.custom;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public class IgniteModifier implements IAttachmentModifier<Ignite, Ignite> {
    public static final String ID = "ignite";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public JsonProperty<Ignite> readJson(String json) {
        IgniteModifier.Data data = CommonGunPackLoader.GSON.fromJson(json, IgniteModifier.Data.class);
        return new IgniteJsonProperty(data.getIgnite());
    }

    @Override
    public CacheValue<Ignite> initCache(ItemStack gunItem, GunData gunData) {
        Ignite ignite = gunData.getBulletData().getIgnite();
        return new CacheValue<>(ignite);
    }

    @Override
    public void eval(List<Ignite> modifiedValues, CacheValue<Ignite> cache) {
        Ignite cacheValue = cache.getValue();
        List<Boolean> igniteEntityValues = Lists.newArrayList();
        igniteEntityValues.add(cacheValue.isIgniteEntity());
        List<Boolean> igniteBlockValues = Lists.newArrayList();
        igniteBlockValues.add(cacheValue.isIgniteBlock());
        modifiedValues.forEach(v -> {
            igniteEntityValues.add(v.isIgniteEntity());
            igniteBlockValues.add(v.isIgniteBlock());
        });
        boolean igniteEntity = AttachmentPropertyManager.eval(igniteEntityValues, false);
        boolean igniteBlock = AttachmentPropertyManager.eval(igniteBlockValues, false);
        cache.setValue(new Ignite(igniteEntity, igniteBlock));
    }

    public static class IgniteJsonProperty extends JsonProperty<Ignite> {
        public IgniteJsonProperty(Ignite value) {
            super(value);
        }

        @Override
        public void initComponents() {
            Ignite value = this.getValue();
            if (value == null) {
                return;
            }
            if (value.isIgniteEntity()) {
                components.add(Component.translatable("tooltip.tacz.attachment.ignite.entity").withStyle(ChatFormatting.GREEN));
            }
            if (value.isIgniteBlock()) {
                components.add(Component.translatable("tooltip.tacz.attachment.ignite.block").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private static class Data {
        @SerializedName("ignite")
        private Ignite ignite = new Ignite(false);

        @Nullable
        public Ignite getIgnite() {
            return ignite;
        }
    }
}
