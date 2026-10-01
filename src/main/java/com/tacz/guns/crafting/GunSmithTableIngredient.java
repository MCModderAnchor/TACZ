package com.tacz.guns.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public class GunSmithTableIngredient {
    public static final Codec<GunSmithTableIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Ingredient.CODEC.fieldOf("item").forGetter(GunSmithTableIngredient::getIngredient),
        Codec.INT.optionalFieldOf("count", 1).forGetter(GunSmithTableIngredient::getCount)
    ).apply(instance, GunSmithTableIngredient::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableIngredient> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, GunSmithTableIngredient::getIngredient,
        ByteBufCodecs.INT, GunSmithTableIngredient::getCount,
        GunSmithTableIngredient::new
    );

    private final Ingredient ingredient;
    private final int count;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }
}
