package com.tacz.guns.crafting;

import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunSmithTableRecipe implements Recipe<Inventory> {
    private final ResourceLocation id;
    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(ResourceLocation id, GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
    }

    public GunSmithTableRecipe(ResourceLocation id, TableRecipe tableRecipe) {
        this(id, tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    @Override
    @Deprecated
    public boolean matches(Inventory playerInventory, Level level) {
        return false;
    }

    @Override
    @Deprecated
    public ItemStack assemble(Inventory playerInventory, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result.getResult().copy();
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipe.GUN_SMITH_TABLE_CRAFTING;
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
}
