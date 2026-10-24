package com.tacz.guns.resource.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

/** Upgrades complete stacks; recipe predicates deliberately use separate adapters. */
public final class LegacyItemStackUpgrader {
    public static final int SOURCE_DATA_VERSION = 3465;
    public static final int TARGET_DATA_VERSION = 3955;
    private final RegistryOps<JsonElement> ops;

    public LegacyItemStackUpgrader(HolderLookup.Provider registries) {
        this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
    }

    public RegistryOps<JsonElement> ops() {
        return ops;
    }

    public static CompoundTag readNbt(JsonElement value) {
        try {
            if (value == null || value.isJsonNull()) return new CompoundTag();
            if (!value.isJsonObject() && !(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())) {
                throw new IllegalArgumentException("NBT must be an object or an SNBT string");
            }
            return TagParser.parseTag(value.isJsonObject() ? value.toString() : value.getAsString());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalArgumentException("Invalid item NBT: " + e.getMessage(), e);
        }
    }

    public JsonObject upgrade(JsonObject item) {
        return upgrade(item, null, "$");
    }

    public JsonObject upgrade(JsonObject item, PackConversionPlan report, String path) {
        for (String key : item.keySet()) {
            if (!java.util.Set.of("item", "count", "nbt").contains(key)) {
                throw new IllegalArgumentException("Unsupported legacy item field: " + key);
            }
        }
        if (!item.has("item")) throw new IllegalArgumentException("Legacy item is missing item ID");
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", item.get("item").getAsString());
        legacy.putInt("Count", item.has("count") ? item.get("count").getAsInt() : 1);
        legacy.put("tag", readNbt(item.get("nbt")));
        return upgradeTag(legacy, 0, report, path).json();
    }

    public JsonObject upgradeComponents(String itemId, JsonElement nbt) {
        return upgradeComponents(itemId, nbt, null, "$");
    }

    public JsonObject upgradeComponents(String itemId, JsonElement nbt, PackConversionPlan report, String path) {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", itemId);
        legacy.putByte("Count", (byte) 1);
        legacy.put("tag", readNbt(nbt));
        JsonObject output = upgradeTag(legacy, 0, report, path).json();
        JsonObject components = output.has("components") ? output.getAsJsonObject("components") : new JsonObject();
        ConversionValidation.validate(net.minecraft.core.component.DataComponentPatch.CODEC, ops, components, report, path);
        return components;
    }

    private UpgradedStack upgradeTag(CompoundTag legacy, int depth, PackConversionPlan report, String path) {
        if (depth > 16) throw new IllegalArgumentException("Embedded attachment nesting exceeds 16 levels");
        if (legacy.isEmpty()) return new UpgradedStack(legacy, new JsonObject());
        if (!legacy.contains("id", Tag.TAG_STRING)) throw new IllegalArgumentException("Embedded stack is missing id");
        ResourceLocation.parse(legacy.getString("id"));
        if (legacy.contains("components")) throw new IllegalArgumentException("Expected a 1.20.1 stack, found components");
        if (legacy.contains("ForgeCaps")) throw new IllegalArgumentException("Forge capability data requires a mod-specific adapter");
        int count = legacy.contains("Count") ? legacy.getInt("Count") : 1;
        if (depth > 0 && legacy.getString("id").equals("minecraft:air") && count == 0) return new UpgradedStack(new CompoundTag(), new JsonObject());
        if (count < 1 || count > 99) throw new IllegalArgumentException("Item count is outside the 1.21.1 codec range 1..99: " + count);
        CompoundTag input = legacy.copy();
        if (input.contains("tag", Tag.TAG_COMPOUND)) {
            CompoundTag tag = input.getCompound("tag");
            for (AttachmentType type : AttachmentType.values()) {
                if (type == AttachmentType.NONE) continue;
                String key = "Attachment" + type.name();
                if (tag.contains(key, Tag.TAG_COMPOUND)) tag.put(key, upgradeTag(tag.getCompound(key), depth + 1, report, path + ".nbt." + key).nbt());
            }
        }
        // Forge recipe counts are integers. Do not truncate them through the legacy byte field.
        input.putByte("Count", (byte) 1);
        Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
            new Dynamic<Tag>(NbtOps.INSTANCE, input), SOURCE_DATA_VERSION, TARGET_DATA_VERSION);
        if (!(fixed.getValue() instanceof CompoundTag result) || result.contains("tag")) {
            throw new IllegalArgumentException("Item data fixer did not produce a component stack");
        }
        result.putInt("count", count);
        // TACZ also interprets the legacy vanilla HideFlags mask as its own tooltip-part mask.
        // Keep that custom meaning in addition to vanilla's component migration.
        if (legacy.getString("id").startsWith("tacz:") && legacy.getCompound("tag").contains("HideFlags", Tag.TAG_ANY_NUMERIC)) {
            CompoundTag components = result.getCompound("components");
            CompoundTag custom = components.getCompound("minecraft:custom_data");
            custom.put("HideFlags", legacy.getCompound("tag").get("HideFlags").copy());
            components.put("minecraft:custom_data", custom);
            result.put("components", components);
        }
        // Running DFU with JsonOps preserves typed component booleans without requiring item registration.
        // Keep the parallel NBT result for custom data, whose numeric/array types JSON cannot express.
        JsonObject json = DataFixers.getDataFixer().update(References.ITEM_STACK,
            new Dynamic<>(JsonOps.INSTANCE, NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, input)),
            SOURCE_DATA_VERSION, TARGET_DATA_VERSION).getValue().getAsJsonObject();
        if (json.has("tag")) throw new IllegalArgumentException("Item data fixer did not produce a component stack");
        json.addProperty("count", count);
        preserveCustomDataTypes(result, json);
        ConversionValidation.validate(ItemStack.CODEC, ops, json, report, path);
        return new UpgradedStack(result, json);
    }

    private static void preserveCustomDataTypes(Tag nbt, JsonElement json) {
        if (nbt instanceof CompoundTag compound && json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            for (String key : compound.getAllKeys()) {
                Tag value = compound.get(key);
                if (value instanceof CompoundTag && java.util.Set.of("minecraft:custom_data", "minecraft:entity_data", "minecraft:block_entity_data", "minecraft:bucket_entity_data").contains(key)) {
                    object.addProperty(key, value.toString());
                } else {
                    if (key.equals("components") && !object.has(key)) object.add(key, new JsonObject());
                    if (object.has(key)) preserveCustomDataTypes(value, object.get(key));
                }
            }
        } else if (nbt instanceof net.minecraft.nbt.ListTag list && json.isJsonArray()) {
            for (int i = 0; i < Math.min(list.size(), json.getAsJsonArray().size()); i++) preserveCustomDataTypes(list.get(i), json.getAsJsonArray().get(i));
        }
    }

    private record UpgradedStack(CompoundTag nbt, JsonObject json) { }
}
