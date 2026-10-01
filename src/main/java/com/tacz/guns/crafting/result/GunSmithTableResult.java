package com.tacz.guns.crafting.result;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunSmithTableResult {
    public static final Codec<GunSmithTableResult> CODEC = ExtraCodecs.catchDecoderException(Codec.of(GunSmithTableResult::encode, GunSmithTableResult::decode));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableResult> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, GunSmithTableResult::getResult,
        ResourceLocation.STREAM_CODEC, GunSmithTableResult::getGroup,
        GunSmithTableResult::new
    );

    public static <T> DataResult<T> encode(GunSmithTableResult input, DynamicOps<T> ops, T prefix) {
        throw new UnsupportedOperationException();
    }

    public static <T> DataResult<Pair<GunSmithTableResult, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).map(map -> {
            String typeName = Codec.STRING.fieldOf("type").decode(ops, map).getOrThrow();
            int count = Codec.INT.optionalFieldOf("count", 1).decode(ops, map).getOrThrow();
            CompoundTag extraTag = CompoundTag.CODEC.optionalFieldOf("nbt", null).decode(ops, map).getOrThrow();
            ResourceLocation tabOverride = Codec.STRING.optionalFieldOf("group").decode(ops, map).getOrThrow()
                .map(raw -> raw.contains(":") ? raw : GunMod.MOD_ID + ":" + raw)
                .map(ResourceLocation::tryParse).orElse(null);

            GunSmithTableResult result;
            switch (typeName) {
                case GunSmithTableResult.GUN,
                     GunSmithTableResult.AMMO,
                     GunSmithTableResult.ATTACHMENT -> {
                    ResourceLocation id = ResourceLocation.CODEC.fieldOf("id").decode(ops, map).getOrThrow();
                    RawGunTableResult raw = new RawGunTableResult(typeName, id, count);
                    if (extraTag != null) raw.setNbt(extraTag);
                    if (typeName.equals(GunSmithTableResult.GUN)) {
                        GunResult.CODEC.parse(ops, input).result().ifPresent(raw::setExtraData);
                    }
                    result = new GunSmithTableResult(raw, tabOverride);
                }
                case GunSmithTableResult.CUSTOM -> {
                    ItemStack itemStack = ItemStack.OPTIONAL_CODEC.fieldOf("item").decode(ops, map).getOrThrow();
                    result = new GunSmithTableResult(itemStack, tabOverride);
                    if (extraTag != null) {
                        result.getResult().update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> data.update(tag -> {
                            for (String key : extraTag.getAllKeys()) {
                                Tag value = extraTag.get(key);
                                if (value != null) {
                                    tag.put(key, value);
                                }
                            }
                        }));
                    }
                }
                default -> result = new GunSmithTableResult(ItemStack.EMPTY, TabConfig.TAB_EMPTY);
            }
            return Pair.of(result, input);
        });
    }

    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";
    public static final String CUSTOM = "custom";

    private ItemStack result = ItemStack.EMPTY;
    private ResourceLocation group = null;

    @Nullable
    private RawGunTableResult raw = null;

    public GunSmithTableResult(ItemStack result, @Nullable ResourceLocation group) {
        this.result = result;
        this.group = group==null ? TabConfig.TAB_EMPTY : group;
    }


    public GunSmithTableResult(@NotNull RawGunTableResult raw) {
        this.raw = raw;
    }

    public GunSmithTableResult(@NotNull RawGunTableResult raw, @Nullable ResourceLocation group) {
        this.raw = raw;
        this.group = group==null ? TabConfig.TAB_EMPTY : group;
    }

    public void init(HolderLookup.Provider provider) {
        if (raw != null) {
            GunSmithTableResult result = RawGunTableResult.init(provider, raw);
            this.result = result.getResult();
            if (group == null || group.equals(TabConfig.TAB_EMPTY)) {
                this.group = result.getGroup();
            }
            this.raw = null;
        }
    }

    public ItemStack getResult() {
        return result;
    }

    public ResourceLocation getGroup() {
        return group;
    }
}
