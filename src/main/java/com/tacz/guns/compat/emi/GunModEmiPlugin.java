package com.tacz.guns.compat.emi;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.compat.jei.entry.AmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.AttachmentQueryData;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.GunAttachmentQueryEntry;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModCreativeTabs;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@EmiEntrypoint
public class GunModEmiPlugin implements EmiPlugin {
    private static final ResourceLocation ATTACHMENT_QUERY = id("attachment_query");
    private static final ResourceLocation GUN_ATTACHMENT_QUERY = id("gun_attachment_query");
    private static final ResourceLocation GUN_AMMO_QUERY = id("gun_ammo_query");
    private static final ResourceLocation AMMO_QUERY = id("ammo_query");

    @Override
    public void register(EmiRegistry registry) {
        Map<ResourceLocation, EmiRecipeCategory> workbenchCategories = registerWorkbenches(registry);
        registerWorkbenchRecipes(registry, workbenchCategories);
        registerAmmoStorageRecipes(registry);
        AttachmentQueryData attachmentQueries = AttachmentQueryData.load();
        registerAttachmentQueries(registry, attachmentQueries.attachmentQueries());
        registerGunAttachmentQueries(registry, attachmentQueries.gunAttachmentQueries());
        registerAmmoQueries(registry);
        registry.addRecipeHandler(GunSmithTableMenu.TYPE, new GunSmithTableEmiRecipeHandler());
    }

    private Map<ResourceLocation, EmiRecipeCategory> registerWorkbenches(EmiRegistry registry) {
        Map<ResourceLocation, EmiRecipeCategory> categories = new HashMap<>();
        for (var entry : TimelessAPI.getAllCommonBlockIndex()) {
            BlockItem block = entry.getValue().getBlock();
            var icon = BlockItemBuilder.create(block).setId(entry.getKey()).build();
            ResourceLocation categoryId = id("gun_smith_table/" + entry.getKey().toString().replace(':', '_'));
            EmiRecipeCategory category = new TaczEmiCategory(categoryId, EmiStack.of(icon), block.getName(icon));
            registry.addCategory(category);
            registry.addWorkstation(category, EmiStack.of(icon));
            categories.put(entry.getKey(), category);
        }
        return categories;
    }

    private void registerWorkbenchRecipes(EmiRegistry registry, Map<ResourceLocation, EmiRecipeCategory> categories) {
        List<RecipeHolder<GunSmithTableRecipe>> recipes = registry.getRecipeManager()
                .getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
        for (var entry : categories.entrySet()) {
            TimelessAPI.getCommonBlockIndex(entry.getKey()).ifPresent(blockIndex -> {
                List<RecipeHolder<GunSmithTableRecipe>> matching = blockIndex.getFilter()
                        .filter(recipes, RecipeHolder::id).stream()
                        .filter(recipe -> blockIndex.getData().getTabs().stream()
                                .anyMatch(tab -> Objects.equals(tab.id(), recipe.value().getResult().getGroup())))
                        .toList();
                for (RecipeHolder<GunSmithTableRecipe> holder : matching) {
                    List<EmiIngredient> inputs = holder.value().getInputs().stream()
                            .map(input -> EmiIngredient.of(input.getIngredient(), input.getCount()))
                            .toList();
                    registry.addRecipe(new TaczEmiRecipe(entry.getValue(),
                            syntheticId("gun_smith_table", entry.getKey() + "/" + holder.id()),
                            TaczEmiRecipe.Layout.GUN_SMITH_TABLE, inputs,
                            List.of(EmiStack.of(holder.value().getOutput())), null, null, null, holder.id()));
                }
            });
        }
    }

    private void registerAmmoStorageRecipes(EmiRegistry registry) {
        for (RecipeHolder<CraftingRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (holder.value() instanceof AmmoStorageUpgradeRecipe recipe) {
                List<EmiIngredient> inputs = recipe.getIngredients().stream()
                        .map(EmiIngredient::of)
                        .toList();
                registry.addRecipe(new EmiCraftingRecipe(inputs,
                        EmiStack.of(recipe.getResultItem(RegistryAccess.EMPTY)),
                        syntheticId("ammo_storage_upgrade", holder.id()), false));
            }
        }
    }

    private void registerAttachmentQueries(EmiRegistry registry, List<AttachmentQueryEntry> entries) {
        EmiRecipeCategory category = queryCategory(registry, ATTACHMENT_QUERY,
                Component.translatable("jei.tacz.attachment_query.title"),
                ModCreativeTabs.ATTACHMENT_SCOPE_TAB.get().getIconItem());
        for (int i = 0; i < entries.size(); i++) {
            AttachmentQueryEntry entry = entries.get(i);
            EmiIngredient extraInput = entry.getExtraAllowGunStacks().isEmpty() ? null
                    : EmiIngredient.of(entry.getExtraAllowGunStacks().stream().map(EmiStack::of).toList());
            registry.addRecipe(new TaczEmiRecipe(category, syntheticId("attachment_query", Integer.toString(i)),
                    TaczEmiRecipe.Layout.ATTACHMENT_QUERY,
                    entry.getAllowGunStacks().stream().map(EmiStack::of)
                            .map(ingredient -> (EmiIngredient) ingredient).toList(),
                    List.of(EmiStack.of(entry.getAttachmentStack())), null, null, extraInput, null));
        }
    }

    private void registerGunAttachmentQueries(EmiRegistry registry, List<GunAttachmentQueryEntry> entries) {
        EmiRecipeCategory category = queryCategory(registry, GUN_ATTACHMENT_QUERY,
                Component.translatable("jei.tacz.gun_attachment_query.title"),
                ModCreativeTabs.GUN_RIFLE_TAB.get().getIconItem());
        for (int i = 0; i < entries.size(); i++) {
            GunAttachmentQueryEntry entry = entries.get(i);
            Component typeName = Component.translatable("tacz.type."
                    + entry.getAttachmentType().name().toLowerCase(Locale.ROOT) + ".name");
            Component header = Component.translatable("jei.tacz.gun_attachment_query.group",
                    typeName, entry.getTotalAttachmentCount());
            Component footer = pageText("jei.tacz.gun_attachment_query.page",
                    entry.getPageNumber(), entry.getPageCount());
            registry.addRecipe(new TaczEmiRecipe(category, syntheticId("gun_attachment_query", Integer.toString(i)),
                    TaczEmiRecipe.Layout.GUN_ATTACHMENT_QUERY,
                    entry.getAttachmentStacks().stream().map(EmiStack::of)
                            .map(ingredient -> (EmiIngredient) ingredient).toList(),
                    List.of(EmiStack.of(entry.getGunStack())), header, footer, null, null));
        }
    }

    private void registerAmmoQueries(EmiRegistry registry) {
        EmiRecipeCategory gunAmmoCategory = queryCategory(registry, GUN_AMMO_QUERY,
                Component.translatable("jei.tacz.gun_ammo_query.title"),
                ModCreativeTabs.GUN_RIFLE_TAB.get().getIconItem());
        EmiRecipeCategory ammoCategory = queryCategory(registry, AMMO_QUERY,
                Component.translatable("jei.tacz.ammo_query.title"),
                ModCreativeTabs.AMMO_TAB.get().getIconItem());
        List<GunAmmoQueryEntry> gunAmmoEntries = GunAmmoQueryEntry.getAllGunAmmoQueryEntries();
        for (int i = 0; i < gunAmmoEntries.size(); i++) {
            GunAmmoQueryEntry entry = gunAmmoEntries.get(i);
            registry.addRecipe(new TaczEmiRecipe(gunAmmoCategory, syntheticId("gun_ammo_query", Integer.toString(i)),
                    TaczEmiRecipe.Layout.GUN_AMMO_QUERY,
                    List.of(EmiStack.of(entry.getAmmoStack())), List.of(EmiStack.of(entry.getGunStack())),
                    Component.translatable("jei.tacz.gun_ammo_query.ammo"), null, null, null));
        }
        List<AmmoQueryEntry> ammoEntries = AmmoQueryEntry.getAllAmmoQueryEntries(gunAmmoEntries);
        for (int i = 0; i < ammoEntries.size(); i++) {
            AmmoQueryEntry entry = ammoEntries.get(i);
            Component header = Component.translatable("jei.tacz.ammo_query.guns", entry.getTotalGunCount());
            Component footer = pageText("jei.tacz.ammo_query.page", entry.getPageNumber(), entry.getPageCount());
            registry.addRecipe(new TaczEmiRecipe(ammoCategory, syntheticId("ammo_query", Integer.toString(i)),
                    TaczEmiRecipe.Layout.AMMO_QUERY,
                    entry.getGunStacks().stream().map(EmiStack::of)
                            .map(ingredient -> (EmiIngredient) ingredient).toList(),
                    List.of(EmiStack.of(entry.getAmmoStack())), header, footer, null, null));
        }
    }

    private EmiRecipeCategory queryCategory(EmiRegistry registry, ResourceLocation id, Component title,
                                            ItemStack icon) {
        EmiRecipeCategory category = new TaczEmiCategory(id, EmiStack.of(icon), title);
        registry.addCategory(category);
        return category;
    }

    private static Component pageText(String key, int page, int pageCount) {
        return pageCount > 1 ? Component.translatable(key, page, pageCount) : null;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, path);
    }

    private static ResourceLocation syntheticId(String category, Object key) {
        return id("/emi/" + category + "/" + key.toString().replace(':', '_'));
    }
}
