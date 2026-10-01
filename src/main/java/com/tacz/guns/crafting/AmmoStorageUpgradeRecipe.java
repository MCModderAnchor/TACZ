package com.tacz.guns.crafting;

import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.IShapedRecipe;

public class AmmoStorageUpgradeRecipe extends CustomRecipe implements IShapedRecipe<CraftingContainer> {
    private final Item from;
    private final Item material;
    private final Item to;

    public AmmoStorageUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
        if (id.getPath().contains("diamond")) {
            from = ModItems.AMMO_STORAGE_GOLD.get();
            material = Items.DIAMOND;
            to = ModItems.AMMO_STORAGE_DIAMOND.get();
        } else {
            from = ModItems.AMMO_STORAGE.get();
            material = Items.GOLD_INGOT;
            to = ModItems.AMMO_STORAGE_GOLD.get();
        }
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (slot == 4) {
                if (!stack.is(from)) {
                    return false;
                }
            } else if (!stack.is(material)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = getResultItem(registryAccess);
        CompoundTag blockEntityData = BlockItem.getBlockEntityData(container.getItem(4));
        if (blockEntityData != null) {
            BlockItem.setBlockEntityData(result, com.tacz.guns.init.ModBlocks.AMMO_STORAGE_BE.get(), blockEntityData.copy());
        }
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient materialIngredient = Ingredient.of(material);
        for (int slot = 0; slot < ingredients.size(); slot++) {
            ingredients.set(slot, slot == 4 ? Ingredient.of(from) : materialIngredient);
        }
        return ingredients;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(to);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public int getRecipeWidth() {
        return 3;
    }

    @Override
    public int getRecipeHeight() {
        return 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.AMMO_STORAGE_UPGRADE_RECIPE_SERIALIZER.get();
    }
}
