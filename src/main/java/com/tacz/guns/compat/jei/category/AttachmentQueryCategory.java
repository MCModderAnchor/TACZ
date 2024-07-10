package com.tacz.guns.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AttachmentQueryCategory implements IRecipeCategory<AttachmentQueryEntry> {
    public static final RecipeType<AttachmentQueryEntry> ATTACHMENT_QUERY = RecipeType.create(GunMod.MOD_ID, "attachment_query", AttachmentQueryEntry.class);
    public static final int MAX_GUN_SHOW_COUNT = 60;
    private static final Component TITLE = new TranslatableComponent("jei.tacz.attachment_query.title");
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public AttachmentQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(160, 145);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "sight_sro_dot")).build());
    }

    @Override
    public void draw(AttachmentQueryEntry entry, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        List<ItemStack> extraAllowGunStacks = entry.getExtraAllowGunStacks();
        if (!extraAllowGunStacks.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            font.draw(poseStack, new TranslatableComponent("jei.tacz.attachment_query.more"), 128, 134, 0x555555);
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AttachmentQueryEntry entry, IFocusGroup focuses) {
        ItemStack attachmentStack = entry.getAttachmentStack();
        List<ItemStack> allowGunStacks = entry.getAllowGunStacks();
        List<ItemStack> extraAllowGunStacks = entry.getExtraAllowGunStacks();

        // 先把配件放在正中央
        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 0).addItemStack(attachmentStack).setBackground(slotDraw, -1, -1);

        // 逐行画枪械，每行 9 个
        int xOffset = 0;
        int yOffset = 20;
        for (int i = 0; i < allowGunStacks.size(); i++) {
            int column = i % 9;
            int row = i / 9;
            xOffset = column * 18;
            yOffset = 20 + row * 18;
            ItemStack gun = allowGunStacks.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset).addItemStack(gun).setBackground(slotDraw, -1, -1);
        }

        // 如果超出上限，那么最后一格则为来回跳变的物品
        if (!extraAllowGunStacks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, xOffset + 18, yOffset).addItemStacks(extraAllowGunStacks).setBackground(slotDraw, -1, -1);
        }
    }

    @Override
    public RecipeType<AttachmentQueryEntry> getRecipeType() {
        return ATTACHMENT_QUERY;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return bgDraw;
    }

    @Override
    public IDrawable getIcon() {
        return iconDraw;
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getUid() {
        return ATTACHMENT_QUERY.getUid();
    }

    @Override
    @SuppressWarnings("removal")
    public Class<? extends AttachmentQueryEntry> getRecipeClass() {
        return ATTACHMENT_QUERY.getRecipeClass();
    }
}
