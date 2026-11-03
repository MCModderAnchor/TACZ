package com.tacz.guns.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class TaczEmiRecipe implements EmiRecipe {
    public enum Layout {
        GUN_SMITH_TABLE,
        ATTACHMENT_QUERY,
        GUN_ATTACHMENT_QUERY,
        GUN_AMMO_QUERY,
        AMMO_QUERY
    }

    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final Layout layout;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final Component header;
    private final Component footer;
    private final EmiIngredient extraInput;
    private final ResourceLocation selectionRecipeId;

    public TaczEmiRecipe(EmiRecipeCategory category, ResourceLocation id, Layout layout,
                         List<EmiIngredient> inputs, List<EmiStack> outputs,
                         Component header, Component footer, EmiIngredient extraInput,
                         ResourceLocation selectionRecipeId) {
        this.category = category;
        this.id = id;
        this.layout = layout;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.header = header;
        this.footer = footer;
        this.extraInput = extraInput;
        this.selectionRecipeId = selectionRecipeId;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        if (extraInput != null && !extraInput.isEmpty()) {
            return java.util.stream.Stream.concat(inputs.stream(), java.util.stream.Stream.of(extraInput)).toList();
        }
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return layout == Layout.GUN_SMITH_TABLE ? 180 : 162;
    }

    @Override
    public int getDisplayHeight() {
        return switch (layout) {
            case GUN_SMITH_TABLE -> 54;
            case ATTACHMENT_QUERY -> 204;
            case GUN_ATTACHMENT_QUERY, AMMO_QUERY -> 156;
            case GUN_AMMO_QUERY -> 58;
        };
    }

    public boolean isGunSmithTableRecipe() {
        return layout == Layout.GUN_SMITH_TABLE && selectionRecipeId != null;
    }

    public ResourceLocation getSelectionRecipeId() {
        return selectionRecipeId;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        switch (layout) {
            case GUN_SMITH_TABLE -> addGunSmithTableWidgets(widgets);
            case ATTACHMENT_QUERY -> addAttachmentQueryWidgets(widgets);
            case GUN_ATTACHMENT_QUERY, AMMO_QUERY -> addGridWidgets(widgets);
            case GUN_AMMO_QUERY -> addGunAmmoWidgets(widgets);
        }
    }

    private void addGunSmithTableWidgets(WidgetHolder widgets) {
        addOutput(widgets, 3, 12);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 30, 12);
        int size = inputs.size();
        if (size < 7) {
            for (int i = 0; i < size; i++) {
                widgets.addSlot(inputs.get(i), 55 + 20 * i, 12);
            }
        } else {
            for (int i = 0; i < size; i++) {
                int row = i < 6 ? 0 : 1;
                int column = i < 6 ? i : i - 6;
                widgets.addSlot(inputs.get(i), 55 + 20 * column, 2 + 20 * row);
            }
        }
    }

    private void addAttachmentQueryWidgets(WidgetHolder widgets) {
        addOutput(widgets, 72, 20);
        addGridInputs(widgets, 0, 56, 9);
        if (extraInput != null && !extraInput.isEmpty()) {
            int lastIndex = inputs.size();
            int column = lastIndex % 9;
            int row = lastIndex / 9;
            widgets.addSlot(extraInput, column * 18, 56 + row * 18);
            widgets.addText(Component.translatable("jei.tacz.attachment_query.more"), 118, 186, 0x555555, false);
        }
    }

    private void addGridWidgets(WidgetHolder widgets) {
        addOutput(widgets, 73, 1);
        if (header != null) {
            widgets.addText(header, 1, 22, 0x555555, false);
        }
        addGridInputs(widgets, 1, 35, 9);
        if (footer != null) {
            widgets.addText(footer, 1, 145, 0x555555, false);
        }
    }

    private void addGunAmmoWidgets(WidgetHolder widgets) {
        addOutput(widgets, 73, 1);
        if (header != null) {
            widgets.addText(header, 1, 24, 0x555555, false);
        }
        if (!inputs.isEmpty()) {
            widgets.addSlot(inputs.getFirst(), 73, 39);
        }
    }

    private void addGridInputs(WidgetHolder widgets, int xOffset, int yOffset, int columns) {
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), xOffset + i % columns * 18, yOffset + i / columns * 18);
        }
    }

    private void addOutput(WidgetHolder widgets, int x, int y) {
        if (!outputs.isEmpty()) {
            widgets.addSlot(outputs.getFirst(), x, y).large(true).recipeContext(this);
        }
    }
}
