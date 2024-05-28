package com.tacz.guns.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.client.gui.components.smith.ResultButton;
import com.tacz.guns.client.gui.components.smith.TypeButton;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableResult;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageCraft;
import com.tacz.guns.util.RenderDistance;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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

    public static void drawModCenteredString(GuiGraphics gui, Font font, Component component, int pX, int pY, int color) {
        FormattedCharSequence text = component.getVisualOrderText();
        gui.drawString(font, text, pX - font.width(text) / 2, pY, color, false);
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

    @Nullable
    private GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId) {
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
        this.addUrlButton();
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

    private void addUrlButton() {
        this.addRenderableWidget(new ImageButton(leftPos + 112, topPos + 164, 18, 18, 149, 211, 18, TEXTURE, b -> {
            if (this.selectedRecipe != null) {
                ItemStack output = selectedRecipe.getOutput();
                Item item = output.getItem();
                ResourceLocation id;
                if (item instanceof IGun iGun) {
                    id = iGun.getGunId(output);
                } else if (item instanceof IAttachment iAttachment) {
                    id = iAttachment.getAttachmentId(output);
                } else if (item instanceof IAmmo iAmmo) {
                    id = iAmmo.getAmmoId(output);
                } else {
                    return;
                }

                PackInfo packInfo = ClientAssetManager.INSTANCE.getPackInfo(id);
                if (packInfo == null) {
                    return;
                }
                String url = packInfo.getUrl();
                if (StringUtils.isNotBlank(url) && minecraft != null) {
                    minecraft.setScreen(new ConfirmLinkScreen(yes -> {
                        if (yes) {
                            Util.getPlatform().openUri(url);
                        }
                        minecraft.setScreen(this);
                    }, url, false));
                }
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
            ItemStack icon = ItemStack.EMPTY;
            CreativeModeTab gunTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(new ResourceLocation(GunMod.MOD_ID, type));
            if (gunTab != null) {
                icon = gunTab.getIconItem();
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
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        drawModCenteredString(gui, font, Component.translatable("gui.tacz.gun_smith_table.preview"), leftPos + 108, topPos + 5, 0x555555);
        gui.drawString(font, Component.translatable(String.format("tacz.type.%s.name", selectedType)), leftPos + 150, topPos + 32, 0x555555, false);
        gui.drawString(font, Component.translatable("gui.tacz.gun_smith_table.ingredient"), leftPos + 254, topPos + 50, 0x555555, false);
        drawModCenteredString(gui, font, Component.translatable("gui.tacz.gun_smith_table.craft"), leftPos + 312, topPos + 167, 0xFFFFFF);
        if (this.selectedRecipe != null) {
            this.renderLeftModel(this.selectedRecipe);
            this.renderPackInfo(gui, this.selectedRecipe);
        }
        if (selectedRecipeList != null && !selectedRecipeList.isEmpty()) {
            renderIngredient(gui);
        }

        this.renderables.stream().filter(w -> w instanceof ResultButton)
                .forEach(w -> ((ResultButton) w).renderTooltips(stack -> this.renderTooltip(gui, mouseX, mouseY)));
    }

    private void renderPackInfo(GuiGraphics gui, GunSmithTableRecipe recipe) {
        ItemStack output = recipe.getOutput();
        Item item = output.getItem();
        ResourceLocation id;
        if (item instanceof IGun iGun) {
            id = iGun.getGunId(output);
        } else if (item instanceof IAttachment iAttachment) {
            id = iAttachment.getAttachmentId(output);
        } else if (item instanceof IAmmo iAmmo) {
            id = iAmmo.getAmmoId(output);
        } else {
            return;
        }

        PackInfo packInfo = ClientAssetManager.INSTANCE.getPackInfo(id);
        PoseStack poseStack = gui.pose();
        if (packInfo != null) {
            poseStack.pushPose();
            poseStack.scale(0.75f, 0.75f, 1);
            Component nameText = Component.translatable(packInfo.getName());
            gui.drawString(font, nameText, (int) ((leftPos + 6) / 0.75f), (int) ((topPos + 122) / 0.75f), ChatFormatting.DARK_GRAY.getColor(), false);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 1);

            int offsetX = (leftPos + 6) * 2;
            int offsetY = (topPos + 123) * 2;
            int nameWidth = font.width(nameText);
            Component ver = Component.literal("v" + packInfo.getVersion()).withStyle(ChatFormatting.UNDERLINE);
            gui.drawString(font, ver, (int) (offsetX + nameWidth * 0.75f / 0.5f + 5), offsetY, ChatFormatting.DARK_GRAY.getColor(), false);
            offsetY += 14;

            String descKey = packInfo.getDescription();
            if (StringUtils.isNoneBlank(descKey)) {
                Component desc = Component.translatable(descKey);
                List<FormattedCharSequence> split = font.split(desc, 245);
                for (FormattedCharSequence charSequence : split) {
                    gui.drawString(font, charSequence, offsetX, offsetY, ChatFormatting.DARK_GRAY.getColor(), false);
                    offsetY += font.lineHeight;
                }
                offsetY += 3;
            }

            gui.drawString(font, Component.translatable("gui.tacz.gun_smith_table.license")
                            .append(Component.literal(packInfo.getLicense()).withStyle(ChatFormatting.DARK_GRAY)),
                    offsetX, offsetY, ChatFormatting.DARK_GRAY.getColor(), false);
            offsetY += 12;

            List<String> authors = packInfo.getAuthors();
            if (!authors.isEmpty()) {
                gui.drawString(font, Component.translatable("gui.tacz.gun_smith_table.authors")
                                .append(Component.literal(StringUtils.join(authors, ", ")).withStyle(ChatFormatting.DARK_GRAY)),
                        offsetX, offsetY, ChatFormatting.DARK_GRAY.getColor(), false);
                offsetY += 12;
            }

            gui.drawString(font, Component.translatable("gui.tacz.gun_smith_table.date")
                            .append(Component.literal(packInfo.getDate()).withStyle(ChatFormatting.DARK_GRAY)),
                    offsetX, offsetY, ChatFormatting.DARK_GRAY.getColor(), false);

            poseStack.popPose();
        }
    }

    private void renderIngredient(GuiGraphics gui) {
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

                gui.renderFakeItem(item, offsetX, offsetY);

                PoseStack poseStack = gui.pose();
                poseStack.pushPose();

                poseStack.translate(0, 0, 200);
                poseStack.scale(0.5f, 0.5f, 1);
                int count = smithTableIngredient.getCount();
                int hasCount = 0;
                if (playerIngredientCount != null && index < playerIngredientCount.size()) {
                    hasCount = playerIngredientCount.get(index);
                }
                int color = count <= hasCount ? 0xFFFFFF : 0xFF0000;
                gui.drawString(font, String.format("%d/%d", count, hasCount), (offsetX + 17) * 2, (offsetY + 10) * 2, color, false);

                poseStack.popPose();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void renderLeftModel(GunSmithTableRecipe recipe) {
        // 先标记一下，渲染高模
        RenderDistance.markGuiRenderTimestamp();

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
        posestack.translate(xPos, yPos, 200);
        posestack.translate(8.0D, 8.0D, 0.0D);
        posestack.scale(1.0F, -1.0F, 1.0F);
        posestack.scale(scale, scale, scale);
        float rot = (System.currentTimeMillis() % (int) (rotationPeriod * 1000)) * (360f / (rotationPeriod * 1000));
        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch));
        posestack.mulPose(Axis.YP.rotationDegrees(rot));
        RenderSystem.applyModelViewMatrix();
        PoseStack tmpPose = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Lighting.setupForFlatItems();

        Minecraft.getInstance().getItemRenderer().renderStatic(recipe.getOutput(), ItemDisplayContext.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, tmpPose, bufferSource, null, 0);

        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableScissor();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(gui);
        gui.blit(SIDE, leftPos, topPos, 0, 0, 134, 187);
        gui.blit(TEXTURE, leftPos + 136, topPos + 27, 0, 0, 208, 160);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
