package com.tacz.guns.compat.jei.transfer;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.tacz.guns.inventory.GunSmithTableMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;
import java.util.Optional;

public class GunSmithTableTransferHandler implements IRecipeTransferHandler<GunSmithTableMenu, RecipeHolder<GunSmithTableRecipe>> {
    private final RecipeType<RecipeHolder<GunSmithTableRecipe>> recipeType;
    private final IRecipeTransferHandlerHelper transferHelper;

    public GunSmithTableTransferHandler(RecipeType<RecipeHolder<GunSmithTableRecipe>> recipeType, IRecipeTransferHandlerHelper transferHelper) {
        this.recipeType = recipeType;
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<GunSmithTableMenu> getContainerClass() {
        return GunSmithTableMenu.class;
    }

    @Override
    public Optional<MenuType<GunSmithTableMenu>> getMenuType() {
        return Optional.of(GunSmithTableMenu.TYPE);
    }

    @Override
    public RecipeType<RecipeHolder<GunSmithTableRecipe>> getRecipeType() {
        return this.recipeType;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(GunSmithTableMenu container, RecipeHolder<GunSmithTableRecipe> recipe, IRecipeSlotsView recipeSlots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        if (!container.canSelectRecipe(recipe.id(), player.level().getRecipeManager())) {
            return this.transferHelper.createUserErrorWithTooltip(Component.translatable("jei.tacz.gun_smith_table.recipe_unavailable"));
        }
        if (doTransfer) {
            // The table crafts from the player's inventory; transferring only selects the recipe.
            // JEI restores the parent screen after this call, which consumes the selection request.
            container.requestRecipeSelection(recipe.id());
        }
        return null;
    }
}
