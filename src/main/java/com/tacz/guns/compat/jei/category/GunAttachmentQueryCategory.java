package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.GunAttachmentQueryEntry;
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

import java.util.Locale;

public class GunAttachmentQueryCategory implements IRecipeCategory<GunAttachmentQueryEntry> {
    public static final RecipeType<GunAttachmentQueryEntry> GUN_ATTACHMENT_QUERY = RecipeType.create(GunMod.MOD_ID, "gun_attachment_query", GunAttachmentQueryEntry.class);
    private static final Component TITLE = Component.translatable("jei.tacz.gun_attachment_query.title");
    private static final int WIDTH = 162;
    private static final int COLUMNS = 9;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public GunAttachmentQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(WIDTH, 156);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ModCreativeTabs.GUN_RIFLE_TAB.get().getIconItem());
    }

    @Override
    public void draw(GunAttachmentQueryEntry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Component typeName = Component.translatable("tacz.type." + entry.getAttachmentType().name().toLowerCase(Locale.ROOT) + ".name");
        Component groupTitle = Component.translatable("jei.tacz.gun_attachment_query.group", typeName, entry.getTotalAttachmentCount());
        guiGraphics.drawString(font, groupTitle, (WIDTH - font.width(groupTitle)) / 2, 22, 0x555555, false);
        if (entry.getPageCount() > 1) {
            Component page = Component.translatable("jei.tacz.gun_attachment_query.page", entry.getPageNumber(), entry.getPageCount());
            guiGraphics.drawString(font, page, (WIDTH - font.width(page)) / 2, 145, 0x555555, false);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GunAttachmentQueryEntry entry, IFocusGroup focuses) {
        // 枪械作为输出，可从其 JEI 配方页查看全部兼容配件。
        builder.addSlot(RecipeIngredientRole.OUTPUT, 73, 1).addItemStack(entry.getGunStack()).setBackground(slotDraw, -1, -1);
        for (int i = 0; i < entry.getAttachmentStacks().size(); i++) {
            int x = 1 + i % COLUMNS * 18;
            int y = 35 + i / COLUMNS * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStack(entry.getAttachmentStacks().get(i)).setBackground(slotDraw, -1, -1);
        }
    }

    @Override
    public RecipeType<GunAttachmentQueryEntry> getRecipeType() {
        return GUN_ATTACHMENT_QUERY;
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
