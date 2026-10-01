package com.tacz.guns.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.init.ModIngredientTypes;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class NBTIngredient implements ICustomIngredient {
    public static final MapCodec<NBTIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
        HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false).fieldOf("items").forGetter(NBTIngredient::items),
        TagParser.LENIENT_CODEC.fieldOf("nbt").forGetter(NBTIngredient::nbt),
        Codec.BOOL.optionalFieldOf("partial", true).forGetter(NBTIngredient::partial)
    ).apply(builder, NBTIngredient::new));

    private final HolderSet<Item> items;
    private final CompoundTag nbt;
    private final NbtPredicate predicate;
    private final boolean partial;
    private final ItemStack[] stacks;

    public NBTIngredient(HolderSet<Item> items, CompoundTag nbt, boolean partial) {
        this.items = items;
        this.nbt = nbt;
        this.predicate = new NbtPredicate(nbt);
        this.partial = partial;
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
            .build();
        this.stacks = items.stream()
            .map(i -> new ItemStack(i, 1, patch))
            .toArray(ItemStack[]::new);
    }

    @Override
    public boolean test(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!this.items.contains(itemStack.getItemHolder())) return false;
        CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt = data != null ? data.copyTag() : null;
        if (this.partial) {
            return predicate.matches(nbt);
        } else {
            return Objects.equals(nbt, this.nbt);
        }
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(this.stacks);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModIngredientTypes.NBT_INGREDIENT_TYPE.get();
    }

    public HolderSet<Item> items() {
        return items;
    }

    public CompoundTag nbt() {
        return nbt;
    }

    public boolean partial() {
        return partial;
    }
}
