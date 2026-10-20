package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunPegboardDyeRecipe;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, GunMod.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, GunMod.MOD_ID);

    public static DeferredHolder<RecipeSerializer<?>, GunSmithTableSerializer> GUN_SMITH_TABLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("gun_smith_table_crafting", GunSmithTableSerializer::new);
    public static DeferredHolder<RecipeType<?>, RecipeType<GunSmithTableRecipe>> GUN_SMITH_TABLE_CRAFTING = RECIPE_TYPES.register("gun_smith_table_crafting", () ->
        RecipeType.simple(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "gun_smith_table_crafting")));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<GunPegboardDyeRecipe>> GUN_PEGBOARD_DYE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("gun_pegboard_dye", () -> new SimpleCraftingRecipeSerializer<>(GunPegboardDyeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoStorageUpgradeRecipe>> AMMO_STORAGE_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("ammo_storage_upgrade", AmmoStorageUpgradeRecipe.Serializer::new);
}
