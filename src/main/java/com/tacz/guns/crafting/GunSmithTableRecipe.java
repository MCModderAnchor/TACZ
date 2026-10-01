package com.tacz.guns.crafting;

import com.mojang.serialization.MapCodec;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunSmithTableRecipe implements Recipe<SmithingRecipeInput> {
    public static final MapCodec<GunSmithTableRecipe> CODEC = TableRecipe.CODEC.xmap(
        GunSmithTableRecipe::new,
        recipe -> new TableRecipe(recipe.inputs, recipe.result)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSmithTableRecipe> STREAM_CODEC = TableRecipe.STREAM_CODEC.map(
        GunSmithTableRecipe::new,
        recipe -> new TableRecipe(recipe.inputs, recipe.result)
    );

    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        this.result = result;
        this.inputs = inputs;
    }

    public GunSmithTableRecipe(TableRecipe tableRecipe) {
        this(tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    @Override
    @Deprecated
    public boolean matches(SmithingRecipeInput smithingRecipeInput, Level level) {
        return false;
    }

    @Override
    @Deprecated
    public ItemStack assemble(SmithingRecipeInput smithingRecipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.result.getResult().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipe.GUN_SMITH_TABLE_CRAFTING.get();
    }

    public ItemStack getOutput() {
        return result.getResult();
    }

    public List<GunSmithTableIngredient> getInputs() {
        return inputs;
    }

    public GunSmithTableResult getResult() {
        return result;
    }

    public void init(HolderLookup.Provider provider) {
        result.init(provider);
    }

    public ResourceLocation getTab() {
        return result.getGroup();
    }
}
