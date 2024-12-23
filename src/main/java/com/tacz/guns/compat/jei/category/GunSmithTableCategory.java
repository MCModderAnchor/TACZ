package com.tacz.guns.compat.jei.category;

import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GunSmithTableCategory implements IRecipeCategory<GunSmithTableRecipe> {
    private final Component title;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;
    private final RecipeType<GunSmithTableRecipe> type;

    public GunSmithTableCategory(IGuiHelper guiHelper, ItemStack icon, RecipeType<GunSmithTableRecipe> type, Component title) {
        this.bgDraw = guiHelper.createBlankDrawable(160, 40);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);
        this.type = type;
        this.title = title;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GunSmithTableRecipe recipe, IFocusGroup focuses) {
        ItemStack output = recipe.getOutput();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 3, 12).addItemStack(output).setBackground(slotDraw, -1, -1);

        List<GunSmithTableIngredient> inputs = recipe.getInputs();
        int size = inputs.size();
        // 单行排布
        if (size < 7) {
            for (int i = 0; i < size; i++) {
                int xOffset = 35 + 20 * i;
                int yOffset = 12;
                builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset).addItemStacks(getInput(inputs, i)).setBackground(slotDraw, -1, -1);
            }
        }
        // 双行排布
        else {
            for (int i = 0; i < 6; i++) {
                int xOffset = 35 + 20 * i;
                int yOffset = 2;
                builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset).addItemStacks(getInput(inputs, i)).setBackground(slotDraw, -1, -1);
            }
            for (int i = 6; i < size; i++) {
                int xOffset = 35 + 20 * (i - 6);
                int yOffset = 22;
                builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset).addItemStacks(getInput(inputs, i)).setBackground(slotDraw, -1, -1);
            }
        }
    }

    private List<ItemStack> getInput(List<GunSmithTableIngredient> inputs, int index) {
        if (index < inputs.size()) {
            GunSmithTableIngredient ingredient = inputs.get(index);
            ItemStack[] items = ingredient.getIngredient().getItems();
            Arrays.stream(items).forEach(stack -> stack.setCount(ingredient.getCount()));
            return List.of(items);
        }
        return Collections.singletonList(ItemStack.EMPTY);
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return bgDraw;
    }

    @Override
    public IDrawable getIcon() {
        return iconDraw;
    }

    @Override
    public RecipeType<GunSmithTableRecipe> getRecipeType() {
        return type;
    }
}
