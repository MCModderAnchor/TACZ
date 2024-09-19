package com.tacz.guns.crafting;

import com.google.gson.JsonObject;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 此类为数据包侧载枪械工作台的实现<br>
 * 枪包的序列化不在此处
 */
public class GunSmithTableSerializer implements RecipeSerializer<GunSmithTableRecipe> {
    @Override
    @Nullable
    public GunSmithTableRecipe fromJson(ResourceLocation id, JsonObject jsonObject) {
        TableRecipe tableRecipe = CommonGunPackLoader.GSON.fromJson(jsonObject, TableRecipe.class);
        if (tableRecipe != null) {
            return new GunSmithTableRecipe(id, tableRecipe);
        }
        return null;
    }

    @Nullable
    @Override
    public GunSmithTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<GunSmithTableIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ingredients.add(new GunSmithTableIngredient(Ingredient.fromNetwork(buffer), buffer.readInt()));
        }
        ItemStack resultItem = buffer.readItem();
        String group = buffer.readUtf();
        GunSmithTableResult result = new GunSmithTableResult(resultItem, group);
        return new GunSmithTableRecipe(recipeId, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, GunSmithTableRecipe recipe) {
        buffer.writeInt(recipe.getInputs().size());
        for(GunSmithTableIngredient ingredient : recipe.getInputs()) {
            ingredient.getIngredient().toNetwork(buffer);
            buffer.writeInt(ingredient.getCount());
        }
        buffer.writeItem(recipe.getResult().getResult());
        buffer.writeUtf(recipe.getResult().getGroup());
    }
}