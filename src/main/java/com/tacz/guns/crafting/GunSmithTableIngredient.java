package com.tacz.guns.crafting;

import net.minecraft.world.item.crafting.Ingredient;

public class GunSmithTableIngredient {
    private final Ingredient ingredient;
    private final int count;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }
}
