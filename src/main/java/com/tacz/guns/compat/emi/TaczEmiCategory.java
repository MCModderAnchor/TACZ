package com.tacz.guns.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TaczEmiCategory extends EmiRecipeCategory {
    private final Component name;

    public TaczEmiCategory(ResourceLocation id, EmiStack icon, Component name) {
        super(id, icon);
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
