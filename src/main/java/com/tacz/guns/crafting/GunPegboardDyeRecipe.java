package com.tacz.guns.crafting;

import com.tacz.guns.item.GunPegboardItem;
import com.tacz.guns.init.ModRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GunPegboardDyeRecipe extends CustomRecipe {
    public GunPegboardDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        boolean pegboard = false;
        boolean dye = false;
        for (int slot = 0; slot < container.size(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof GunPegboardItem) {
                if (pegboard) return false;
                pegboard = true;
            } else if (stack.getItem() instanceof DyeItem) {
                dye = true;
            } else {
                return false;
            }
        }
        return pegboard && dye;
    }

    @Override
    public ItemStack assemble(CraftingInput container, net.minecraft.core.HolderLookup.Provider registryAccess) {
        ItemStack pegboard = ItemStack.EMPTY;
        List<DyeItem> dyes = new ArrayList<>();
        for (int slot = 0; slot < container.size(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof GunPegboardItem && pegboard.isEmpty()) {
                pegboard = stack;
            } else if (stack.getItem() instanceof DyeItem dye) {
                dyes.add(dye);
            } else {
                return ItemStack.EMPTY;
            }
        }
        return pegboard.isEmpty() || dyes.isEmpty() ? ItemStack.EMPTY : DyedItemColor.applyDyes(pegboard, dyes);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipe.GUN_PEGBOARD_DYE_RECIPE_SERIALIZER.get();
    }
}
