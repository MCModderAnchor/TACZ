package com.tacz.guns.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * 单纯的占位符类
 */
public class GunSmithTableSerializer implements RecipeSerializer<GunSmithTableRecipe> {
    @Override
    @Nullable
    public GunSmithTableRecipe fromJson(ResourceLocation id, JsonObject jsonObject) {
        // 不走原版数据包系统，所以这一块直接返回 null
        return null;
    }

    @Nullable
    @Override
    public GunSmithTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        // 不走原版网络包同步系统，所以这一块直接返回 null
        return null;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
        // 不走原版网络包同步系统，所以这一块为空
    }
}
