package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.AmmoQueryEntry;
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

public class AmmoQueryCategory implements IRecipeCategory<AmmoQueryEntry> {
    public static final RecipeType<AmmoQueryEntry> AMMO_QUERY = RecipeType.create(GunMod.MOD_ID, "ammo_query", AmmoQueryEntry.class);
    private static final Component TITLE = Component.translatable("jei.tacz.ammo_query.title");
    private static final int WIDTH = 162;
    private static final int COLUMNS = 9;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public AmmoQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(WIDTH, 156);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ModCreativeTabs.AMMO_TAB.get().getIconItem());
    }

    @Override
    public void draw(AmmoQueryEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Component gunsLabel = Component.translatable("jei.tacz.ammo_query.guns", entry.getTotalGunCount());
        guiGraphics.drawString(font, gunsLabel, (WIDTH - font.width(gunsLabel)) / 2, 22, 0x555555, false);
        if (entry.getPageCount() > 1) {
            Component page = Component.translatable("jei.tacz.ammo_query.page", entry.getPageNumber(), entry.getPageCount());
            guiGraphics.drawString(font, page, (WIDTH - font.width(page)) / 2, 145, 0x555555, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AmmoQueryEntry entry, IFocusGroup focuses) {
        // 弹药作为输出，从其 JEI 配方页查询全部适用枪械。
        builder.addSlot(RecipeIngredientRole.OUTPUT, 73, 1).addItemStack(entry.getAmmoStack()).setBackground(slotDraw, -1, -1);
        for (int i = 0; i < entry.getGunStacks().size(); i++) {
            int x = 1 + i % COLUMNS * 18;
            int y = 35 + i / COLUMNS * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStack(entry.getGunStacks().get(i)).setBackground(slotDraw, -1, -1);
        }
    }

    @Override
    public RecipeType<AmmoQueryEntry> getRecipeType() {
        return AMMO_QUERY;
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
