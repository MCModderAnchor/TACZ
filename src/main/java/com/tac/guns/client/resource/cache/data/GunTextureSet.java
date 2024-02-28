package com.tac.guns.client.resource.cache.data;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

public class GunTextureSet {
    private final Map<String, ResourceLocation> modelTextures;
    @Nullable
    private ResourceLocation hudIcon;
    @Nullable
    private ResourceLocation slotIcon;

    public GunTextureSet() {
        modelTextures = Maps.newHashMap();
    }

    public Map<String, ResourceLocation> getModelTextures() {
        return modelTextures;
    }

    @Nullable
    public ResourceLocation getHudIcon() {
        return hudIcon;
    }

    public void setHudIcon(@Nullable ResourceLocation hudIcon) {
        this.hudIcon = hudIcon;
    }

    @Nullable
    public ResourceLocation getSlotIcon() {
        return slotIcon;
    }

    public void setSlotIcon(@Nullable ResourceLocation slotIcon) {
        this.slotIcon = slotIcon;
    }
}
