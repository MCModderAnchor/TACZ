package com.tacz.guns.resource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum CommonAssetManager {
    INSTANCE;

    /**
     * 储存枪械 data 数据
     */
    private final Map<ResourceLocation, GunData> gunData = Maps.newHashMap();

    private final Map<ResourceLocation, AttachmentData> attachmentData = Maps.newHashMap();

    private final Map<ResourceLocation, GunSmithTableRecipe> gunSmithTableRecipes = Maps.newHashMap();

    private final Map<ResourceLocation, Set<String>> attachmentTags = Maps.newHashMap();

    private final Map<ResourceLocation, Set<String>> allowAttachmentTags = Maps.newHashMap();

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

    public Optional<GunSmithTableRecipe> getRecipe(ResourceLocation recipeId) {
        return Optional.ofNullable(gunSmithTableRecipes.get(recipeId));
    }

    public Map<ResourceLocation, GunSmithTableRecipe> getAllRecipes() {
        return gunSmithTableRecipes;
    }

    public void putAttachmentTags(ResourceLocation registryName, List<String> tags) {
        this.attachmentTags.computeIfAbsent(registryName, (id) -> Sets.newHashSet()).addAll(tags);
    }

    public Set<String> getAttachmentTags(ResourceLocation registryName) {
        return attachmentTags.get(registryName);
    }

    public void putAllowAttachmentTags(ResourceLocation registryName, List<String> tags) {
        this.allowAttachmentTags.computeIfAbsent(registryName, (id) -> Sets.newHashSet()).addAll(tags);
    }

    public Set<String> getAllowAttachmentTags(ResourceLocation registryName) {
        return allowAttachmentTags.get(registryName);
    }

    public void clearAll() {
        this.gunData.clear();
        this.attachmentData.clear();
        this.attachmentTags.clear();
        this.allowAttachmentTags.clear();
    }

    public void clearRecipes() {
        gunSmithTableRecipes.clear();
    }
}
