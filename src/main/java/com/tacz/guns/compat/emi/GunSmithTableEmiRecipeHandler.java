package com.tacz.guns.compat.emi;

import com.tacz.guns.inventory.GunSmithTableMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GunSmithTableEmiRecipeHandler implements EmiRecipeHandler<GunSmithTableMenu> {
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<GunSmithTableMenu> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? new EmiPlayerInventory(List.of()) : new EmiPlayerInventory(minecraft.player);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof TaczEmiRecipe taczRecipe && taczRecipe.isGunSmithTableRecipe();
    }

    @Override
    public boolean alwaysDisplaySupport(EmiRecipe recipe) {
        return false;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<GunSmithTableMenu> context) {
        Minecraft minecraft = Minecraft.getInstance();
        return recipe instanceof TaczEmiRecipe taczRecipe
                && minecraft.level != null
                && context.getScreenHandler().canSelectRecipe(taczRecipe.getSelectionRecipeId(), minecraft.level.getRecipeManager());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<GunSmithTableMenu> context) {
        return canCraft(recipe, context) ? List.of()
                : List.of(ClientTooltipComponent.create(Component.translatable("jei.tacz.gun_smith_table.recipe_unavailable")
                .getVisualOrderText()));
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<GunSmithTableMenu> context) {
        if (!(recipe instanceof TaczEmiRecipe taczRecipe) || !canCraft(recipe, context)) {
            return false;
        }
        context.getScreenHandler().requestRecipeSelection(taczRecipe.getSelectionRecipeId());
        Minecraft.getInstance().setScreen(context.getScreen());
        return true;
    }
}
