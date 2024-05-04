package com.tacz.guns.crafting;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunSmithTableSerializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<GunSmithTableRecipe> {
    @Override
    @Nullable
    public GunSmithTableRecipe fromJson(ResourceLocation id, JsonObject jsonObject) {
        // 不走原版数据包系统，所以这一块直接返回 null
        return null;
    }

    @Nullable
    @Override
    public GunSmithTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        GunSmithTableResult result = GunSmithTableResult.fromNetwork(buffer);
        int ingredientSize = buffer.readVarInt();
        List<GunSmithTableIngredient> ingredients = Lists.newArrayList();
        for (int i = 0; i < ingredientSize; i++) {
            ingredients.add(GunSmithTableIngredient.fromNetwork(buffer));
        }
        return new GunSmithTableRecipe(recipeId, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
        GunSmithTableResult.toNetwork(buffer, recipe.getResult());
        List<GunSmithTableIngredient> inputs = recipe.getInputs();
        buffer.writeVarInt(inputs.size());
        for (GunSmithTableIngredient input : inputs) {
            GunSmithTableIngredient.toNetwork(buffer, input);
        }
    }
}
