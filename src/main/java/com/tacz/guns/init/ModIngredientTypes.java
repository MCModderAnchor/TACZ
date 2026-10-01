package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.NBTIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModIngredientTypes {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, GunMod.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<NBTIngredient>> NBT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("nbt", () -> new IngredientType<>(NBTIngredient.CODEC));
}
