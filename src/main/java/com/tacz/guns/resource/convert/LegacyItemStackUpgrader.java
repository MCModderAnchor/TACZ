package com.tacz.guns.resource.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Upgrades complete stacks; recipe predicates deliberately use separate adapters. */
public final class LegacyItemStackUpgrader {
    public static final int SOURCE_DATA_VERSION = 3465;
    public static final int TARGET_DATA_VERSION = 3955;
    private final RegistryOps<JsonElement> ops;
    private final RegistryOps<Tag> nbtOps;

    public LegacyItemStackUpgrader(HolderLookup.Provider registries) {
        this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
        this.nbtOps = registries.createSerializationContext(NbtOps.INSTANCE);
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
        CompoundTag upgraded = upgradeTag(legacy, 0);
        JsonObject output = toJson(upgraded);
        ItemStack.CODEC.parse(ops, output).getOrThrow(IllegalArgumentException::new);
        return output;
    }

    public JsonObject upgradeComponents(String itemId, JsonElement nbt) {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", itemId);
        legacy.putByte("Count", (byte) 1);
        legacy.put("tag", readNbt(nbt));
        JsonObject output = toJson(upgradeTag(legacy, 0));
        JsonObject components = output.has("components") ? output.getAsJsonObject("components") : new JsonObject();
        net.minecraft.core.component.DataComponentPatch.CODEC.parse(ops, components).getOrThrow(IllegalArgumentException::new);
        return components;
    }

    private CompoundTag upgradeTag(CompoundTag legacy, int depth) {
        if (depth > 16) throw new IllegalArgumentException("Embedded attachment nesting exceeds 16 levels");
        if (legacy.isEmpty()) return legacy;
        if (!legacy.contains("id", Tag.TAG_STRING)) throw new IllegalArgumentException("Embedded stack is missing id");
        if (legacy.contains("components")) throw new IllegalArgumentException("Expected a 1.20.1 stack, found components");
        if (legacy.contains("ForgeCaps")) throw new IllegalArgumentException("Forge capability data requires a mod-specific adapter");
        int count = legacy.contains("Count") ? legacy.getInt("Count") : 1;
        if (depth > 0 && legacy.getString("id").equals("minecraft:air") && count == 0) return new CompoundTag();
        if (count < 1 || count > 99) throw new IllegalArgumentException("Item count is outside the 1.21.1 codec range 1..99: " + count);
        CompoundTag input = legacy.copy();
        if (input.contains("tag", Tag.TAG_COMPOUND)) {
            CompoundTag tag = input.getCompound("tag");
            for (AttachmentType type : AttachmentType.values()) {
                if (type == AttachmentType.NONE) continue;
                String key = "Attachment" + type.name();
                if (tag.contains(key, Tag.TAG_COMPOUND)) tag.put(key, upgradeTag(tag.getCompound(key), depth + 1));
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
        ItemStack.CODEC.parse(nbtOps, result).getOrThrow(IllegalArgumentException::new);
        return result;
    }

    private JsonObject toJson(CompoundTag stack) {
        // Convert through the typed codecs: raw NbtOps conversion turns NBT booleans into JSON 0/1.
        ItemStack decoded = ItemStack.CODEC.parse(nbtOps, stack).getOrThrow(IllegalArgumentException::new);
        JsonObject result = ItemStack.CODEC.encodeStart(ops, decoded).getOrThrow(IllegalArgumentException::new).getAsJsonObject();
        if (stack.contains("components", Tag.TAG_COMPOUND)) {
            CompoundTag components = stack.getCompound("components");
            for (String key : components.getAllKeys()) {
                var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(key));
                if (type != null && decoded.get(type) instanceof CustomData data && result.has("components")) {
                    result.getAsJsonObject("components").addProperty(key, data.copyTag().toString());
                }
            }
        }
        return result;
    }
}
