package com.tacz.guns.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 工作台配方序列化器
 */
public class GunSmithTableSerializer implements RecipeSerializer<GunSmithTableRecipe> {
    @Override
    public MapCodec<GunSmithTableRecipe> codec() {
        return GunSmithTableRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> streamCodec() {
        return GunSmithTableRecipe.STREAM_CODEC;
    }
}
