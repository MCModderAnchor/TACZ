package com.tac.guns.resource;

import com.google.common.collect.Maps;
import com.tac.guns.crafting.GunSmithTableRecipe;
import com.tac.guns.resource.pojo.data.attachment.AttachmentData;
import com.tac.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public enum CommonAssetManager {
    INSTANCE;

    /**
     * 储存枪械 data 数据
     */
    private final Map<ResourceLocation, GunData> gunData = Maps.newHashMap();

    private final Map<ResourceLocation, AttachmentData> attachmentData = Maps.newHashMap();

    private final Map<ResourceLocation, GunSmithTableRecipe> gunSmithTableRecipes = Maps.newHashMap();

    public void putGunData(ResourceLocation registryName, GunData data) {
        gunData.put(registryName, data);
    }

    public GunData getGunData(ResourceLocation registryName) {
        return gunData.get(registryName);
    }

    public void putAttachmentData(ResourceLocation registryName, AttachmentData data) {
        attachmentData.put(registryName, data);
    }

    public AttachmentData getAttachmentData(ResourceLocation registryName) {
        return attachmentData.get(registryName);
    }

    public void putRecipe(ResourceLocation registryName, GunSmithTableRecipe recipe) {
        gunSmithTableRecipes.put(registryName, recipe);
    }

    public Map<ResourceLocation, GunSmithTableRecipe> getAllRecipe() {
        return gunSmithTableRecipes;
    }

    public void clearAll() {
        // TODO：重载时清理缓存
    }

    public void clearRecipes() {
        gunSmithTableRecipes.clear();
    }
}
