package com.tacz.guns.compat.jei;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.compat.jei.category.*;
import com.tacz.guns.compat.jei.entry.AmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.GunAttachmentQueryEntry;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JeiPlugin
public class GunModPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(GunMod.MOD_ID, "jei");

    private Map<ResourceLocation, RecipeType<GunSmithTableRecipe>> recipeTypeMap = new HashMap<>();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        recipeTypeMap.clear();
        var map = TimelessAPI.getAllCommonBlockIndex();
        for (var entry : map) {
            BlockItem item = entry.getValue().getBlock();
            ItemStack icon = BlockItemBuilder.create(item).setId(entry.getKey()).build();
            RecipeType<GunSmithTableRecipe> type = RecipeType.create(GunMod.MOD_ID, "gun_smith_table/" + entry.getKey().toString().replace(':', '_'), GunSmithTableRecipe.class);
            registration.addRecipeCategories(new GunSmithTableCategory(registration.getJeiHelpers().getGuiHelper(), icon, type, item.getName(icon)));
            recipeTypeMap.put(entry.getKey(), type);
        }
        registration.addRecipeCategories(new AttachmentQueryCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new GunAttachmentQueryCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new GunAmmoQueryCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AmmoQueryCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if(Minecraft.getInstance().level==null) return;
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<GunSmithTableRecipe> recipes = recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());

        for (var entry : recipeTypeMap.entrySet()) {
            TimelessAPI.getCommonBlockIndex(entry.getKey()).ifPresent(blockIndex -> {
                List<GunSmithTableRecipe> recipeList = blockIndex.getFilter().filter(recipes, GunSmithTableRecipe::getId);
                recipeList.removeIf(recipe -> {
                    return blockIndex.getData().getTabs().stream().noneMatch(tab -> Objects.equals(tab.id(), recipe.getResult().getGroup()));
                });
                registration.addRecipes(entry.getValue(), recipeList);
            });
        }

        List<CraftingRecipe> ammoStorageUpgradeRecipes = recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .filter(AmmoStorageUpgradeRecipe.class::isInstance)
                .map(recipe -> expandToShapedRecipe((AmmoStorageUpgradeRecipe) recipe))
                .toList();
        registration.addRecipes(RecipeTypes.CRAFTING, ammoStorageUpgradeRecipes);

        registration.addRecipes(AttachmentQueryCategory.ATTACHMENT_QUERY, AttachmentQueryEntry.getAllAttachmentQueryEntries());
        registration.addRecipes(GunAttachmentQueryCategory.GUN_ATTACHMENT_QUERY, GunAttachmentQueryEntry.getAllGunAttachmentQueryEntries());
        List<GunAmmoQueryEntry> gunAmmoEntries = GunAmmoQueryEntry.getAllGunAmmoQueryEntries();
        registration.addRecipes(GunAmmoQueryCategory.GUN_AMMO_QUERY, gunAmmoEntries);
        registration.addRecipes(AmmoQueryCategory.AMMO_QUERY, AmmoQueryEntry.getAllAmmoQueryEntries(gunAmmoEntries));
    }

    /**
     * {@link AmmoStorageUpgradeRecipe} 继承自 {@link net.minecraft.world.item.crafting.CustomRecipe}，
     * 其 isSpecial() 返回 true。JEI 无法处理特殊配方（不会读取其布局与产物，导致无法通过物品检索到配方）。
     * 这里参照原版 JEI 内置插件对特殊配方的处理方式，将其展开为一个等价的、JEI 能够识别的 {@link ShapedRecipe}。
     */
    private static CraftingRecipe expandToShapedRecipe(AmmoStorageUpgradeRecipe recipe) {
        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        return new ShapedRecipe(
                recipe.getId(),
                "",
                CraftingBookCategory.MISC,
                recipe.getRecipeWidth(),
                recipe.getRecipeHeight(),
                recipe.getIngredients(),
                recipe.getResultItem(registryAccess)
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var entry : recipeTypeMap.entrySet()) {
            TimelessAPI.getCommonBlockIndex(entry.getKey()).ifPresent(blockIndex -> {
                ItemStack stack = BlockItemBuilder.create(blockIndex.getBlock()).setId(entry.getKey()).build();
                registration.addRecipeCatalyst(stack, entry.getValue());
            });

        }

    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.AMMO.get(), GunModSubtype.getAmmoSubtype());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.ATTACHMENT.get(), GunModSubtype.getAttachmentSubtype());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.AMMO_BOX.get(), GunModSubtype.getAmmoBoxSubtype());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.WORKBENCH_111.get(), GunModSubtype.getTableSubType());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.WORKBENCH_121.get(), GunModSubtype.getTableSubType());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, ModItems.WORKBENCH_211.get(), GunModSubtype.getTableSubType());
        GunItemManager.getAllGunItems().forEach(item -> registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item.get(), GunModSubtype.getGunSubtype()));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
}
