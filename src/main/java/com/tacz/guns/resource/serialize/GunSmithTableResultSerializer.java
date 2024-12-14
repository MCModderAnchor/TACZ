package com.tacz.guns.resource.serialize;

import com.google.gson.*;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.crafting.result.RawGunTableResult;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;


public class GunSmithTableResultSerializer implements JsonDeserializer<GunSmithTableResult> {

    @Override
    public GunSmithTableResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            String typeName = GsonHelper.getAsString(jsonObject, "type");
            int count = 1;
            CompoundTag extraTag = null;
            if (jsonObject.has("count")) {
                count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
            }
            if (jsonObject.has("nbt")) {
                extraTag = CraftingHelper.getNBT(jsonObject.get("nbt"));
            }

            GunSmithTableResult result;
            switch (typeName) {
                case GunSmithTableResult.GUN,GunSmithTableResult.AMMO,GunSmithTableResult.ATTACHMENT -> {
                    RawGunTableResult raw = new RawGunTableResult(typeName, getId(jsonObject), count);
                    if (extraTag != null) {
                        raw.setNbt(extraTag);
                    }
                    if (typeName.equals(GunSmithTableResult.GUN)) {
                        GunResult gunResult = CommonAssetsManager.GSON.fromJson(jsonObject, GunResult.class);
                        if (gunResult != null) {
                            raw.setExtraData(gunResult);
                        }
                    }
                    result = new GunSmithTableResult(raw);
                }
                case GunSmithTableResult.CUSTOM -> {
                    JsonObject resultObject = GsonHelper.getAsJsonObject(jsonObject, "item");
                    String group = GsonHelper.getAsString(jsonObject, "group", StringUtils.EMPTY);
                    ItemStack itemStack = CraftingHelper.getItemStack(resultObject, true);
                    result = new GunSmithTableResult(itemStack, group);
                    if (extraTag != null) {
                        CompoundTag itemTag = result.getResult().getOrCreateTag();
                        for (String key : extraTag.getAllKeys()) {
                            Tag tag = extraTag.get(key);
                            if (tag != null) {
                                itemTag.put(key, tag);
                            }
                        }
                    }
                }
                default -> {
                    return new GunSmithTableResult(ItemStack.EMPTY, StringUtils.EMPTY);
                }
            }
            return result;
        }
        return new GunSmithTableResult(ItemStack.EMPTY, StringUtils.EMPTY);
    }

    private ResourceLocation getId(JsonObject jsonObject) {
        return new ResourceLocation(GsonHelper.getAsString(jsonObject, "id"));
    }
}
