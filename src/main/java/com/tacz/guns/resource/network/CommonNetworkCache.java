package com.tacz.guns.resource.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.ICommonResourceProvider;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.*;
import com.tacz.guns.resource.manager.AttachmentDataManager;
import com.tacz.guns.resource.manager.GunDataManager;
import com.tacz.guns.resource.pojo.AttachmentVariantIndexPOJO;
import com.tacz.guns.resource.pojo.SlotAdapterIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.SlotAdapterData;
import com.tacz.guns.resource.pojo.data.attachment.VariantData;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;

import java.util.*;

/**
 * 缃戠粶浣嶇疆鐨勭紦瀛?br/>
 * 鐢ㄤ簬瀛樺偍浠庣綉缁滆幏鍙栫殑鏁版嵁
 */
public enum CommonNetworkCache implements ICommonResourceProvider {
    INSTANCE;

    public Map<ResourceLocation, GunData> gunData = new HashMap<>();
    public Map<ResourceLocation, AttachmentData> attachmentData = new HashMap<>();
    public Map<ResourceLocation, VariantData> attachmentVariantData = new HashMap<>();
    public Map<ResourceLocation, SlotAdapterData> slotAdapterData = new HashMap<>();
    public Map<ResourceLocation, RecipeFilter> recipeFilter = new HashMap<>();
    public Map<ResourceLocation, BlockData> blockData = new HashMap<>();
    public Map<ResourceLocation, CommonGunIndex> gunIndex = new HashMap<>();
    public Map<ResourceLocation, CommonAmmoIndex> ammoIndex = new HashMap<>();
    public Map<ResourceLocation, CommonAttachmentIndex> attachmentIndex = new HashMap<>();
    public Map<ResourceLocation, CommonAttachmentVariantIndex> attachmentVariantIndex = new HashMap<>();
    public Map<ResourceLocation, CommonSlotAdapterIndex> slotAdapterIndex = new HashMap<>();
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
    public VariantData getAttachmentVariantData(ResourceLocation variantDataId) {
        return attachmentVariantData.get(variantDataId);
    }

    @Nullable
    @Override
    public SlotAdapterData getSlotAdapterData(ResourceLocation adapterDataId) {
        return slotAdapterData.get(adapterDataId);
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
    public @Nullable CommonAttachmentVariantIndex getAttachmentVariantIndex(ResourceLocation variantId) {
        return attachmentVariantIndex.get(variantId);
    }

    @Override
    public @Nullable CommonSlotAdapterIndex getSlotAdapterIndex(ResourceLocation adapterId) {
        return slotAdapterIndex.get(adapterId);
    }

    @Override
    public @Nullable CommonBlockIndex getBlockIndex(ResourceLocation blockId) {
        return blockIndex.get(blockId);
    }

    @Override
    public @Nullable LuaTable getScript(ResourceLocation scriptId) {
        return null;
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
    public Set<Map.Entry<ResourceLocation, CommonAttachmentVariantIndex>> getAllAttachmentVariants() {
        return attachmentVariantIndex.entrySet();
    }

    @Override
    public Set<Map.Entry<ResourceLocation, CommonSlotAdapterIndex>> getAllSlotAdapters() {
        return slotAdapterIndex.entrySet();
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

    public void clear() {
        gunData.clear();
        attachmentData.clear();
        attachmentVariantData.clear();
        slotAdapterData.clear();
        gunIndex.clear();
        ammoIndex.clear();
        attachmentIndex.clear();
        attachmentVariantIndex.clear();
        slotAdapterIndex.clear();
        blockIndex.clear();
        recipeFilter.clear();
        blockData.clear();
        attachmentTags.clear();
        allowAttachmentTags.clear();
        AllowAttachmentTagMatcher.resetCache();
    }

    public void fromNetwork(Map<DataType, Map<ResourceLocation, String>> cache) {
        clear();
        Map<DataType, Map<ResourceLocation, String>> delayed = new HashMap<>();
        for (Map.Entry<DataType, Map<ResourceLocation, String>> entry : cache.entrySet()) {
            switch (entry.getKey()) {
                case GUN_INDEX:
                case AMMO_INDEX:
                case ATTACHMENT_INDEX:
                case ATTACHMENT_VARIANT_INDEX:
                case SLOT_ADAPTER_INDEX:
                case BLOCK_INDEX:
                    delayed.put(entry.getKey(), entry.getValue());
                    break;
                default:
                    fromNetwork(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<DataType, Map<ResourceLocation, String>> entry : delayed.entrySet()) {
            fromNetwork(entry.getKey(), entry.getValue());
        }
    }

    private <T> T parse(String json, Class<T> dataClass) {
        return CommonAssetsManager.GSON.fromJson(json, dataClass);
    }

    private AttachmentData parseAttachmentData(String json) {
        AttachmentData data = CommonAssetsManager.GSON.fromJson(json, AttachmentData.class);
        JsonElement element = CommonAssetsManager.GSON.fromJson(json, JsonElement.class);
        if (data != null) {
            AttachmentDataManager.initModifiers(data, element, CommonAssetsManager.GSON);
        }
        return data;
    }

    private GunData parseGunData(String json) {
        GunData data = CommonAssetsManager.GSON.fromJson(json, GunData.class);
        JsonElement element = CommonAssetsManager.GSON.fromJson(json, JsonElement.class);
        GunDataManager.initExclusiveAttachmentModifiers(data, element);
        return data;
    }

    private void resolveAttachmentTags(Map<ResourceLocation, String> data) {
        for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
            List<String> parsed = CommonAssetsManager.GSON.fromJson(entry.getValue(), new TypeToken<>(){});
            Set<String> ordered = parsed == null
                    ? Collections.emptySet()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(parsed));
            if (entry.getKey().getPath().startsWith("allow_attachments/") && entry.getKey().getPath().length() > 18) {
                ResourceLocation gunId = entry.getKey().withPath(entry.getKey().getPath().substring(18));
                allowAttachmentTags.put(gunId, ordered);
            } else {
                attachmentTags.put(entry.getKey(), ordered);
            }
        }
    }

    private void fromNetwork(DataType type, Map<ResourceLocation, String> data) {
        if (type == DataType.ATTACHMENT_TAGS) {
            resolveAttachmentTags(data);
            return;
        }
        for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
            try {
                switch (type) {
                    case GUN_DATA -> gunData.put(entry.getKey(), parseGunData(entry.getValue()));
                    case GUN_INDEX -> gunIndex.put(entry.getKey(), parse(entry.getValue(), CommonGunIndex.class));
                    case AMMO_INDEX -> ammoIndex.put(entry.getKey(), parse(entry.getValue(), CommonAmmoIndex.class));
                    case ATTACHMENT_DATA -> attachmentData.put(entry.getKey(), parseAttachmentData(entry.getValue()));
                    case ATTACHMENT_INDEX -> attachmentIndex.put(entry.getKey(), parse(entry.getValue(), CommonAttachmentIndex.class));
                    case ATTACHMENT_VARIANT_DATA -> attachmentVariantData.put(entry.getKey(), parse(entry.getValue(), VariantData.class));
                    case ATTACHMENT_VARIANT_INDEX -> attachmentVariantIndex.put(entry.getKey(), CommonAttachmentVariantIndex.getInstance(parse(entry.getValue(), AttachmentVariantIndexPOJO.class)));
                    case SLOT_ADAPTER_DATA -> slotAdapterData.put(entry.getKey(), parse(entry.getValue(), SlotAdapterData.class));
                    case SLOT_ADAPTER_INDEX -> slotAdapterIndex.put(entry.getKey(), CommonSlotAdapterIndex.getInstance(parse(entry.getValue(), SlotAdapterIndexPOJO.class)));
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
