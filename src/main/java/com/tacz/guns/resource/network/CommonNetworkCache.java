package com.tacz.guns.resource.network;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.ICommonResourceProvider;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

import java.util.*;

/**
 * 网络位置的缓存<br/>
 * 用于存储从网络获取的数据
 */
public enum CommonNetworkCache implements ICommonResourceProvider {
    INSTANCE;

    public Map<ResourceLocation, GunData> gunData = new HashMap<>();
    public Map<ResourceLocation, AttachmentData> attachmentData = new HashMap<>();
    public Map<ResourceLocation, RecipeFilter> recipeFilter = new HashMap<>();
    public Map<ResourceLocation, BlockData> blockData = new HashMap<>();
    public Map<ResourceLocation, CommonGunIndex> gunIndex = new HashMap<>();
    public Map<ResourceLocation, CommonAmmoIndex> ammoIndex = new HashMap<>();
    public Map<ResourceLocation, CommonAttachmentIndex> attachmentIndex = new HashMap<>();
    public Map<ResourceLocation, CommonBlockIndex> blockIndex = new HashMap<>();
    public Map<ResourceLocation, Set<String>> attachmentTags = new HashMap<>();
    public Map<ResourceLocation, Set<String>> allowAttachmentTags = new HashMap<>();

    @Nullable
    @Override
    public GunData getGunData(ResourceLocation id) {
        return gunData.get(id);
    }

    @Nullable
    @Override
    public AttachmentData getAttachmentData(ResourceLocation attachmentId) {
        return attachmentData.get(attachmentId);
    }

    @Nullable
    @Override
    public BlockData getBlockData(ResourceLocation id) {
        return blockData.get(id);
    }

    @Nullable
    @Override
    public RecipeFilter getRecipeFilter(ResourceLocation id) {
        return recipeFilter.get(id);
    }

    @Nullable
    @Override
    public CommonGunIndex getGunIndex(ResourceLocation id) {
        return gunIndex.get(id);
    }

    @Override
    public @Nullable CommonAmmoIndex getAmmoIndex(ResourceLocation ammoId) {
        return ammoIndex.get(ammoId);
    }

    @Override
    public @Nullable CommonAttachmentIndex getAttachmentIndex(ResourceLocation attachmentId) {
        return attachmentIndex.get(attachmentId);
    }

    @Override
    public @Nullable CommonBlockIndex getBlockIndex(ResourceLocation blockId) {
        return blockIndex.get(blockId);
    }

    @Override
    public @Nullable LuaTable getScript(ResourceLocation scriptId) {
        return null; // 脚本不需要同步
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllGuns() {
        return gunIndex.entrySet();
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllAmmos() {
        return ammoIndex.entrySet();
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllAttachments() {
        return attachmentIndex.entrySet();
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonBlockIndex>> getAllBlocks() {
        return blockIndex.entrySet();
    }

    @Override
    public Set<String> getAttachmentTags(ResourceLocation registryName) {
        return attachmentTags.get(registryName);
    }

    @Override
    public Set<String> getAllowAttachmentTags(ResourceLocation registryName) {
        return allowAttachmentTags.get(registryName);
    }

    public void fromNetwork(Map<DataType, Map<ResourceLocation, String>> cache) {
        gunData.clear();
        attachmentData.clear();
        gunIndex.clear();
        ammoIndex.clear();
        attachmentIndex.clear();
        blockIndex.clear();
        recipeFilter.clear();
        blockData.clear();

        attachmentTags.clear();
        allowAttachmentTags.clear();
        // 延后处理
        Map<DataType, Map<ResourceLocation, String>> delayed = new HashMap<>();
        for (Map.Entry<DataType, Map<ResourceLocation, String>> entry : cache.entrySet()) {
            switch (entry.getKey()) {
                case GUN_INDEX:
                case AMMO_INDEX:
                case ATTACHMENT_INDEX:
                case BLOCK_INDEX:
                    delayed.put(entry.getKey(), entry.getValue());
                    break;
                default: fromNetwork(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<DataType, Map<ResourceLocation, String>> entry : delayed.entrySet()) {
            fromNetwork(entry.getKey(), entry.getValue());
        }
    }

    private <T> T parse(String json, Class<T> dataClass) {
        return CommonAssetsManager.GSON.fromJson(json, dataClass);
    }

    private void resolveAttachmentTags(Map<ResourceLocation, String> data) {
        for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
            List<String> tags = CommonAssetsManager.GSON.fromJson(entry.getValue(), new TypeToken<>(){});
            if (entry.getKey().getPath().startsWith("allow_attachments/") && entry.getKey().getPath().length()>18) {
                ResourceLocation gunId = entry.getKey().withPath(entry.getKey().getPath().substring(18));
                allowAttachmentTags.computeIfAbsent(gunId, (v) -> new HashSet<>()).addAll(tags);
            } else {
                attachmentTags.computeIfAbsent(entry.getKey(), (v) -> new HashSet<>()).addAll(tags);
            }
        }
    }


    private void fromNetwork(DataType type, Map<ResourceLocation, String> data) {
        for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
            try {
                switch (type) {
                    case GUN_DATA -> gunData.put(entry.getKey(), parse(entry.getValue(), GunData.class));
                    case GUN_INDEX -> gunIndex.put(entry.getKey(), parse(entry.getValue(), CommonGunIndex.class));
                    case AMMO_INDEX -> ammoIndex.put(entry.getKey(), parse(entry.getValue(), CommonAmmoIndex.class));
                    case ATTACHMENT_DATA -> attachmentData.put(entry.getKey(), parse(entry.getValue(), AttachmentData.class));
                    case ATTACHMENT_INDEX -> attachmentIndex.put(entry.getKey(), parse(entry.getValue(), CommonAttachmentIndex.class));
                    case ATTACHMENT_TAGS -> resolveAttachmentTags(data);
                    case BLOCK_INDEX -> blockIndex.put(entry.getKey(), parse(entry.getValue(), CommonBlockIndex.class));
                    case RECIPE_FILTER -> recipeFilter.put(entry.getKey(), parse(entry.getValue(), RecipeFilter.class));
                    case BLOCK_DATA -> blockData.put(entry.getKey(), parse(entry.getValue(), BlockData.class));
                }
            } catch (IllegalArgumentException | JsonParseException exception) {
                GunMod.LOGGER.warn("Failed to parse data from network for {} with id {}", type, entry.getKey(), exception);
            }
        }
    }
}
