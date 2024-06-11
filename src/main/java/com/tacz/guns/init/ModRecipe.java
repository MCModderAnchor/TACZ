package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, GunMod.MOD_ID);
    public static RegistryObject<RecipeSerializer<?>> GUN_SMITH_TABLE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("gun_smith_table_crafting", GunSmithTableSerializer::new);
    public static RecipeType<GunSmithTableRecipe> GUN_SMITH_TABLE_CRAFTING;

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        // todo 需要检查实现
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_TYPES)) {
            GUN_SMITH_TABLE_CRAFTING = register(event, GunMod.MOD_ID + ":gun_smith_table_crafting");
        }
    }

    private static <T extends Recipe<?>> RecipeType<T> register(RegisterEvent event, final String key) {
        event.register(ForgeRegistries.Keys.RECIPE_TYPES, new ResourceLocation(key), () -> new RecipeType<T>() {
            @Override
            public String toString() {
                return key;
            }
        });
        return (RecipeType<T>) ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(key));
    }
}