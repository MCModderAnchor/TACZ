package com.tac.guns.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.client.gui.components.ResultButton;
import com.tac.guns.client.gui.components.TypeButton;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.CustomTabPOJO;
import com.tac.guns.crafting.GunSmithTableIngredient;
import com.tac.guns.crafting.GunSmithTableRecipe;
import com.tac.guns.crafting.GunSmithTableResult;
import com.tac.guns.inventory.GunSmithTableMenu;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ClientMessageCraft;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class GunSmithTableScreen extends AbstractContainerScreen<GunSmithTableMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private static final ResourceLocation SIDE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table_side.png");

    private final List<String> recipeKeys = Lists.newArrayList();
    private final Map<String, List<ResourceLocation>> recipes = Maps.newHashMap();

    private int typePage;
    private String selectedType;
    private List<ResourceLocation> selectedRecipeList;

    private int indexPage;
    private @Nullable GunSmithTableRecipe selectedRecipe;
    private @Nullable Int2IntArrayMap playerIngredientCount;

    private int scale = 70;

    public GunSmithTableScreen(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 344;
        this.imageHeight = 186;
        this.classifyRecipes();

        this.typePage = 0;
        this.selectedType = GunSmithTableResult.AMMO;
        this.selectedRecipeList = recipes.get(selectedType);

        this.indexPage = 0;
        this.selectedRecipe = this.getSelectedRecipe(this.selectedRecipeList.get(0));
        this.getPlayerIngredientCount(this.selectedRecipe);
    }

    private void classifyRecipes() {
        TimelessAPI.getAllRecipes().forEach((id, recipe) -> {
            GunSmithTableResult result = recipe.getResult();
            String group = result.getGroup();
            if (!recipeKeys.contains(group)) {
                recipeKeys.add(group);
            }
            recipes.putIfAbsent(group, Lists.newArrayList());
            recipes.get(group).add(id);
        });
    }

    private @Nullable GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId) {
        return TimelessAPI.getAllRecipes().get(recipeId);
    }

    private void getPlayerIngredientCount(GunSmithTableRecipe recipe) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<GunSmithTableIngredient> ingredients = recipe.getInputs();
        int size = ingredients.size();
        this.playerIngredientCount = new Int2IntArrayMap(size);
        for (int i = 0; i < size; i++) {
            GunSmithTableIngredient ingredient = ingredients.get(i);
            Inventory inventory = player.getInventory();
            int count = 0;
            for (ItemStack stack : inventory.items) {
                if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                    count = count + stack.getCount();
                }
            }
            playerIngredientCount.put(i, count);
        }
    }

    public void updateIngredientCount() {
        if (this.selectedRecipe != null) {
            this.getPlayerIngredientCount(selectedRecipe);
        }
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.addTypePageButtons();
        this.addTypeButtons();
        this.addIndexPageButtons();
        this.addIndexButtons();
        this.addScaleButtons();
        this.addCraftButton();
    }

    private void addCraftButton() {
        this.addRenderableWidget(new ImageButton(leftPos + 289, topPos + 162, 48, 18, 138, 164, 18, TEXTURE, b -> {
            if (this.selectedRecipe != null && playerIngredientCount != null) {
                // 检查是否能合成，不能就不发包
                List<GunSmithTableIngredient> inputs = selectedRecipe.getInputs();
                int size = inputs.size();
                for (int i = 0; i < size; i++) {
                    if (i >= playerIngredientCount.size()) {
                        return;
                    }
                    int hasCount = playerIngredientCount.get(i);
                    int needCount = inputs.get(i).getCount();
                    // 拥有数量小于需求数量，不发包
                    if (hasCount < needCount) {
                        return;
                    }
                }
                NetworkHandler.CHANNEL.sendToServer(new ClientMessageCraft(this.selectedRecipe.getId(), this.menu.containerId));
            }
        }));
    }

    private void addIndexButtons() {
        if (selectedRecipeList == null || selectedRecipeList.isEmpty()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            int finalIndex = i + indexPage * 6;
            if (finalIndex >= selectedRecipeList.size()) {
                break;
            }
            int yOffset = topPos + 66 + 17 * i;
            TimelessAPI.getRecipe(selectedRecipeList.get(finalIndex)).ifPresent(recipe -> {
                ResultButton button = addRenderableWidget(new ResultButton(leftPos + 144, yOffset, recipe.getOutput(), b -> {
                    this.selectedRecipe = recipe;
                    this.getPlayerIngredientCount(this.selectedRecipe);
                    this.init();
                }));
                if (this.selectedRecipe != null && recipe.getId().equals(this.selectedRecipe.getId())) {
                    button.setSelected(true);
                }
            });
        }
    }

    private void addTypeButtons() {
        for (int i = 0; i < 7; i++) {
            int typeIndex = typePage * 7 + i;
            if (typeIndex >= recipes.size()) {
                return;
            }
            String type = recipeKeys.get(typeIndex);
            int xOffset = leftPos + 157 + 24 * i;
            List<ResourceLocation> recipeIdGroups = recipes.get(type);
            if (recipeIdGroups.isEmpty()) {
                continue;
            }
            CustomTabPOJO tabPOJO = ClientAssetManager.INSTANCE.getAllCustomTabs().get(type);
            ItemStack icon = ItemStack.EMPTY;
            if (tabPOJO != null) {
                icon = tabPOJO.getIconStack();
            } else if (GunSmithTableResult.AMMO.equals(type)) {
                icon = AmmoItemBuilder.create().build();
            } else if (GunSmithTableResult.ATTACHMENT.equals(type)) {
                icon = AttachmentItemBuilder.create().build();
            }
            TypeButton typeButton = new TypeButton(xOffset, topPos + 2, icon, b -> {
                this.selectedType = type;
                this.selectedRecipeList = recipes.get(type);
                this.indexPage = 0;
                this.selectedRecipe = getSelectedRecipe(this.selectedRecipeList.get(0));
                this.getPlayerIngredientCount(this.selectedRecipe);
                this.init();
            });
            if (this.selectedType.equals(type)) {
                typeButton.setSelected(true);
            }
            this.addRenderableWidget(typeButton);
        }
    }

    private void addIndexPageButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 143, topPos + 56, 96, 6, 40, 166, 6, TEXTURE, b -> {
            if (this.indexPage > 0) {
                this.indexPage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 143, topPos + 171, 96, 6, 40, 186, 6, TEXTURE, b -> {
            if (selectedRecipeList != null && !selectedRecipeList.isEmpty()) {
                int maxIndexPage = (selectedRecipeList.size() - 1) / 6;
                if (this.indexPage < maxIndexPage) {
                    this.indexPage++;
                    this.init();
                }
            }
        }));
    }

    private void addTypePageButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 136, topPos + 4, 18, 20, 0, 162, 20, TEXTURE, b -> {
            if (this.typePage > 0) {
                this.typePage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 327, topPos + 4, 18, 20, 20, 162, 20, TEXTURE, b -> {
            int maxIndexPage = (recipes.size() - 1) / 7;
            if (this.typePage < maxIndexPage) {
                this.typePage++;
                this.init();
            }
        }));
    }

    private void addScaleButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 5, topPos + 5, 10, 10, 188, 173, 10, TEXTURE, b -> {
            this.scale = Math.min(this.scale + 20, 200);
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 17, topPos + 5, 10, 10, 200, 173, 10, TEXTURE, b -> {
            this.scale = Math.max(this.scale - 20, 10);
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 29, topPos + 5, 10, 10, 212, 173, 10, TEXTURE, b -> {
            this.scale = 70;
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawModCenteredString(poseStack, font, new TranslatableComponent("gui.tac.gun_smith_table.preview"), leftPos + 108, topPos + 5, 0x555555);
        font.draw(poseStack, new TranslatableComponent(String.format("tac.type.%s.name", selectedType)), leftPos + 150, topPos + 32, 0x555555);
        font.draw(poseStack, new TranslatableComponent("gui.tac.gun_smith_table.ingredient"), leftPos + 254, topPos + 50, 0x555555);
        drawModCenteredString(poseStack, font, new TranslatableComponent("gui.tac.gun_smith_table.craft"), leftPos + 312, topPos + 167, 0xFFFFFF);
        if (this.selectedRecipe != null) {
            this.renderLeftModel(this.selectedRecipe);
        }
        if (selectedRecipeList != null && !selectedRecipeList.isEmpty()) {
            renderIngredient(poseStack);
        }
        this.renderables.stream().filter(w -> w instanceof ResultButton)
                .forEach(w -> ((ResultButton) w).renderTooltips(stack -> this.renderTooltip(poseStack, stack, mouseX, mouseY)));
    }

    private void renderIngredient(PoseStack poseStack) {
        if (this.selectedRecipe == null) {
            return;
        }
        List<GunSmithTableIngredient> inputs = this.selectedRecipe.getInputs();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                int index = i * 2 + j;
                if (index >= inputs.size()) {
                    return;
                }
                int offsetX = leftPos + 254 + 45 * j;
                int offsetY = topPos + 62 + 17 * i;

                GunSmithTableIngredient smithTableIngredient = inputs.get(index);
                Ingredient ingredient = smithTableIngredient.getIngredient();

                ItemStack[] items = ingredient.getItems();
                int itemIndex = ((int) (System.currentTimeMillis() / 1_000)) % items.length;
                ItemStack item = items[itemIndex];
                this.itemRenderer.renderAndDecorateFakeItem(item, offsetX, offsetY);

                poseStack.pushPose();
                poseStack.translate(0, 0, 200);
                poseStack.scale(0.5f, 0.5f, 1);

                int count = smithTableIngredient.getCount();
                int hasCount = 0;
                if (playerIngredientCount != null && index < playerIngredientCount.size()) {
                    hasCount = playerIngredientCount.get(index);
                }
                int color = count <= hasCount ? 0xFFFFFF : 0xFF0000;
                font.draw(poseStack, String.format("%d/%d", count, hasCount), (offsetX + 17) * 2, (offsetY + 10) * 2, color);
                poseStack.popPose();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void renderLeftModel(GunSmithTableRecipe recipe) {
        float rotationPeriod = 8f;
        int xPos = leftPos + 60;
        int yPos = topPos + 50;
        int startX = leftPos + 3;
        int startY = topPos + 16;
        int width = 128;
        int height = 99;
        float rotPitch = 15;

        Window window = Minecraft.getInstance().getWindow();
        double windowGuiScale = window.getGuiScale();
        int scissorX = (int) (startX * windowGuiScale);
        int scissorY = (int) (window.getHeight() - ((startY + height) * windowGuiScale));
        int scissorW = (int) (width * windowGuiScale);
        int scissorH = (int) (height * windowGuiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate(xPos, yPos, -50);
        posestack.translate(8.0D, 8.0D, 0.0D);
        posestack.scale(1.0F, -1.0F, 1.0F);
        posestack.scale(scale, scale, scale);
        float rot = (System.currentTimeMillis() % (int) (rotationPeriod * 1000)) * (360f / (rotationPeriod * 1000));
        posestack.mulPose(Vector3f.XP.rotationDegrees(rotPitch));
        posestack.mulPose(Vector3f.YP.rotationDegrees(rot));
        RenderSystem.applyModelViewMatrix();
        PoseStack tmpPose = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Lighting.setupForFlatItems();
        Minecraft.getInstance().getItemRenderer().renderStatic(recipe.getOutput(), ItemTransforms.TransformType.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, tmpPose, bufferSource, 0);
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableScissor();
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(poseStack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SIDE);
        blit(poseStack, leftPos, topPos, 0, 0, 134, 187);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, leftPos + 136, topPos + 27, 0, 0, 208, 160);
    }

    public static void drawModCenteredString(PoseStack poseStack, Font font, Component component, int pX, int pY, int color) {
        FormattedCharSequence text = component.getVisualOrderText();
        font.draw(poseStack, text, (float) (pX - font.width(text) / 2), (float) pY, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
