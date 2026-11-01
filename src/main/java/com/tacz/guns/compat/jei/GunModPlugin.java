package com.tacz.guns.compat.jei;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.compat.jei.category.*;
import com.tacz.guns.compat.jei.entry.AmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.AttachmentQueryData;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.compat.jei.transfer.GunSmithTableTransferHandler;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@JeiPlugin
public class GunModPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "jei");

    private Map<ResourceLocation, RecipeType<RecipeHolder<GunSmithTableRecipe>>> recipeTypeMap = new HashMap<>();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        recipeTypeMap.clear();
        var map = TimelessAPI.getAllCommonBlockIndex();
        for (var entry : map) {
            BlockItem item = entry.getValue().getBlock();
            ItemStack icon = BlockItemBuilder.create(item).setId(entry.getKey()).build();
            RecipeType<RecipeHolder<GunSmithTableRecipe>> type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "gun_smith_table/" + entry.getKey().toString().replace(':', '_')));
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
        List<RecipeHolder<GunSmithTableRecipe>> recipes = recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());

        long workbenchStart = System.nanoTime();
        for (var entry : recipeTypeMap.entrySet()) {
            TimelessAPI.getCommonBlockIndex(entry.getKey()).ifPresent(blockIndex -> {
                List<RecipeHolder<GunSmithTableRecipe>> recipeList = blockIndex.getFilter().filter(recipes, RecipeHolder::id).stream().collect(Collectors.toList());
                recipeList.removeIf(recipe -> {
                    return blockIndex.getData().getTabs().stream().noneMatch(tab -> Objects.equals(tab.id(), recipe.value().getResult().getGroup()));
                });
                registration.addRecipes(entry.getValue(), recipeList);
            });
        }

        List<RecipeHolder<CraftingRecipe>> ammoStorageUpgradeRecipes = recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.value() instanceof AmmoStorageUpgradeRecipe)
                .map(GunModPlugin::expandToShapedRecipe)
                .toList();
        registration.addRecipes(RecipeTypes.CRAFTING, ammoStorageUpgradeRecipes);
        long workbenchNanos = System.nanoTime() - workbenchStart;

        long attachmentStart = System.nanoTime();
        AttachmentQueryData attachmentQueries = AttachmentQueryData.load();
        long attachmentBuildNanos = System.nanoTime() - attachmentStart;
        long attachmentRegisterNanos = registerTimed(registration, AttachmentQueryCategory.ATTACHMENT_QUERY, attachmentQueries.attachmentQueries());
        long gunAttachmentRegisterNanos = registerTimed(registration, GunAttachmentQueryCategory.GUN_ATTACHMENT_QUERY, attachmentQueries.gunAttachmentQueries());

        long gunAmmoStart = System.nanoTime();
        List<GunAmmoQueryEntry> gunAmmoEntries = GunAmmoQueryEntry.getAllGunAmmoQueryEntries();
        long gunAmmoBuildNanos = System.nanoTime() - gunAmmoStart;
        long gunAmmoRegisterNanos = registerTimed(registration, GunAmmoQueryCategory.GUN_AMMO_QUERY, gunAmmoEntries);
        long ammoStart = System.nanoTime();
        List<AmmoQueryEntry> ammoEntries = AmmoQueryEntry.getAllAmmoQueryEntries(gunAmmoEntries);
        long ammoBuildNanos = System.nanoTime() - ammoStart;
        long ammoRegisterNanos = registerTimed(registration, AmmoQueryCategory.AMMO_QUERY, ammoEntries);

        GunMod.LOGGER.info("JEI query timings (ms): workbench/crafting={}, attachment build={} ({} guns, {} attachments, {} checks), " +
                        "attachment register={}, gun-attachment register={}, gun-ammo build={}, gun-ammo register={}, ammo build={}, ammo register={}",
                TimeUnit.NANOSECONDS.toMillis(workbenchNanos), TimeUnit.NANOSECONDS.toMillis(attachmentBuildNanos),
                attachmentQueries.gunCount(), attachmentQueries.attachmentCount(), attachmentQueries.compatibilityChecks(),
                TimeUnit.NANOSECONDS.toMillis(attachmentRegisterNanos), TimeUnit.NANOSECONDS.toMillis(gunAttachmentRegisterNanos),
                TimeUnit.NANOSECONDS.toMillis(gunAmmoBuildNanos), TimeUnit.NANOSECONDS.toMillis(gunAmmoRegisterNanos),
                TimeUnit.NANOSECONDS.toMillis(ammoBuildNanos), TimeUnit.NANOSECONDS.toMillis(ammoRegisterNanos));
    }

    private static <T> long registerTimed(IRecipeRegistration registration, RecipeType<T> type, List<T> recipes) {
        long start = System.nanoTime();
        registration.addRecipes(type, recipes);
        return System.nanoTime() - start;
    }

    /**
     * {@link AmmoStorageUpgradeRecipe} 继承自 {@link net.minecraft.world.item.crafting.CustomRecipe}，
     * 其 isSpecial() 返回 true。JEI 无法处理特殊配方（不会读取其布局与产物，导致无法通过物品检索到配方）。
     * 这里参照原版 JEI 内置插件对特殊配方的处理方式，将其展开为一个等价的、JEI 能够识别的 {@link ShapedRecipe}。
     */
    private static RecipeHolder<CraftingRecipe> expandToShapedRecipe(RecipeHolder<CraftingRecipe> holder) {
        AmmoStorageUpgradeRecipe recipe = (AmmoStorageUpgradeRecipe) holder.value();
        RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
        var pattern = new net.minecraft.world.item.crafting.ShapedRecipePattern(recipe.getRecipeWidth(),
                recipe.getRecipeHeight(), recipe.getIngredients(), java.util.Optional.empty());
        return new RecipeHolder<>(holder.id(), new ShapedRecipe("", CraftingBookCategory.MISC, pattern,
                recipe.getResultItem(registryAccess)));
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
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        for (RecipeType<RecipeHolder<GunSmithTableRecipe>> type : recipeTypeMap.values()) {
            registration.addRecipeTransferHandler(new GunSmithTableTransferHandler(type, registration.getTransferHelper()), type);
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
