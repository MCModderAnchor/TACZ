package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.init.ModCreativeTabs;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class GunAmmoQueryCategory implements IRecipeCategory<GunAmmoQueryEntry> {
    public static final RecipeType<GunAmmoQueryEntry> GUN_AMMO_QUERY = RecipeType.create(GunMod.MOD_ID, "gun_ammo_query", GunAmmoQueryEntry.class);
    private static final Component TITLE = Component.translatable("jei.tacz.gun_ammo_query.title");
    private static final Component AMMO_LABEL = Component.translatable("jei.tacz.gun_ammo_query.ammo");
    private static final int WIDTH = 162;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public GunAmmoQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(WIDTH, 58);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ModCreativeTabs.GUN_RIFLE_TAB.get().getIconItem());
    }

    @Override
    public void draw(GunAmmoQueryEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, AMMO_LABEL, (WIDTH - font.width(AMMO_LABEL)) / 2, 24, 0x555555, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GunAmmoQueryEntry entry, IFocusGroup focuses) {
        // 枪械作为输出，从其 JEI 配方页查询所用弹药。
        builder.addSlot(RecipeIngredientRole.OUTPUT, 73, 1).addItemStack(entry.getGunStack()).setBackground(slotDraw, -1, -1);
        builder.addSlot(RecipeIngredientRole.INPUT, 73, 39).addItemStack(entry.getAmmoStack()).setBackground(slotDraw, -1, -1);
    }

    @Override
    public RecipeType<GunAmmoQueryEntry> getRecipeType() {
        return GUN_AMMO_QUERY;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return bgDraw;
    }

    @Override
    public IDrawable getIcon() {
        return iconDraw;
    }
}
